import java.io.*;
import java.net.*;
//import java.util.*;
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

    // Construtor
    public ChatClient(String server, int port) throws IOException {

        // Inicialização da interface gráfica --- * NÃO MODIFICAR *
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
                    newMessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {
                    chatBox.setText("");
                }
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                chatBox.requestFocusInWindow();
            }
        });
    // --- Fim da inicialização da interface gráfica

        // NETWORKING SETUP
        socket = new Socket(server, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Display a welcome message
        printMessage("Connected to the server at " + server + ":" + port + "\n");
        printMessage("Use /nick <name> to set your nickname, /join <room> to join a room, and /leave to leave the room.\n");
        printMessage("Type /bye to disconnect.\n");
    }

    // METHOD TO ENSURE IF IS A REAL COMMAND OR JUST A MESSAGE WITH '/' IN THE BEGGINIG
    private boolean checkIfCommand(String message){
        if (message.startsWith("/")){
            String[] parts = message.split(" ", 3);
            String cmd = parts[0];
            switch (cmd) {
                case "/nick":
                    return true;
                case "/join":
                    return true;
                case "/leave":
                    return true;
                case "/bye":
                    return true;
                case "/priv":
                    return true;
                default:
                    return false;
            }
        }
        return true;
    }

    // METHOD TO CREATE A MORE FRIENDLY MESSAGE FOR CLIENT
    private String friendlyMessage(String message){
        String splitMessage[] = message.split(" ");
        String header = splitMessage[0];
        String newMessage = "";

        switch (header) {
            case "OK":
                return "Ok: Message received sucessfuly";
            case "ERROR":
                return "Error: Message sent was unsuccessful";
            case "MESSAGE":
                newMessage += splitMessage[1] + ": ";
                for (int i = 2; i < splitMessage.length; i++){
                    newMessage += splitMessage[i] + " ";
                }
                return newMessage;
            case "NEWNICK":
                return splitMessage[1] + " changed name to " + splitMessage[2];
            case "JOINED":
                return splitMessage[1] + " joined the room";
            case "LEFT":
                return splitMessage[1] + " leaft the room";
            case "BYE":
                return "Goodbye!";
            default:
                return "Unknown message received";
        }
    }
    
    
    //METHOD TO SEND A NEW MESSAGE TO THE SERVER
    public void newMessage(String message) throws IOException {

        if (!checkIfCommand(message)){ 
            out.println( "/" + message);
        } else {
            // Send the message to the server
            out.println(message);
        }

        // If the message is a disconnect command, close the socket
        if (message.equalsIgnoreCase("/bye")) {
            socket.close();
        } else {
            // Show the message in the chat area with blue background for sender
            printMessage(message + "\n");
        }
    }

    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    public void printMessage(final String message) {
        chatArea.append(message);
    }


    // Main client loop for receiving messages
    public void run() throws IOException {
        try {
            String response;
            while ((response = in.readLine()) != null) {
                printMessage(friendlyMessage(response) + "\n");
            }
        } catch (IOException ex) {
            printMessage("Connection lost. Please try reconnecting.\n");
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    // Main method to start the client
    // * DO NOT MODIFY *
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: java ChatClient <server> <port>");
            System.exit(1);
        }
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }
}
