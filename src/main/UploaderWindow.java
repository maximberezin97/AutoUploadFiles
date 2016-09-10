package main;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * The window used to show the status of the FTP upload.
 * Features a status label, progress bar and percentage indicator,
 * a console showing the FTP commands being sent between this client
 * and the server, which can also show errors if debug was checked,
 * as well as cancel and close buttons.
 * Status, progress, and percentage indicators are bound to their
 * respective {@link javafx.beans.property.Property} values
 * in the {@link UploaderTask} and are updated between {@link Thread}s.
 */
public class UploaderWindow implements Runnable {
    private AutoUploadFiles autoUploadFiles;
    private Stage window;
    private TextArea textArea;
    private Label statusLabel;
    private ProgressBar progressBar;
    private Label percentLabel;
    private Button cancelButton;
    private Button closeButton;

    /**
     * Empty constructor for the uploader window.
     * Initializes all declared fields to default values.
     */
    public UploaderWindow() {
        autoUploadFiles = null;
        window = new Stage();
        textArea = newTextArea();
        statusLabel = new Label("");
        progressBar = new ProgressBar();
        percentLabel = new Label("");
        cancelButton = newCancelButton();
        closeButton = newCloseButton();
    }

    /**
     * Constructor that takes in the {@link AutoUploadFiles}.
     * @param autoUploadFiles    Main {@link AutoUploadFiles} instance of the program.
     */
    public UploaderWindow(AutoUploadFiles autoUploadFiles) {
        this();
        this.autoUploadFiles = autoUploadFiles;
    }

    /**
     * Overrides the {@link Runnable}.run() method.
     * Runs this class in a unique thread.
     */
    @Override
    public void run() {
        initUploaderWindow();
    }

    /**
     * Constructs the window from initialized fields and shows it.
     */
    private void initUploaderWindow() {
        window.setTitle(autoUploadFiles.getDialogTitle());
        window.getIcons().add(autoUploadFiles.getIcon());
        window.setResizable(true);

        statusLabel.textProperty().unbind();
        statusLabel.textProperty().bind(autoUploadFiles.getUploaderTask().titleProperty());
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(autoUploadFiles.getUploaderTask().progressProperty());
        percentLabel.textProperty().unbind();
        percentLabel.textProperty().bind(autoUploadFiles.getUploaderTask().progressProperty().multiply(100).asString("%.2f").concat("%"));
        textArea.textProperty().unbind();
        textArea.textProperty().bind(autoUploadFiles.getUploaderTask().stringProperty());

        progressBar.setMaxWidth(Double.MAX_VALUE);
        VBox vboxProgressBar = new VBox(progressBar);
        vboxProgressBar.setFillWidth(true);
        percentLabel.setAlignment(Pos.CENTER_LEFT);

        GridPane topGrid = new GridPane();
        topGrid.setHgap(10);
        topGrid.setVgap(6);
        ColumnConstraints column0 = new ColumnConstraints();
        column0.setHgrow(Priority.ALWAYS);
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setMinWidth(50);
        topGrid.getColumnConstraints().addAll(column0, column1);

        topGrid.add(statusLabel, 0, 0);
        topGrid.add(vboxProgressBar, 0, 1);
        topGrid.add(percentLabel, 1, 1);

        HBox hboxButtons = new HBox(6);
        hboxButtons.getChildren().addAll(cancelButton, closeButton);
        BorderPane borderBottom = new BorderPane();
        borderBottom.setRight(hboxButtons);

        BorderPane border = new BorderPane();
        border.setPadding(new Insets(10));
        border.setTop(topGrid);
        border.setCenter(textArea);
        border.setMargin(textArea, new Insets(10, 0, 10, 0));
        border.setBottom(borderBottom);

        window.setScene(new Scene(border));
        window.show();
    }

    /**
     * Constructs a new {@link TextArea} for the console that
     * listens for updates and scrolls to the bottom automatically.
     * @return  {@link TextArea} for the console.
     */
    private TextArea newTextArea() {
        TextArea textArea = new TextArea("");
        textArea.setWrapText(false);
        textArea.setEditable(false);
        textArea.textProperty().addListener(e -> {
            Platform.runLater(() -> {
                textArea.setScrollTop(Double.MAX_VALUE);
            });
        });
        return textArea;
    }

    /**
     * Returns a new {@link Button} that cancels the {@link UploaderTask}.
     * @return  {@link Button} that cancels the upload.
     */
    private Button newCancelButton() {
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> {
            autoUploadFiles.cancelUploaderTask();
        });
        cancelButton.setCancelButton(true);
        return cancelButton;
    }

    /**
     * Returns a new {@link Button} that closes the {@link UploaderWindow}
     * and returns console output to {@link System}.out.
     * Cancels the {@link UploaderTask} if it is running.
     * @return  {@link Button} that closes the window and cancels the upload
     *          if running.
     */
    private Button newCloseButton() {
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> {
            autoUploadFiles.redirectOutput(System.out, true);
            Stage stage = (Stage) closeButton.getScene().getWindow();
            stage.close();
            if(autoUploadFiles.getUploaderTask().isRunning()) {
                autoUploadFiles.cancelUploaderTask();
            }
        });
        closeButton.setCancelButton(true);
        return closeButton;
    }
}
