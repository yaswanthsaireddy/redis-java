import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    private static final ConcurrentHashMap<String, ValueHolder> storage = new ConcurrentHashMap<>();
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
                    // If there are multiple arguments (like ECHO hello world),
                    // we can join them or just take the first one as per Redis spec.
                    // Let's take the first argument (index 4)
                    String argument = parts[4];

                    // Construct the Bulk String response
                    StringBuilder response = new StringBuilder();
                    response.append("$").append(argument.length()).append("\r\n");
                    response.append(argument).append("\r\n");

                    clientSocket.getOutputStream().write(response.toString().getBytes());
                }
                else if (command.equals("SET")) {
                    String key = parts[4];
                    String value = parts[6];
                    Long expiry = null;

                    if (parts.length > 8) {
                        String option = parts[8].toUpperCase(); // Could be "EX" or "PX"
                        long duration = Long.parseLong(parts[10]);

                        if (option.equals("EX")) {
                            // Seconds to Milliseconds
                            expiry = System.currentTimeMillis() + (duration * 1000);
                        } else if (option.equals("PX")) {
                            // Already Milliseconds
                            expiry = System.currentTimeMillis() + duration;
                        }
                    }

                    storage.put(key, new ValueHolder(value, expiry));
                    clientSocket.getOutputStream().write("+OK\r\n".getBytes());
                }
                else if (command.equals("GET")) {
                    String key = parts[4];
                    ValueHolder holder = storage.get(key);

                    if (holder == null || holder.isExpired()) {
                        if (holder != null) storage.remove(key); // Cleanup expired key
                        clientSocket.getOutputStream().write("$-1\r\n".getBytes());
                    } else {
                        if(holder.value instanceof String)
                        {
                            String val= (String) holder.value;
                            String response = "$" + val.length() + "\r\n" + holder.value + "\r\n";
                            clientSocket.getOutputStream().write(response.getBytes());
                        }
                        else
                        {
                            clientSocket.getOutputStream().write("-WRONGTYPE Operation against a key holding the wrong kind of value\r\n".getBytes());
                        }
                    }
                }
                else if (command.equals("RPUSH")) {
                    String key = parts[4];

                    ValueHolder holder = storage.get(key);
                    List<String> list;

                    if (holder == null || holder.isExpired()) {
                        list = new LinkedList<>();
                        storage.put(key, new ValueHolder(list, null));
                    } else {
                        list = (List<String>) holder.value;
                    }


                    for (int i = 6; i < parts.length; i += 2) {
                        list.add(parts[i]);
                    }

                    String response = ":" + list.size() + "\r\n";
                    clientSocket.getOutputStream().write(response.getBytes());
                }
                else if (command.equals("LPUSH")) {
                    String key = parts[4];

                    ValueHolder holder = storage.get(key);
                    List<String> list;

                    if (holder == null || holder.isExpired()) {
                        list = new LinkedList<>();
                        storage.put(key, new ValueHolder(list, null));
                    } else {
                        list = (List<String>) holder.value;
                    }

                    int count=0;
                    for (int i = 6; i < parts.length; i += 2) {
                        list.add(0,parts[i]);
                        count++;
                    }

                    String response = ":" + list.size() + "\r\n";
                    clientSocket.getOutputStream().write(response.getBytes());
                }
                else if (command.equals("LRANGE")) {
                    String key = parts[4];
                    int start = Integer.parseInt(parts[6]);
                    int stop = Integer.parseInt(parts[8]);

                    ValueHolder holder = storage.get(key);

                    if (holder == null || !(holder.value instanceof List)) {
                        clientSocket.getOutputStream().write("*0\r\n".getBytes());
                    }
                    else if (!(holder.value instanceof List<?>))
                    {
                        clientSocket.getOutputStream().write("-The key's type is not of list\r\n".getBytes());
                    }
                    else {
                        List<String> list = (List<String>) holder.value;
                        int size = list.size();


                        if (start < 0) start = size + start;
                        if (stop < 0) stop = size + stop;


                        start = Math.max(0, start);
                        stop = Math.min(size - 1, stop);

                        if (start > stop || start >= size) {
                            clientSocket.getOutputStream().write("*0\r\n".getBytes());
                        } else {
                            int count = stop - start + 1;
                            StringBuilder response = new StringBuilder();
                            response.append("*").append(count).append("\r\n");

                            for (int i = start; i <= stop; i++) {
                                String item = list.get(i);
                                response.append("$").append(item.length()).append("\r\n");
                                response.append(item).append("\r\n");
                            }
                            clientSocket.getOutputStream().write(response.toString().getBytes());
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Client handler error: " + e.getMessage());
        }
    }
}
