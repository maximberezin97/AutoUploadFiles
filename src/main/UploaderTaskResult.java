package main;

import javafx.scene.control.Alert;

/**
 * Container for information used for {@link AutoUploadFiles} to generate an {@link Alert}.
 * Contains a {@link String} message and an {@link javafx.scene.control.Alert.AlertType} for the icon.
 */
public class UploaderTaskResult {
    private String message;
    private Alert.AlertType alertType;

    /**
     * Constructs a new {@link UploaderTaskResult} with the provided message and alert type.
     * @param message   Message to show in the alert.
     * @param alertType Alert type icon to show in the alert.
     */
    public UploaderTaskResult(String message, Alert.AlertType alertType) {
        this.message = message;
        this.alertType = alertType;
    }

    /**
     * Returns the message of the alert.
     * @return  The message of the alert.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the alert type of the alert.
     * @return The alert type of the alert.
     */
    public Alert.AlertType getAlertType() {
        return alertType;
    }
}
