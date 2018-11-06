import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class MultiThreadChatClient implements Runnable {

    // The client socket
    private static Socket clientSocket = null;
    // The output stream
    private static PrintStream os = null;
    // The input stream
    private static BufferedReader is = null;

    private static BufferedReader inputLine = null;
    private static boolean closed = false;

    public static void main(String[] args) {

        // The default port.
        int portNumber = 2222;
        // The default host.
        String host = "localhost";

        if (args.length == 2) {
            host = args[0];
            portNumber = Integer.valueOf(args[1]);
        }

        System.out.println(String.format("Connecting to host %s on port %d", host, portNumber));

        /*
         * Open a socket on a given host and port. Open input and output streams.
         */
        try {
            clientSocket = new Socket(host, portNumber);
            inputLine = new BufferedReader(new InputStreamReader(System.in));
            os = new PrintStream(clientSocket.getOutputStream());
            is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println(String.format("Unknown host %s", host));
        } catch (IOException e) {
            System.err.println(String.format("Couldn't connect the host %s", host));
        }

        /*
         * If everything has been initialized then we can send data to the
         * socket we have opened a connection to on the port portNumber.
         */
        if (clientSocket != null && os != null && is != null) {
            try {

                /* Create a thread to read from the server. */
                new Thread(new MultiThreadChatClient()).start();

                while (!closed) {
                    os.println(inputLine.readLine().trim());
                }
                /*
                 * Close the output stream, close the input stream, close the socket.
                 */
                os.close();
                is.close();
                clientSocket.close();
            } catch (IOException e) {
                System.err.println(String.format("IOException:  %s", e.getMessage()));
            }
        }
    }

    /*
     * Create a thread to read from the server.
     */
    public void run() {
        /*
         * Keep on reading from the socket till we receive "*** Bye" from the
         * server. Once we receive that, then we want to break.
         */
        String responseLine;
        try {
            while ((responseLine = is.readLine()) != null) {
                System.out.println(responseLine);
                if (responseLine.contains("*** Bye")) {
                    break;
                }
            }
            closed = true;
        } catch (IOException e) {
            System.err.println(String.format("IOException:  %s", e.getMessage()));
        }
    }
}