package com.example.javaedtion;

// Config - Application settings storage
// Manages user preferences like switch consent, loads/saves to config.txt

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Config {
    private static final String FILE_NAME = "config.txt";
    private static Config instance;
    
    private boolean switchConsent = false;  // Auto-accept server switch requests

    // Private constructor for singleton pattern
    private Config() {
        load();
    }

    // Get singleton instance - creates if not exists
    public static Config get() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    // Reset all settings to defaults
    public void reset() {
        switchConsent = false;
        save();
    }

    // Save settings to file in JSON-like format
    public void save() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            writer.println("{");
            writer.println("  \"switch_consent\": " + switchConsent);
            writer.println("}");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load settings from file
    private void load() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            save();
            return;
        }

        try {
            String content = new String(Files.readAllBytes(Paths.get(FILE_NAME)));
            // Remove whitespace and newlines for parsing
            content = content.replaceAll("[\\r\\n]", "").replaceAll(" ", "");
            
            switchConsent = Boolean.parseBoolean(extractValue(content, "switch_consent"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Extract value from simple key:value format
    private String extractValue(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return "false";
        int colon = json.indexOf(":", idx);
        int quote = json.indexOf("\"", colon);
        if (quote == -1) return "false";
        int endQuote = json.indexOf("\"", quote + 1);
        if (endQuote == -1) return "false";
        return json.substring(quote + 1, endQuote);
    }

    // Getters and setters
    public boolean isSwitchConsent() { return switchConsent; }
    public void setSwitchConsent(boolean b) { switchConsent = b; save(); }
}