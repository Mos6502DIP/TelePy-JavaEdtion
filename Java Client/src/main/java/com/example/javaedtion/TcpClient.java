package com.example.javaedtion;

// TcpClient - Handles TCP network connections to server
// Provides methods for connecting, sending, receiving and closing connections

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TcpClient {
    // ANSI colour codes for foreground text
    private static final java.util.Map<String, String> colour_data = new java.util.HashMap<>();
    // ANSI colour codes for background
    private static final java.util.Map<String, String> background_data = new java.util.HashMap<>();

    final String host;  // Server address
    final int port;     // Server port
    private Socket socket;
    private InputStream in;
    private OutputStream out;

    // Initialise colour code mappings
    static {
        // Foreground colours
        colour_data.put("black", "\033[30m");
        colour_data.put("red", "\033[31m");
        colour_data.put("green", "\033[32m");
        colour_data.put("yellow", "\033[33m");
        colour_data.put("blue", "\033[34m");
        colour_data.put("magenta", "\033[35m");
        colour_data.put("cyan", "\033[36m");
        colour_data.put("white", "\033[37m");
        colour_data.put("light_black", "\033[1;30m");
        colour_data.put("light_red", "\033[1;31m");
        colour_data.put("light_green", "\033[1;32m");
        colour_data.put("light_yellow", "\033[1;33m");
        colour_data.put("light_blue", "\033[1;34m");
        colour_data.put("light_magenta", "\033[1;35m");
        colour_data.put("light_cyan", "\033[1;36m");
        colour_data.put("light_white", "\033[1;37m");
        colour_data.put("reset", "\033[0m");

        // Background colours
        background_data.put("black", "\033[40m");
        background_data.put("red", "\033[41m");
        background_data.put("green", "\033[42m");
        background_data.put("yellow", "\033[43m");
        background_data.put("blue", "\033[44m");
        background_data.put("magenta", "\033[45m");
        background_data.put("cyan", "\033[46m");
        background_data.put("white", "\033[47m");
        background_data.put("light_black", "\033[1;40m");
        background_data.put("light_red", "\033[1;41m");
        background_data.put("light_green", "\033[1;42m");
        background_data.put("light_yellow", "\033[1;43m");
        background_data.put("light_blue", "\033[1;44m");
        background_data.put("light_magenta", "\033[45m");
        background_data.put("light_cyan", "\033[1;46m");
        background_data.put("light_white", "\033[1;47m");
        background_data.put("reset", "\033[0m");
    }

    // Apply colour codes to text - convenience method for server responses
    public static String colour(String text, String colour, String background) {
        String colourCode = colour_data.getOrDefault(colour, colour_data.get("reset"));
        String backgroundCode = background_data.getOrDefault(background, "");
        return backgroundCode + colourCode + text + colour_data.get("reset");
    }

    // Create new client connection
    public TcpClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // Establish connection to server
    public void connect() throws IOException {
        socket = new Socket(host, port);
        socket.setTcpNoDelay(true);  // Disable Nagle's algorithm for faster response
        in = socket.getInputStream();
        out = socket.getOutputStream();
        System.out.println("Connected to " + host + ":" + port);
    }

    // Send message to server
    public void send(String message) throws IOException {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        out.write(data);
        out.flush();
    }

    // Send acknowledgement to server
    public void ack() throws IOException {
        BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(socket.getOutputStream(), "UTF-8")
        );
        writer.write("ACK\n");
        writer.flush();
    }

    // Receive message from server (blocking)
    public String receive() throws IOException {
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(socket.getInputStream(), "UTF-8")
        );
        return reader.readLine();
    }

    // Close connection cleanly
    public void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) { }
    }
}