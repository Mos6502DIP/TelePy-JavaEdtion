package com.example.javaedtion;

// ServerList - Manages saved server list
// Loads and saves servers to/from text file for persistent storage

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ServerList {
    private static final String FILE_NAME = "servers.txt";
    private List<Server> servers = new ArrayList<>();

    // Load saved servers on initialization
    public ServerList() {
        load();
    }

    // Add new server to list and save
    public void add(Server server) {
        servers.add(server);
        save();
    }

    // Remove server by index and save
    public void remove(int index) {
        if (index >= 0 && index < servers.size()) {
            servers.remove(index);
            save();
        }
    }

    // Clear all saved servers
    public void clearAll() {
        servers.clear();
        save();
    }

    // Get copy of all saved servers
    public List<Server> getAll() {
        return new ArrayList<>(servers);
    }

    // Save servers to file - format: name|host|port
    private void save() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Server s : servers) {
                writer.println(s.getName() + "|" + s.getHost() + "|" + s.getPort());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load servers from file
    private void load() {
        servers.clear();
        File file = new File(FILE_NAME);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 3) {
                    servers.add(new Server(parts[0], parts[1], Integer.parseInt(parts[2])));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}