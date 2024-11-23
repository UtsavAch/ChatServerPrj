import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ChatClient {

    // GUI components --- *DO NOT MODIFY*
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- End of GUI components

    // Networking components
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // Constructor
    public ChatClient(String server, int port) throws IOException {
        // GUI setup --- *DO NOT MODIFY*
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText(), true); // Sending message from the user
                } catch (IOException ex) {
                    printMessage("Error sending message. Please try again.\n", true);
                } finally {
                    chatBox.setText("");
                }
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                chatBox.requestFocusInWindow();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (socket != null && !socket.isClosed()) {
                        out.println("/bye");
                        socket.close();
                    }
                } catch (IOException ex) {
                    System.err.println("Error during client shutdown: " + ex.getMessage());
                }
            }
        });
        // --- End of GUI setup

        // NETWORKING SETUP
        socket = new Socket(server, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Display a welcome message
        printMessage("Connected to the server at " + server + ":" + port + "\n", false);
        printMessage("Use /nick <name> to set your nickname, /join <room> to join a room, and /leave to leave the room.\n", false);
        printMessage("Type /bye to disconnect.\n", false);
    }

    //METHOD TO SEND A NEW MESSAGE TO THE CERVER (----------May still need to modify---------)
    public void newMessage(String message, boolean isSender) throws IOException {
        // Send the message to the server
        out.println(message);

        // If the message is a disconnect command, close the socket
        if (message.equalsIgnoreCase("/bye")) {
            printMessage("Disconnected from the server.\n", false);
            socket.close();
        } else {
            // Show the message in the chat area with blue background for sender
            printMessage(message, isSender);
        }
    }

    // METHOD TO PRINT A MESSAGE TO THE CHAT AREA (----------May still need to modify---------)
    public void printMessage(final String message, boolean isSender) {
        // Creating a new JPanel to hold the message
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout());
        JTextArea messageArea = new JTextArea(message);
        messageArea.setEditable(false);

        messagePanel.add(messageArea, BorderLayout.CENTER);
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength()); // Auto-scroll to the latest message
    }

    // Main client loop for receiving messages
    public void run() throws IOException {
        try {
            String response;
            while ((response = in.readLine()) != null) {
                // Handle specific server responses
                if (response.startsWith("ERROR")) {
                    printMessage("Error: Invalid command or action.\n", false);
                } else if (response.equalsIgnoreCase("BYE")) {
                    printMessage("Disconnected from the server.\n", false);
                    break;
                } else {
                    // General messages
                    printMessage(response, false);
                }
            }
        } catch (IOException ex) {
            printMessage("Connection lost. Please try reconnecting.\n", false);
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    // Main method to start the client
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: java ChatClient <server> <port>");
            System.exit(1);
        }
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }
}
