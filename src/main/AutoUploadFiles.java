package main;

import javafx.application.Application;
import javafx.concurrent.WorkerStateEvent;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.commons.lang3.SystemUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributes;
import java.util.List;
import java.util.Properties;

/**
 * The main controller class of the JavaFX program.
 * Contains both the main(String[]) function to be run by JVM,
 * and the {@link Application}.start() function to be run by JavaFX.
 * Also handles the creation and management of JavaFX Application Thread
 * and the Task thread.
 * Also handles persistent data. Retrieves properties of existing
 * .properties file if one exists, creates .properties file if user wants
 * to save settings. File is hidden (dotted file is hidden on Unix,
 * "hidden" DOS attribute set on Windows).
 */
public class AutoUploadFiles extends Application {
    public static final String name = "AutoUploadFiles";
    public static final String version = "3.9";
    public static final String dialogTitle = name+" "+version;
    public static int textFieldWidth = 25;
    public Image icon = new Image(getClass().getResourceAsStream("icon.png"));
    private UploaderTask uploaderTask;
    private Properties properties;
    private File propertiesFile = new File(".autoUploadFiles.properties");

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
        properties = new Properties();
        if(propertiesFile.exists()) {
            FileInputStream inputStream = new FileInputStream(propertiesFile);
            properties.load(inputStream);
            inputStream.close();
        }
        MainWindow mainWindow = new MainWindow(this, primaryStage);
        mainWindow.run();
    }

    /**
     * Starts the {@link javafx.concurrent.Task} {@link Thread} that executes the FTP file upload.
     * If user wants to save settings, current values of hostname, port, username, and uploadPath
     * will be saved for future use.
     * @param hostname      Hostname for the FTP server.
     * @param port          Port for the FTP server.
     * @param username      Username for the FTP server login.
     * @param password      Password for the FTP server login.
     * @param uploadPath    Path on FTP server to upload to.
     * @param files         {@link File}(s) to upload to the FTP server.
     */
    public void startUploaderTask(String hostname, int port, String username,
                                  String password, String uploadPath, List<File> files) {
        uploaderTask = new UploaderTask(this, hostname, port, username, password, uploadPath, files);
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
     * Saves settings if checkbox is checked and exits program.
     * If OS is Windows, sets "hidden" DOS attribute.
     * @param saveSettings  Saves settings if checkbox is checked.
     */
    public void exit(boolean saveSettings) {
        if(saveSettings) {
            try {
                FileOutputStream outputStream = new FileOutputStream(propertiesFile);
                properties.store(outputStream, null);
                outputStream.close();
                if(SystemUtils.IS_OS_WINDOWS) {
                    Path propertiesNioPath = propertiesFile.toPath();
                    Files.setAttribute(propertiesNioPath, "dos:hidden", true);
                    DosFileAttributes attr = Files.readAttributes(propertiesNioPath, DosFileAttributes.class);
                    System.out.println("isHidden? "+attr.isHidden());
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        System.exit(0);
    }

    /**
     * Redirects System.out to {@link PrintStream} prs.
     * @param prs               {@link PrintStream} to redirect {@link System}.out to.
     */
    public void redirectOutput(PrintStream prs) {
        System.setOut(prs);
        System.setErr(prs);
    }

    /**
     * Creates an {@link Alert} showing the message and the icon AlertType.
     * @param message   The text of the message shown in the {@link Alert}.
     * @param alertType The icon shown in the {@link Alert} to designate success/failure/error.
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
     * Returns the current instance of {@link Properties} in use.
     * @return  Current instance of {@link Properties}.
     */
    public Properties getProperties() {
        return properties;
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
