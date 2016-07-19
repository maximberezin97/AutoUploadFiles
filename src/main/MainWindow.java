package main;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Iterator;
import java.util.List;

public class MainWindow implements Runnable {
    private Controller controller;
    private List<File> files;
    private Stage window;
    private Label hostnameLabel;
    private Label portLabel;
    private Label usernameLabel;
    private Label passwordLabel;
    private Label uploadPathLabel;
    private Label reuseSslLabel;
    private Label passiveModeLabel;
    private Label explicitLabel;
    private Label printErrorsLabel;
    private TextField hostnameInput;
    private TextField portInput;
    private TextField usernameInput;
    private PasswordField passwordInput;
    private TextField uploadPathInput;
    private CheckBox reuseSslCheckbox;
    private CheckBox passiveModeCheckbox;
    private CheckBox explicitCheckbox;
    private CheckBox printErrorsCheckbox;

    public MainWindow() {
        window = null;
        hostnameLabel = new Label("Hostname:");
        portLabel = new Label("Port:");
        usernameLabel = new Label("Username:");
        passwordLabel = new Label("Password:");
        uploadPathLabel = new Label("Upload Path:");
        reuseSslLabel = new Label("Reuse SSL:");
        passiveModeLabel = new Label("Passive Mode:");
        explicitLabel = new Label("Explicit Mode:");
        printErrorsLabel = new Label("Debug:");
        hostnameInput = newHostnameInput();
        portInput = newPortInput();
        usernameInput = newUsernameInput();
        passwordInput = newPasswordInput();
        uploadPathInput = newUploadPathInput();
        reuseSslCheckbox = newCheckbox(true, newReuseSslTooltip());
        passiveModeCheckbox = newCheckbox(true, newPassiveModeTooltip());
        explicitCheckbox = newCheckbox(true, newExplicitTooltip());
        printErrorsCheckbox = newCheckbox(true, newPrintErrorsTooltip());
        controller = null;
    }
    public MainWindow(Controller controller, Stage primaryStage) {
        this();
        this.controller = controller;
        this.window = primaryStage;
    }

    @Override
    public void run() {
        initMainWindow();
    }
    private void initMainWindow() {
        window.setTitle(controller.getDialogTitle());
        window.getIcons().add(controller.getIcon());

        GridPane gridTop = new GridPane();
        gridTop.setHgap(10);
        gridTop.setVgap(6);

        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        gridTop.getColumnConstraints().addAll(new ColumnConstraints(), column1);

        gridTop.add(hostnameLabel, 0, 0);
        gridTop.add(portLabel, 0, 1);
        gridTop.add(usernameLabel, 0, 2);
        gridTop.add(passwordLabel, 0, 3);
        gridTop.add(uploadPathLabel, 0, 4);
        gridTop.add(hostnameInput, 1, 0);
        gridTop.add(portInput, 1, 1);
        gridTop.add(usernameInput, 1, 2);
        gridTop.add(passwordInput, 1, 3);
        gridTop.add(uploadPathInput, 1, 4);
        HBox hboxCheckbox = new HBox(6, reuseSslLabel, reuseSslCheckbox, passiveModeLabel,
                passiveModeCheckbox, explicitLabel, explicitCheckbox, printErrorsLabel, printErrorsCheckbox);
        VBox vboxGridTop = new VBox(10, gridTop, hboxCheckbox);

        VBox vboxFileLabels = new VBox(10, new Label("No file(s) selected."));
        vboxFileLabels.setAlignment(Pos.CENTER_LEFT);
        Button selectFileButton = newSelectFileButton(vboxFileLabels);
        Button connectButton = newConnectButton();
        Button cancelButton = newCancelButton();
        HBox hboxBottomRight = new HBox(6, connectButton, cancelButton);

        BorderPane borderBottom = new BorderPane();
        borderBottom.setLeft(selectFileButton);
        borderBottom.setCenter(vboxFileLabels);
        BorderPane.setMargin(vboxFileLabels, new Insets(0, 10, 0, 10));
        borderBottom.setRight(hboxBottomRight);

        BorderPane border = new BorderPane();
        border.setPadding(new Insets(12));
        border.setTop(vboxGridTop);
        border.setBottom(borderBottom);
        BorderPane.setMargin(borderBottom, new Insets(6, 0, 0, 0));

        window.setScene(new Scene(border));
        window.show();
    }

    private TextField newHostnameInput() {
        TextField hostnameInput = new TextField();
        hostnameInput.setPrefColumnCount(controller.getTextFieldWidth());
        hostnameInput.setPromptText("Enter the hostname here...");
        hostnameInput.setTooltip(new Tooltip("Hostname of FTP server"));
        return hostnameInput;
    }
    private TextField newPortInput() {
        TextField portInput = new TextField();
        portInput.setPrefColumnCount(controller.getTextFieldWidth());
        portInput.setPromptText("Enter the port here...");
        portInput.setTooltip(new Tooltip("Port of FTP server"));
        return portInput;
    }
    private TextField newUsernameInput() {
        TextField usernameInput = new TextField();
        usernameInput.setPrefColumnCount(controller.getTextFieldWidth());
        usernameInput.setPromptText("Enter the username here...");
        usernameInput.setTooltip(new Tooltip("Username for the FTP server"));
        return usernameInput;
    }
    private PasswordField newPasswordInput() {
        PasswordField passwordInput = new PasswordField();
        passwordInput.setPrefColumnCount(controller.getTextFieldWidth());
        passwordInput.setPromptText("Enter the password here...");
        passwordInput.setTooltip(new Tooltip("Password for the FTP server"));
        return passwordInput;
    }
    private TextField newUploadPathInput() {
        TextField uploadPathInput = new TextField();
        uploadPathInput.setPrefColumnCount(controller.getTextFieldWidth());
        uploadPathInput.setPromptText("Enter the upload path here...");
        uploadPathInput.setTooltip(new Tooltip("Path on the FTP server to upload the file(s) to"));
        return uploadPathInput;
    }
    private CheckBox newCheckbox(boolean setSelected, Tooltip tooltip) {
        CheckBox checkBox = new CheckBox();
        checkBox.setAllowIndeterminate(false);
        checkBox.setSelected(setSelected);
        checkBox.setTooltip(tooltip);
        return checkBox;
    }
    private Tooltip newReuseSslTooltip() {
        return new Tooltip("If checked, will reuse SSL session from control transport for data transport.\n" +
                "If unchecked, will create new SSL session for data transport.");
    }
    private Tooltip newPassiveModeTooltip() {
        return new Tooltip("If checked, will transfer data over random port selected by server.\n" +
                "If unchecked, will transfer data over same port as control transport.");
    }
    private Tooltip newExplicitTooltip() {
        return new Tooltip("If checked, will initiate TLS encryption after connecting to the server.\n" +
                "If unchecked, will initiate TLS encryption immediately after connecting to the server on port 990.");
    }
    private Tooltip newPrintErrorsTooltip() {
        return new Tooltip("If checked, console will print errors encountered during upload.\n" +
                "If unchecked, console will only print FTP commands.");
    }
    private Button newSelectFileButton(VBox vboxFileLabels) {
        Button selectFileButton = new Button("Select File(s)");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")+File.separator+"Downloads"));
        selectFileButton.setOnAction(e -> {
            files = fileChooser.showOpenMultipleDialog(window);
            ObservableList<Node> listLabels = vboxFileLabels.getChildren();
            listLabels.remove(0, listLabels.size());
            if(files != null) {
                for(Iterator<File> it = files.iterator(); it.hasNext();) {
                    listLabels.add(new Label(it.next().getName()));
                }
            } else {
                listLabels.add(new Label("No file(s) selected."));
            }
        });
        selectFileButton.setTooltip(new Tooltip("Select file(s) to upload"));
        return selectFileButton;
    }
    private Button newConnectButton() {
        Button connectButton = new Button("Connect");
        connectButton.setOnAction(e -> {
            if(files != null) {
                String hostname = hostnameInput.getText();
                int port = portStringToInt(portInput.getText());
                String username = usernameInput.getText();
                String password = passwordInput.getText();
                String uploadPath = uploadPathInput.getText();
                boolean reuseSsl = reuseSslCheckbox.isSelected();
                boolean passiveMode = passiveModeCheckbox.isSelected();
                boolean implicit = !explicitCheckbox.isSelected();
                boolean printErrors = printErrorsCheckbox.isSelected();
                if(hostname.equals("")) {
                    controller.showAlert("Please enter the hostname.", Alert.AlertType.WARNING);
                } else if(port == -1) {
                    controller.showAlert("Please enter a valid port number.", Alert.AlertType.WARNING);
                } else if(username.equals("")) {
                    controller.showAlert("Please enter the username.", Alert.AlertType.WARNING);
                } else if(password.equals("")) {
                    controller.showAlert("Please enter the password.", Alert.AlertType.WARNING);
                } else if (uploadPath.equals("")) {
                    controller.showAlert("Please enter the upload path.", Alert.AlertType.WARNING);
                } else {
                    controller.startUploaderTask(hostname, port, username, password, uploadPath,
                            reuseSsl, passiveMode, implicit, printErrors, files);
                }
            } else {
                controller.showAlert("Please select the file(s).", Alert.AlertType.WARNING);
            }
        });
        connectButton.setTooltip(new Tooltip("Connect to FTP server, upload file(s)"));
        return connectButton;
    }
    private Button newCancelButton() {
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> System.exit(0));
        cancelButton.setCancelButton(true);
        return cancelButton;
    }
    public int portStringToInt(String portString) {
        int port = -1;
        try {
            port = Integer.valueOf(portString);
        } catch (NumberFormatException numFormat) {
            numFormat.printStackTrace();
        } finally {
            return port;
        }
    }
}
