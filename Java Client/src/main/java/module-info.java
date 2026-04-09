module com.example.javaedtion {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;


    opens com.example.javaedtion to javafx.fxml, com.fasterxml.jackson.databind;
    exports com.example.javaedtion;
}