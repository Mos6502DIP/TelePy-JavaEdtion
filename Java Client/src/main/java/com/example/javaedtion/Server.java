package com.example.javaedtion;

// Server - Represents a saved server connection
// Stores name, address and port for quick connection

public class Server {
    private String name;   // Display name for server
    private String host;   // IP address or hostname
    private int port;      // Port number

    // Create new server entry
    public Server(String name, String host, int port) {
        this.name = name;
        this.host = host;
        this.port = port;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    // Get formatted display string for list UI
    public String getDisplayName() {
        return name + " (" + host + ":" + port + ")";
    }
}