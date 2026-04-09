package com.example.javaedtion;

// TelePy Java Edition - Terminal Controller
// Handles terminal UI display and server communication
// Processes server packets and manages user input

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TerminalController {
    // FXML injected UI elements
    @FXML
    private TextFlow terminalOutput;  // Display area for terminal text
    @FXML
    private TextField terminalInput;  // User input field
    @FXML
    private ScrollPane scrollPane;    // Scrollable container
    @FXML
    private VBox outputContainer;      // Main container

    // Connection and state variables
    private TcpClient client;          // Network client to server
    private ObjectMapper mapper = new ObjectMapper();  // JSON parsing
    private boolean running = false;   // Connection loop flag
    private int inputMode = 0;         // Current input type (0=none, 1=text, 5/8=password)
    private Color defaultColor = Color.LIME;  // Default text color
    private String host;               // Server address
    private int port;                 // Server port
    private Stage stage;              // Window reference
    
    // Input buffering for handling data while waiting for user
    private volatile boolean waitingForInput = false;
    private volatile String pendingInput = null;

    // ANSI escape code pattern for colour parsing
    private static final Pattern ansiPattern = Pattern.compile("\033\\[([0-9;]*)m");
    // Maps ANSI colour codes to JavaFX colours
    private static final java.util.Map<String, Color> colourMap = new java.util.HashMap<>();

    // Initialise colour mapping for ANSI codes
    static {
        // Standard foreground colours (30-37)
        colourMap.put("30", Color.BLACK);
        colourMap.put("31", Color.RED);
        colourMap.put("32", Color.LIME);
        colourMap.put("33", Color.YELLOW);
        colourMap.put("34", Color.BLUE);
        colourMap.put("35", Color.MAGENTA);
        colourMap.put("36", Color.CYAN);
        colourMap.put("37", Color.WHITE);
        
        // Extended colours (90-97) - bright versions
        colourMap.put("90", Color.GRAY);
        colourMap.put("91", Color.rgb(255, 102, 102));
        colourMap.put("92", Color.rgb(102, 255, 102));
        colourMap.put("93", Color.rgb(255, 255, 102));
        colourMap.put("94", Color.rgb(102, 102, 255));
        colourMap.put("95", Color.rgb(255, 102, 255));
        colourMap.put("96", Color.rgb(102, 255, 255));
        colourMap.put("97", Color.WHITE);
        
        // Reset and bold codes
        colourMap.put("0", Color.LIME);
        colourMap.put("1", Color.LIME);
        
        // Bold foreground colours (1;30 - 1;37)
        colourMap.put("1;30", Color.GRAY);
        colourMap.put("1;31", Color.RED);
        colourMap.put("1;32", Color.LIME);
        colourMap.put("1;33", Color.YELLOW);
        colourMap.put("1;34", Color.BLUE);
        colourMap.put("1;35", Color.MAGENTA);
        colourMap.put("1;36", Color.CYAN);
        colourMap.put("1;37", Color.WHITE);
    }

    // Initialise terminal with connection details
    public void initialize(String host, int port, Stage stage) {
        // Set defaults for missing parameters
        if (host == null || host.isEmpty()) host = "127.0.0.1";
        if (port <= 0) port = 1998;
        
        this.host = host;
        this.port = port;
        this.stage = stage;
        
        updateTitle("Connecting...");
        
        // Convert @ to localhost
        if (this.host.equals("@")) this.host = "127.0.0.1";
        
        // Clean disconnect on window close
        stage.setOnCloseRequest(event -> {
            running = false;
            try {
                if (client != null) client.close();
            } catch (Exception ignored) {}
        });
        
        client = new TcpClient(host, port);
        startTerminal();
    }

    // Update window title with connection status
    private void updateTitle(String status) {
        if (stage != null) {
            Platform.runLater(() -> stage.setTitle("Terminal - " + host + ":" + port + " [" + status + "]"));
        }
    }

    // Main connection loop - runs in separate thread
    private void startTerminal() {
        new Thread(() -> {
            try {
                client.connect();
                client.send("terminal");
                running = true;

                Platform.runLater(() -> updateTitle("Connected"));
                appendText("Connecting to " + client.host + ":" + client.port + "...\n");

                while (running) {
                    String response = client.receive();
                    if (response == null) break;
                    
                    // Buffer incoming data if waiting for user input
                    if (waitingForInput && pendingInput == null) {
                        pendingInput = response;
                        client.ack();
                        continue;
                    }
                    
                    client.ack();
                    
                    // Process received packet
                    boolean cont = handlePacket(response);
                    if (!cont) running = false;
                    
                    // Handle any buffered packets
                    while (pendingInput != null && running) {
                        String saved = pendingInput;
                        pendingInput = null;
                        boolean cont2 = handlePacket(saved);
                        if (!cont2) { running = false; break; }
                    }
                }

                client.close();
                Platform.runLater(() -> {
                    updateTitle("Disconnected");
                    appendText("\n[Disconnected]\n");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateTitle("Error");
                    appendText("\n[Error: " + e.getMessage() + "]\n");
                });
            }
        }).start();
        
        // Enable input field after connection
        Platform.runLater(() -> {
            terminalInput.setDisable(false);
            terminalInput.setVisible(true);
            terminalInput.requestFocus();
        });
    }

    // Process server packet - format: MODE|DATA
    private boolean handlePacket(String data) {
        String[] Packet = data.split("\\|");
        String Mode = Packet[0];
        
        // Restore pipe characters from separator
        for (int i = 0; i < Packet.length; i++) {
            Packet[i] = Packet[i].replace("($SEP$)", "|");
        }
        
        try {
            switch (Mode) {
                // Mode 0 - Print text to terminal
                case "0":
                    appendText(Packet[1] + "\n");
                    return true;
                    
                // Mode 1 - Input request, display prompt and wait for user
                case "1":
                    inputMode = 1;
                    appendText(Packet[1]);
                    enableInput();
                    return true;
                    
                // Mode 2 - Server disconnecting client
                case "2":
                    appendText(Packet[1] + "\n");
                    onDisconnect();
                    return false;
                    
                // Mode 3 - Clear terminal screen
                case "3":
                    Platform.runLater(() -> terminalOutput.getChildren().clear());
                    return true;
                    
                // Mode 4 - Print blank line
                case "4":
                    appendText("\n");
                    return true;
                    
                // Mode 5 - Password input (hidden)
                case "5":
                    inputMode = 5;
                    appendText(Packet[1]);
                    enablePasswordInput();
                    return true;
                    
                // Mode 6 - Client version check
                case "6":
                    client.send("TelePy-Java-2.0");
                    return true;
                    
                // Mode 8 - Hidden input (alternative password mode)
                case "8":
                    inputMode = 8;
                    appendText(Packet[1]);
                    enablePasswordInput();
                    return true;
                    
                // Mode 9 - Print with specific colour
                case "9":
                    String[] ServerColourData = mapper.readValue(Packet[1], String[].class);
                    Color serverColor = colourMap.getOrDefault(ServerColourData[0], defaultColor);
                    Color savedDefault = defaultColor;
                    defaultColor = serverColor;
                    appendText(Packet[2] + "\n");
                    defaultColor = savedDefault;
                    return true;
                    
                // Mode 10 - Screen display (2D array)
                case "10":
                    String[][] Screen = mapper.readValue(Packet[1], String[][].class);
                    StringBuilder sb = new StringBuilder();
                    for (String[] row : Screen) {
                        for (String cell : row) sb.append(cell);
                        sb.append("\n");
                    }
                    appendText(sb.toString());
                    return true;
                    
                // Mode 15 - Switch to different server
                case "15":
                    String newServer = Packet.length > 1 ? Packet[1].replace("($SEP$)", "|") : "";
                    handleSwitch(newServer);
                    return true;
                    
                // Mode 19 - Print without newline
                case "19":
                    appendText(Packet[1]);
                    return true;
                    
                // Unknown mode - debug output
                default:
                    appendText("[DEBUG] " + data + "\n");
                    return true;
            }
        } catch (Exception e) {
            appendText("[Error handling packet: " + e.getMessage() + "]\n");
            return true;
        }
    }

    // Handle server initiated disconnect
    private void onDisconnect() {
        Platform.runLater(() -> {
            updateTitle("Disconnected");
            terminalInput.setDisable(true);
            appendText("\n[Server disconnected. Close this window.]\n");
        });
    }

    // Enable text input mode
    private void enableInput() {
        waitingForInput = true;
        Platform.runLater(() -> {
            terminalInput.setDisable(false);
            terminalInput.setVisible(true);
            terminalInput.requestFocus();
        });
    }

    // Enable password/hidden input mode
    private void enablePasswordInput() {
        waitingForInput = true;
        Platform.runLater(() -> {
            terminalInput.setDisable(false);
            terminalInput.setVisible(true);
            terminalInput.requestFocus();
        });
    }

    // Handle user submitting input
    @FXML
    private void onInputSubmit() {
        String input = terminalInput.getText();
        if (input == null || input.isEmpty()) input = "None";
        
        // Display input (text mode) or stars (password mode)
        if (inputMode == 1) {
            appendText(input + "\n");
        } else {
            appendText("*".repeat(Math.min(input.length(), 8)) + "\n");
        }
        
        terminalInput.clear();
        inputMode = 0;
        waitingForInput = false;
        
        try {
            client.send(input);
        } catch (IOException e) {
            appendText("\n[Send Error: " + e.getMessage() + "]\n");
        }
    }

    // Add text with ANSI colour parsing
    private void appendText(String text) {
        Platform.runLater(() -> {
            parseAnsiAndAdd(text, defaultColor);
            trimOutput();
        });
    }

    // Remove old text when terminal exceeds scroll area
    private void trimOutput() {
        terminalOutput.applyCss();
        terminalOutput.layout();
        
        double scrollHeight = scrollPane.getViewportBounds().getHeight();
        double textHeight = terminalOutput.getBoundsInParent().getHeight();
        
        while (textHeight > scrollHeight && terminalOutput.getChildren().size() > 1) {
            terminalOutput.getChildren().remove(0);
            terminalOutput.applyCss();
            terminalOutput.layout();
            textHeight = terminalOutput.getBoundsInParent().getHeight();
        }
        
        scrollPane.setVvalue(1.0);
    }

    // Parse ANSI escape codes and add coloured text segments
    private void parseAnsiAndAdd(String text, Color defaultColor) {
        Matcher matcher = ansiPattern.matcher(text);
        int lastEnd = 0;
        Color currentColor = defaultColor;

        while (matcher.find()) {
            // Add text before colour code with current colour
            if (matcher.start() > lastEnd) {
                String segment = text.substring(lastEnd, matcher.start());
                Text t = new Text(segment);
                t.setFill(currentColor);
                t.setFont(javafx.scene.text.Font.font("Courier New", 14));
                terminalOutput.getChildren().add(t);
            }
            
            // Parse colour code
            String code = matcher.group(1);
            if (code.isEmpty() || code.equals("0")) {
                currentColor = defaultColor;
            } else {
                Color mapped = colourMap.get(code);
                if (mapped != null) currentColor = mapped;
            }
            
            lastEnd = matcher.end();
        }

        // Add remaining text after last colour code
        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd);
            Text t = new Text(remaining);
            t.setFill(currentColor);
            t.setFont(javafx.scene.text.Font.font("Courier New", 14));
            terminalOutput.getChildren().add(t);
        }
    }

    // Handle server switch request
    private void handleSwitch(String newServer) {
        String[] parts = newServer.split(":");
        final String host;
        
        // Convert @ to localhost
        if (parts[0].equals("@")) {
            host = "127.0.0.1";
        } else {
            host = parts[0];
        }
        final int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 1998;
        
        Config cfg = Config.get();
        
        // Auto-switch if consent enabled, otherwise ask user
        if (cfg.isSwitchConsent()) {
            doSwitch(host, port);
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Switch Server");
                alert.setHeaderText("Switch to " + host + ":" + port + "?");
                alert.setContentText("The server is requesting to switch connections.");
                
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        doSwitch(host, port);
                    } else {
                        appendText("\n[Switch declined by user]\n");
                        enableInput();
                    }
                });
            });
        }
    }

    // Perform server switch - close current and connect to new
    private void doSwitch(String host, int port) {
        appendText("\n[Switching to " + host + ":" + port + "...]\n");
        
        running = false;
        client.close();
        
        Platform.runLater(() -> {
            try {
                Stage currentStage = stage;
                
                // Open new terminal window
                Stage newStage = new Stage();
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("terminal-view.fxml"));
                Scene scene = new Scene(fxmlLoader.load(), 1000, 700);
                newStage.setTitle("Terminal - " + host + ":" + port + " [Connecting...]");
                newStage.setScene(scene);
                newStage.show();

                // Initialise new terminal controller
                TerminalController controller = fxmlLoader.getController();
                controller.initialize(host, port, newStage);
                
                // Close old window
                currentStage.close();
            } catch (IOException e) {
                appendText("\n[Error switching: " + e.getMessage() + "]\n");
            }
        });
    }
}