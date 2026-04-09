package com.example.javaedtion;

// LauncherController - Main application entry point
// Displays server selection UI and launches terminal windows

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;

public class LauncherController extends Application {
    
    // FXML UI elements
    @FXML
    private TextField nameField;      // Server name input
    @FXML
    private TextField ipField;        // IP address input
    @FXML
    private TextField portField;      // Port input
    @FXML
    private ListView<Server> serverList;  // Saved servers list
    @FXML
    private CheckBox switchConsentCheck;  // Auto-switch setting
    
    // Data storage
    private final ServerList serverListData = new ServerList();
    private final ObservableList<Server> observableServers = FXCollections.observableArrayList();

    @Override
    // Initialise launcher window
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(LauncherController.class.getResource("launch-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 400, 500);
        stage.setTitle("TelePy Java Edition Launcher");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    // Initialise after FXML loaded - set up list cell factory and load data
    public void initialize() {
        // Custom cell renderer for server list
        serverList.setCellFactory(lv -> new ListCell<Server>() {
            @Override
            protected void updateItem(Server item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                    setTextFill(javafx.scene.paint.Color.LIME);
                }
            }
        });
        
        serverList.setItems(observableServers);
        refreshServerList();
        loadSettings();
    }

    // Load settings from config file
    private void loadSettings() {
        Config cfg = Config.get();
        switchConsentCheck.setSelected(cfg.isSwitchConsent());
    }

    // Refresh displayed server list from storage
    private void refreshServerList() {
        observableServers.clear();
        observableServers.addAll(serverListData.getAll());
    }

    @FXML
    // Add new server to saved list
    private void onAddServer() {
        String name = nameField.getText();
        String ip = ipField.getText();
        String portStr = portField.getText();

        // Apply defaults for empty fields
        if (name == null || name.isEmpty()) name = "Server";
        if (ip == null || ip.isEmpty()) ip = "127.0.0.1";
        if (ip.equals("@")) ip = "127.0.0.1";
        
        int port = 1998;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) { }

        serverListData.add(new Server(name, ip, port));
        refreshServerList();
        
        // Clear input fields after adding
        nameField.clear();
        ipField.clear();
        portField.clear();
    }

    @FXML
    // Connect to selected server or manual input
    private void onConnect() {
        Server selected = serverList.getSelectionModel().getSelectedItem();
        String ip = ipField.getText();
        String portStr = portField.getText();

        String host;
        int port;

        // Use selected server or manual input
        if (selected != null) {
            host = selected.getHost();
            port = selected.getPort();
        } else if (ip != null && !ip.isEmpty()) {
            host = ip.equals("@") ? "127.0.0.1" : ip;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                port = 1998;
            }
        } else {
            return;
        }

        launchTerminal(host, port);
    }

    @FXML
    // Save current settings to config file
    private void onSaveSettings() {
        Config cfg = Config.get();
        cfg.setSwitchConsent(switchConsentCheck.isSelected());
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Settings Saved");
        alert.setHeaderText(null);
        alert.setContentText("Your settings have been saved.");
        alert.showAndWait();
    }

    @FXML
    // Reset all settings and saved servers
    private void onResetSettings() {
        Config.get().reset();
        serverListData.clearAll();
        refreshServerList();
        switchConsentCheck.setSelected(false);
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Settings Reset");
        alert.setHeaderText(null);
        alert.setContentText("All settings and servers have been reset.");
        alert.showAndWait();
    }

    @FXML
    // Delete selected server from saved list
    private void onDeleteServer() {
        int selectedIndex = serverList.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            serverListData.remove(selectedIndex);
            refreshServerList();
        }
    }

    // Open new terminal window and connect to server
    private void launchTerminal(String host, int port) {
        try {
            Stage terminalStage = new Stage();
            FXMLLoader fxmlLoader = new FXMLLoader(LauncherController.class.getResource("terminal-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1000, 700);
            terminalStage.setTitle("Terminal - " + host + ":" + port + " [Connecting...]");
            terminalStage.setScene(scene);
            terminalStage.show();

            // Initialise terminal with connection details
            TerminalController controller = fxmlLoader.getController();
            controller.initialize(host, port, terminalStage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}