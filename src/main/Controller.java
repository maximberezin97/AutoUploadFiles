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

/**
 * The main controller class of the JavaFX program.
 * Contains both the main(String[]) function to be run by JVM,
 * and the {@link Application}.start() function to be run by JavaFX.
 * Also handles the creation and management of JavaFX Application Thread
 * and the Task thread.
 */
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

    /**
     * Overrides the {@link Application}.start() function
     * that starts the JavaFX program.
     * @param primaryStage  {@link Stage} of the initial window.
     * @throws Exception    Necessary for override.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        MainWindow mainWindow = new MainWindow(this, primaryStage);
        mainWindow.run();
    }

    /**
     * Starts the {@link javafx.concurrent.Task} {@link Thread} that executes the FTP file upload.
     * @param hostname      hostname for the FTP server
     * @param port          port for the FTP server
     * @param username      username for the FTP server login
     * @param password      password for the FTP server login
     * @param uploadPath    path on FTP server to upload to
     * @param reuseSsl      reuse or create new SSL context for upload connection
     * @param passiveMode   passive or active mode for the FTP server
     * @param implicit      implicit or explicit connection for the FTP server
     * @param printErrors   redirect error {@link PrintStream} for errors, used for debugging error messages
     * @param files         {@link File}(s) to upload to the FTP server
     */
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

    /**
     * Attempts to cancel the FTP file upload.
     * First attempts to send FTP command "ABOR" to server.
     * Prints error stack trace if {@link IOException} is thrown/caught.
     * Then sets the {@link javafx.concurrent.Task} state to "CANCELLED"
     */
    public void cancelUploaderTask() {
        try {
            uploaderTask.cancelFtpUpload();
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            uploaderTask.cancel();
        }
    }

    /**
     * Redirects {@link System}.out to {@link PrintStream} {@param prs}.
     * @param prs           {@link PrintStream} to redirect {@link System}.out to
     * @param transferErrors    does or does not redirect {@link System}.err to {@param prs}, used for debugging
     */
    public void redirectOutput(PrintStream prs, boolean transferErrors) {
        System.setOut(prs);
        if(transferErrors) {
            System.setErr(prs);
        }
    }

    /**
     * Creates an {@link Alert} showing the {@param message} and the icon {@alertType}.
     * @param message   The text of the message shown in the {@link Alert}
     * @param alertType The icon shown in the {@link Alert} to designate success/failure/error
     */
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

    /**
     * Returns the current instance of the {@link UploaderTask} in use.
     * @return  Current instance of {@link UploaderTask}.
     */
    public UploaderTask getUploaderTask() {
        return uploaderTask;
    }

    /**
     * Returns the title of the dialog.
     * Used for the title of {@link Stage}s in case of a name change.
     * @return {@link String} of the dialog title.
     */
    public String getDialogTitle() {
        return dialogTitle;
    }

    /**
     * Returns the icon of the program.
     * Icon is an {@link Image} object containing an image file.
     * Used as the icon of the program and in windows.
     * @return  {@link Image} of the icon.
     */
    public Image getIcon() {
        return icon;
    }

    /**
     * Returns the width in characters of the text fields
     * in the {@link UploaderWindow} in case of a width adjustment.
     * @return  int of the width of the text fields in character widths.
     */
    public static int getTextFieldWidth() {
        return textFieldWidth;
    }
}
