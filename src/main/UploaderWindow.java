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

public class UploaderWindow implements Runnable {
    private Controller controller;
    private Stage window;
    private TextArea textArea;
    private Label statusLabel;
    private ProgressBar progressBar;
    private Label percentLabel;
    private Button cancelButton;
    private Button closeButton;

    public UploaderWindow() {
        controller = null;
        window = new Stage();
        textArea = newTextArea();
        statusLabel = new Label("Connecting to FTP server...");
        progressBar = new ProgressBar();
        percentLabel = new Label("0.0%");
        cancelButton = newCancelButton();
        closeButton = newCloseButton();
    }
    public UploaderWindow(Controller controller) {
        this();
        this.controller = controller;
    }

    @Override
    public void run() {
        initUploaderWindow();
    }
    private void initUploaderWindow() {
        window.setTitle(controller.getDialogTitle());
        window.getIcons().add(controller.getIcon());
        window.setResizable(true);

        statusLabel.textProperty().unbind();
        statusLabel.textProperty().bind(controller.getUploaderTask().titleProperty());
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(controller.getUploaderTask().progressProperty());
        percentLabel.textProperty().unbind();
        percentLabel.textProperty().bind(controller.getUploaderTask().progressProperty().multiply(100).asString("%.2f").concat("%"));
        textArea.textProperty().unbind();
        textArea.textProperty().bind(controller.getUploaderTask().stringProperty());

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
    private Button newCancelButton() {
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> {
            controller.cancelUploaderTask();
        });
        cancelButton.setCancelButton(true);
        return cancelButton;
    }
    private Button newCloseButton() {
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> {
            controller.redirectOutput(System.out, true);
            Stage stage = (Stage) closeButton.getScene().getWindow();
            stage.close();
            if(controller.getUploaderTask().isRunning()) {
                controller.cancelUploaderTask();
            }
        });
        closeButton.setCancelButton(true);
        return closeButton;
    }
}
