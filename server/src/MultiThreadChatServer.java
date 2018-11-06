import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;

/*
 * Chat server that broadcasts client messages to all connected clients
 */
public class MultiThreadChatServer {

    // The server socket.
    private static ServerSocket serverSocket = null;

    // This chat server can accept up to maxClientsCount clients' connections.
    private static final int maxClientsCount = 10;
    private static final clientThread[] threads = new clientThread[maxClientsCount];

    public static void main(String args[]) {

        // The default port number.
        int portNumber = 2222;

        if (args.length == 1) {
            portNumber = Integer.valueOf(args[0]);
        }

        System.out.println(String.format("Server staring on port number %d %n", portNumber));


        /*
         * Open a server socket on the portNumber (default 2222). Note that we can
         * not choose a port less than 1023 if we are not privileged users (root).
         */
        try {
            serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        /*
         * Create a client socket for each connection and pass it to a new client
         * thread.
         */
        while (true) {
            try {
                // The client socket.
                Socket clientSocket = serverSocket.accept();
                int i;
                for (i = 0; i < maxClientsCount; i++) {
                    if (threads[i] == null) {
                        (threads[i] = new clientThread(clientSocket, threads)).start();
                        break;
                    }
                }
                if (i == maxClientsCount) {
                    PrintStream os = new PrintStream(clientSocket.getOutputStream());
                    os.println("Server too busy. Try later.");
                    os.close();
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}

/*
 * The chat client thread. This client thread opens the input and the output
 * streams for a particular client, ask the client's name, informs all the
 * clients connected to the server about the fact that a new client has joined
 * the chat room, and as long as it receives data, echos that data back to all
 * other clients. When a client leaves the chat room this thread informs
 * all the clients about that and terminates.
 */
class clientThread extends Thread {

    private PrintStream os = null;
    private Socket clientSocket;
    private final clientThread[] threads;
    private int maxClientsCount;

    clientThread(Socket clientSocket, clientThread[] threads) {
        this.clientSocket = clientSocket;
        this.threads = threads;
        maxClientsCount = threads.length;
    }

    public void run() {
        int maxClientsCount = this.maxClientsCount;
        clientThread[] threads = this.threads;

        try {
            /*
             * Create input and output streams for this client.
             */

            BufferedReader is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            os = new PrintStream(clientSocket.getOutputStream());

            os.println(String.format("%nEnter your name: "));
            String name = is.readLine().trim();

            os.println(String.format("%nWelcome %s to our chat room", name));
            os.println(String.format("To leave enter /quit in a new line%n"));

            // synchronized is used to make these statements thread-safe
            // using synchronized forces all other "synchronized" statements to check/wait, until the array thread isn't in use
            synchronized(this) {
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] != null && threads[i] != this) {
                        threads[i].os.println(String.format("*** A new user, %s, has entered the chat room! ***", name));
                    }
                }
            }

            while (true) {
                String line = is.readLine().trim();

                if (line.startsWith("/quit")) {
                    break;
                }

                synchronized(this) { // "thread-safe"
                    for (int i = 0; i < maxClientsCount; i++) {
                        if (threads[i] != null) {
                            threads[i].os.println(String.format("<%s> %s", name, line));
                        }
                    }
                }

            }

            synchronized(this) {
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] != null && threads[i] != this) {
                        threads[i].os.println(String.format("*** The user, %s, is leaving the chat room! ***", name));
                    }
                }
            }


            os.println(String.format("*** Bye %s ***", name));

            /*
             * Clean up. Set the current thread variable to null so that a new client
             * could be accepted by the server.
             */
            synchronized(this) {
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] == this) {
                        threads[i] = null;
                    }
                }
            }


            /*
             * Close the output stream, close the input stream, close the socket.
             */
            is.close();
            os.close();
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Error");
        }
    }
}
