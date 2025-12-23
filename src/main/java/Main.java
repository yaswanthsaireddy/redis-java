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
        try (clientSocket;
             InputStream input = clientSocket.getInputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                String request = new String(buffer, 0, bytesRead);

                // Split the RESP message into parts
                String[] parts = request.split("\r\n");

                // Look for the command (usually at parts[2] in the RESP array)
                // Example: *2, $4, ECHO, $3, hey
                String command = parts[2].toUpperCase();

                if (command.equals("PING")) {
                    clientSocket.getOutputStream().write("+PONG\r\n".getBytes());
                }
                else if (command.equals("ECHO")) {
                    StringBuilder fullMessage = new StringBuilder();

                    // Start at index 4 (the first argument)
                    // Skip by 2 because every piece of data has a '$' length header before it
                    for (int i = 4; i < parts.length; i += 2) {
                        fullMessage.append(parts[i]);
                        if (i + 2 < parts.length) fullMessage.append(" "); // Add space between words
                    }

                    String result = fullMessage.toString();
                    String response = "$" + result.length() + "\r\n" + result + "\r\n";
                    clientSocket.getOutputStream().write(response.getBytes());
                }
            }
        } catch (IOException e) {
            System.out.println("Client handler error: " + e.getMessage());
        }
    }
}
