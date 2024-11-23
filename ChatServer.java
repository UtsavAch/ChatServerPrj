import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

public class ChatServer {
    // A pre-allocated buffer for the received data
    static private final ByteBuffer buffer = ByteBuffer.allocate(16384);

    // Decoder for incoming text -- assume UTF-8
    static private final Charset charset = Charset.forName("UTF-8");
    static private final CharsetDecoder decoder = charset.newDecoder();
    static private final CharsetEncoder encoder = charset.newEncoder(); // Encoder for sending data back

    static private final String STATE_INIT = "init";
    static private final String STATE_OUTSIDE = "outside";
    static private final String STATE_INSIDE = "inside";

    // Maps to manage state
    static private final Map<SocketChannel, String> nicknames = new HashMap<>();
    static private final Map<SocketChannel, String> rooms = new HashMap<>();
    static private final Map<String, Set<SocketChannel>> roomMembers = new HashMap<>();
    static private final Map<SocketChannel, String> clientState = new HashMap<>();//TO BE USED FOR STATES


    static public void main(String args[]) throws Exception {
        // Parse port from command line
        int port = Integer.parseInt(args[0]);

        try {
            // Instead of creating a ServerSocket, create a ServerSocketChannel
            ServerSocketChannel ssc = ServerSocketChannel.open();

            // Set it to non-blocking, so we can use select
            ssc.configureBlocking(false);

            // Get the Socket connected to this channel, and bind it to the
            // listening port
            ServerSocket ss = ssc.socket();
            InetSocketAddress isa = new InetSocketAddress(port);
            ss.bind(isa);

            // Create a new Selector for selecting
            Selector selector = Selector.open();

            // Register the ServerSocketChannel, so we can listen for incoming
            // connections
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Listening on port " + port);

            while (true) {
                // See if we've had any activity -- either an incoming connection,
                // or incoming data on an existing connection
                int num = selector.select();

                // If we don't have any activity, loop around and wait again
                if (num == 0) {
                    continue;
                }

                // Get the keys corresponding to the activity that has been
                // detected, and process them one by one
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    // Get a key representing one of bits of I/O activity
                    SelectionKey key = it.next();

                    // What kind of activity is it?
                    if (key.isAcceptable()) {

                        // It's an incoming connection.  Register this socket with
                        // the Selector so we can listen for input on it
                        Socket s = ss.accept();
                        System.out.println("Got connection from " + s);

                        // Make sure to make it non-blocking, so we can use a selector
                        // on it.
                        SocketChannel sc = s.getChannel();
                        sc.configureBlocking(false);

                        // Register it with the selector, for reading
                        sc.register(selector, SelectionKey.OP_READ);

                        // Initialize the state for the new client
                        clientState.put(sc, STATE_INIT);
                        System.out.println("Client connected: INIT state set.");//debug

                    } else if (key.isReadable()) {

                        SocketChannel sc = null;

                        try {
                            // It's incoming data on a connection -- process it
                            sc = (SocketChannel) key.channel();
                            boolean ok = processInput(sc, selector, key);

                            // If the connection is dead, remove it from the selector
                            // and close it
                            if (!ok) {
                                key.cancel();

                                Socket s = null;
                                try {
                                    s = sc.socket();
                                    System.out.println("Closing connection to " + s);
                                    s.close();
                                } catch (IOException ie) {
                                    System.err.println("Error closing socket " + s + ": " + ie);
                                }
                            }

                        } catch (IOException ie) {
                            // On exception, remove this channel from the selector
                            key.cancel();

                            try {
                                sc.close();
                            } catch (IOException ie2) {
                                System.out.println(ie2);
                            }

                            System.out.println("Closed " + sc);
                        }
                    }
                }

                // We remove the selected keys, because we've dealt with them.
                keys.clear();
            }
        } catch (IOException ie) {
            System.err.println(ie);
        }
    }


    // Just read the message from the socket and send it to stdout
    //PROCESS INPUT
    static private boolean processInput(SocketChannel sc, Selector selector, SelectionKey key) throws IOException {
        buffer.clear();
        int bytesRead = sc.read(buffer);
        buffer.flip();

        if (bytesRead == -1) {
            disconnectUser(sc, key);
            return false;
        }

        String message = decoder.decode(buffer).toString().trim();

        System.out.println("Received message: " + message);  // Debugging

        if (message.startsWith("/")) {
            return handleCommand(sc, message);
        } else {
            return handleMessage(sc, message);
        }
    }

    //HANDLE COMMAND
    static private boolean handleCommand(SocketChannel sc, String command) throws IOException {
        String[] parts = command.split(" ", 2);
        String cmd = parts[0];
        String arg = (parts.length > 1) ? parts[1] : null;

        //System.out.println(Arrays.toString(parts));//debug

        switch (cmd) {
            case "/nick":
                return handleNick(sc, arg);
            case "/join":
                return handleJoin(sc, arg);
            case "/leave":
                return handleLeave(sc);
            case "/bye":
                return handleBye(sc);
            default:
                sendMessage(sc, "ERROR");
                return true;
        }
    }

    //HANDLE NICK
    static private boolean handleNick(SocketChannel sc, String nickname) throws IOException {
        if (nickname == null || nickname.trim().isEmpty() || nicknames.containsValue(nickname)) {
            sendMessage(sc, "ERROR");
            return true;
        }

        String oldNickname = nicknames.put(sc, nickname);
        if (oldNickname == null) {
            sendMessage(sc, "OK");
            //state to outside
        } else {
            sendMessage(sc, "OK");
            String room = rooms.get(sc); // Get the current room, if any
            if (room != null) {
                notifyRoom(sc, "NEWNICK " + oldNickname + " " + nickname, room);
            }
        }
        return true;
    }

    //HANDLE JOIN
    static private boolean handleJoin(SocketChannel sc, String room) throws IOException {
        if (room == null || room.trim().isEmpty()) {
            sendMessage(sc, "ERROR");
            return true;
        }

        String currentRoom = rooms.put(sc, room);
        roomMembers.putIfAbsent(room, new HashSet<>());
        roomMembers.get(room).add(sc);

        if (currentRoom != null && !currentRoom.equals(room)) {
            notifyRoom(sc, "LEFT " + nicknames.get(sc), currentRoom);
            roomMembers.get(currentRoom).remove(sc);
        }

        sendMessage(sc, "OK");
        notifyRoom(sc, "JOINED " + nicknames.get(sc), room);
        //state to inside
        return true;
    }

    //HANDLE LEAVE
    static private boolean handleLeave(SocketChannel sc) throws IOException {
        String room = rooms.remove(sc);
        if (room == null) {
            sendMessage(sc, "ERROR");
            return true;
        }

        roomMembers.get(room).remove(sc);
        notifyRoom(sc, "LEFT " + nicknames.get(sc), room);
        sendMessage(sc, "OK");
        //state to outside
        return true;
    }

    //HANDLE BYE
    static private boolean handleBye(SocketChannel sc) throws IOException {
        disconnectUser(sc, null);
        sendMessage(sc, "BYE");
        //state not inside
        return false;
    }


    //HANDLE MESSAGE
    static private boolean handleMessage(SocketChannel sc, String message) throws IOException {
        String room = rooms.get(sc);
        if (room == null) {
            sendMessage(sc, "ERROR");
            return true;
        }

        String nickname = nicknames.get(sc);
        if (message.startsWith("//")) {
            message = message.substring(1); // Unescape leading slash
        }

        String fullMessage = "MESSAGE " + nickname + " " + message;
        notifyRoom(sc, fullMessage, room);
        return true;
    }

    //DISCONNECT USER
    static private void disconnectUser(SocketChannel sc, SelectionKey key) throws IOException {
        if (key != null) key.cancel();
        String room = rooms.remove(sc);
        if (room != null) {
            roomMembers.get(room).remove(sc);
            notifyRoom(sc, "LEFT " + nicknames.get(sc), room);
        }
        nicknames.remove(sc);
        sc.close();
    }

    //SEND MESSAGE
    static private void sendMessage(SocketChannel sc, String message) throws IOException {
        sc.write(encoder.encode(CharBuffer.wrap(message + "\n")));
    }

    //NOTIFY ROOM
    static private void notifyRoom(SocketChannel sc, String message, String room) throws IOException {
        Set<SocketChannel> members = roomMembers.getOrDefault(room, Collections.emptySet());
        for (SocketChannel member : members) {
            if (!member.equals(sc)) {
                sendMessage(member, message);
            }
        }
    }
}
