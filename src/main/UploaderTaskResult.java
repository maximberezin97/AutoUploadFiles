package main;

import javafx.scene.control.Alert;

public class UploaderTaskResult {
    private String message;
    private Alert.AlertType alertType;

    public UploaderTaskResult(String message, Alert.AlertType alertType) {
        this.message = message;
        this.alertType = alertType;
    }

    public String getMessage() {
        return message;
    }
    public Alert.AlertType getAlertType() {
        return alertType;
    }
}
