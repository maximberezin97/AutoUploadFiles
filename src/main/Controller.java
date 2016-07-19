package main;

import javafx.application.Application;
import javafx.concurrent.WorkerStateEvent;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

//Maxim Berezin - 16 July 2016
public class Controller extends Application {
    public static final String name = "AutoUploadFiles";
    public static final String version = "3.6";
    public static final String dialogTitle = name+" "+version;
    public Image icon = new Image(getClass().getResourceAsStream("icon.png"));
    public static int textFieldWidth = 25;

    private UploaderTask uploaderTask;

    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void start(Stage primaryStage) throws Exception {
        MainWindow mainWindow = new MainWindow(this, primaryStage);
        mainWindow.run();
    }
    public void startUploaderTask(String hostname, int port, String username, String password, String uploadPath,
                                  boolean reuseSsl, boolean passiveMode, boolean implicit, boolean printErrors, List<File> files) {
        uploaderTask = new UploaderTask(this, hostname, port, username, password, uploadPath,
                reuseSsl, passiveMode, implicit, printErrors, files);
        uploaderTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, e -> {
            UploaderTaskResult result = uploaderTask.getValue();
            showAlert(result.getMessage(), result.getAlertType());
        });
        UploaderWindow uploaderWindow = new UploaderWindow(this);
        uploaderWindow.run();
        Thread uploaderThread = new Thread(uploaderTask);
        uploaderThread.setDaemon(true);
        uploaderThread.start();
    }
    public void cancelUploaderTask() {
        try {
            uploaderTask.cancelFtpUpload();
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            uploaderTask.cancel();
        }
    }

    public void redirectOutput(PrintStream prs, boolean transferErrors) {
        System.setOut(prs);
        if(transferErrors) {
            System.setErr(prs);
        }
    }
    public void showAlert(String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(dialogTitle);
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(icon);
        //stage.initModality(Modality.APPLICATION_MODAL);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    public UploaderTask getUploaderTask() {
        return uploaderTask;
    }
    public String getDialogTitle() {
        return dialogTitle;
    }
    public Image getIcon() {
        return icon;
    }
    public static int getTextFieldWidth() {
        return textFieldWidth;
    }
}
