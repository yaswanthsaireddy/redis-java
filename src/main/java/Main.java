import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        int port = 6379;

        // Use try-with-resources for the ServerSocket itself
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            System.out.println("Server started on port " + port);

            while (true) {
                // Declare the socket INSIDE the loop
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected!");

                // Pass it to the thread immediately
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("Server Error: " + e.getMessage());
        }
        // No finally block needed! ServerSocket closes automatically via try-with-resources
    }

    private static void handleClient(Socket clientSocket) {
        // try-with-resources here handles closing the clientSocket automatically
        try (clientSocket;
             InputStream input = clientSocket.getInputStream()) {

            byte[] buffer = new byte[1024];
            while (input.read(buffer) != -1) {
                clientSocket.getOutputStream().write("+PONG\r\n".getBytes());
            }
        } catch (IOException e) {
            System.out.println("Client handler error: " + e.getMessage());
        }
    }
}
