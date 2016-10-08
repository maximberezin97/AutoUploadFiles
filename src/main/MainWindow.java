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

/**
 * The first window that is shown at the start of the program.
 * Used to configure the information in preparation for the upload.
 * Has fields for connection, login, and upload information,
 * checkboxes for connection and debugging settings,
 * and buttons and lists to show the selected file(s) and begin connection.
 */
public class MainWindow implements Runnable {
    private AutoUploadFiles autoUploadFiles;
    private List<File> files;
    private Stage window;
    private Label hostnameLabel;
    private Label portLabel;
    private Label usernameLabel;
    private Label passwordLabel;
    private Label uploadPathLabel;
    private Label saveSettingsLabel;
    private Label reuseSslLabel;
    private Label passiveModeLabel;
    private Label explicitLabel;
    private Label printErrorsLabel;
    private TextField hostnameInput;
    private TextField portInput;
    private TextField usernameInput;
    private PasswordField passwordInput;
    private TextField uploadPathInput;
    private CheckBox saveSettingsCheckbox;
    private CheckBox reuseSslCheckbox;
    private CheckBox passiveModeCheckbox;
    private CheckBox explicitCheckbox;
    private CheckBox printErrorsCheckbox;

    /**
     * Empty constructor for the main window.
     * Initializes all declared fields to default values.
     */
    public MainWindow() {
        window = null;
        hostnameLabel = new Label("Hostname:");
        portLabel = new Label("Port:");
        usernameLabel = new Label("Username:");
        passwordLabel = new Label("Password:");
        uploadPathLabel = new Label("Upload Path:");
        saveSettingsLabel = new Label("Save Settings:");
        reuseSslLabel = new Label("Reuse SSL:");
        passiveModeLabel = new Label("Passive Mode:");
        explicitLabel = new Label("Explicit Mode:");
        printErrorsLabel = new Label("Debug:");
        hostnameInput = newTextField("Enter the hostname here...", new Tooltip("Hostname of FTP server"));
        portInput = newTextField("Enter the port here...", new Tooltip("Port of the FTP server"));
        usernameInput = newTextField("Enter the username here...", new Tooltip("Username for the FTP server"));
        uploadPathInput = newTextField("Enter the upload path here...",
                new Tooltip("Path on the FTP server to upload the file(s) to"));
        passwordInput = newPasswordInput();
        saveSettingsCheckbox = newCheckbox(false, new Tooltip(
                "If checked, current settings will be saved for future use.\n" +
                "If unchecked, settings will be blank in future use."));
        reuseSslCheckbox = newCheckbox(true, new Tooltip(
                "If checked, will reuse SSL session from control transport for data transport.\n" +
                "If unchecked, will create new SSL session for data transport."));
        passiveModeCheckbox = newCheckbox(true, new Tooltip(
                "If checked, will transfer data over random port selected by server.\n" +
                "If unchecked, will transfer data over same port as control transport."));
        explicitCheckbox = newCheckbox(true, new Tooltip(
                "If checked, will initiate TLS encryption after connecting to the server.\n" +
                "If unchecked, will initiate TLS encryption immediately after connecting to the server on port 990."));
        printErrorsCheckbox = newCheckbox(true, new Tooltip(
                "If checked, console will print errors encountered during upload.\n" +
                "If unchecked, console will only print FTP commands."));
        autoUploadFiles = null;
    }

    /**
     * Constructor that takes in the {@link AutoUploadFiles} and a given {@link Stage}.
     * @param autoUploadFiles    Main {@link AutoUploadFiles} instance of the program.
     * @param primaryStage  {@link Stage} given from call to be shown.
     */
    public MainWindow(AutoUploadFiles autoUploadFiles, Stage primaryStage) {
        this();
        this.autoUploadFiles = autoUploadFiles;
        this.window = primaryStage;
        hostnameInput.setText(autoUploadFiles.getProperties().getProperty("hostname"));
        portInput.setText(autoUploadFiles.getProperties().getProperty("port"));
        usernameInput.setText(autoUploadFiles.getProperties().getProperty("username"));
        uploadPathInput.setText(autoUploadFiles.getProperties().getProperty("uploadPath"));
    }

    /**
     * Overrides the {@link Runnable}.run() method.
     * Runs this class in a unique thread.
     */
    @Override
    public void run() {
        initMainWindow();
    }

    /**
     * Constructs the window from initialized fields and shows it.
     */
    private void initMainWindow() {
        window.setTitle(autoUploadFiles.getDialogTitle());
        window.getIcons().add(autoUploadFiles.getIcon());

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
        HBox hboxCheckbox = new HBox(6, saveSettingsLabel, saveSettingsCheckbox, reuseSslLabel, reuseSslCheckbox,
                passiveModeLabel, passiveModeCheckbox, explicitLabel, explicitCheckbox, printErrorsLabel, printErrorsCheckbox);
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

    /**
     * Creates a new {@link TextField} with the given parameters.
     * @param promptText    The text to show in the field when it is empty.
     * @param tooltip       The {@link Tooltip} to show when the mouse hovers over the field.
     * @return              {@link TextField} with the given parameters.
     */
    private TextField newTextField(String promptText, Tooltip tooltip) {
        TextField input = new TextField();
        input.setPrefColumnCount(autoUploadFiles.getTextFieldWidth());
        input.setPromptText(promptText);
        input.setTooltip(tooltip);
        return input;
    }

    /**
     * Returns a new {@link PasswordField} for the password.
     * @return  {@link PasswordField} for the password.
     */
    private PasswordField newPasswordInput() {
        PasswordField passwordInput = new PasswordField();
        passwordInput.setPrefColumnCount(autoUploadFiles.getTextFieldWidth());
        passwordInput.setPromptText("Enter the password here...");
        passwordInput.setTooltip(new Tooltip("Password for the FTP server"));
        return passwordInput;
    }

    /**
     * Returns a new {@link CheckBox} with the provided initial setting
     * and tooltip.
     * @param setSelected   If true, checkbox will be checked initially.
     *                      If false, checkbox will be unchecked initially.
     * @param tooltip       {@link Tooltip} for the new {@link CheckBox}.
     * @return              {@link CheckBox} instance with the provided state and tooltip.
     */
    private CheckBox newCheckbox(boolean setSelected, Tooltip tooltip) {
        CheckBox checkBox = new CheckBox();
        checkBox.setAllowIndeterminate(false);
        checkBox.setSelected(setSelected);
        checkBox.setTooltip(tooltip);
        return checkBox;
    }

    /**
     * Returns a new {@link Button} that selects an array of {@link File}s when used.
     * Also updates the {@link VBox} to display the array of {@link File}s when used.
     * @param vboxFileLabels    {@link VBox} to update with array of {@link File}s.
     * @return                  {@link Button} to select files.
     */
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

    /**
     * Returns a new {@link Button} that retrieves the fields' configurations
     * and either alerts the user to enter valid configurations, or tells the
     * the {@link AutoUploadFiles} to start the {@link UploaderTask}.
     * @return  {@link Button} to begin connection and upload.
     */
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
                    autoUploadFiles.showAlert("Please enter the hostname.", Alert.AlertType.WARNING);
                } else if(port == -1) {
                    autoUploadFiles.showAlert("Please enter a valid port number.", Alert.AlertType.WARNING);
                } else if(username.equals("")) {
                    autoUploadFiles.showAlert("Please enter the username.", Alert.AlertType.WARNING);
                } else if(password.equals("")) {
                    autoUploadFiles.showAlert("Please enter the password.", Alert.AlertType.WARNING);
                } else if (uploadPath.equals("")) {
                    autoUploadFiles.showAlert("Please enter the upload path.", Alert.AlertType.WARNING);
                } else {
                    autoUploadFiles.startUploaderTask(hostname, port, username, password, uploadPath,
                            reuseSsl, passiveMode, implicit, printErrors, files);
                }
            } else {
                autoUploadFiles.showAlert("Please select the file(s).", Alert.AlertType.WARNING);
            }
        });
        connectButton.setTooltip(new Tooltip("Connect to FTP server, upload file(s)"));
        return connectButton;
    }

    /**
     * Returns a new {@link Button} that uses {@link System}.exit(0) to quit the program.
     * @return  {@link Button} that exits the program.
     */
    private Button newCancelButton() {
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> {
            if(hostnameInput.getText() != null) {
                autoUploadFiles.getProperties().setProperty("hostname", hostnameInput.getText());
            }
            if(portInput.getText() != null) {
                autoUploadFiles.getProperties().setProperty("port", portInput.getText());
            }
            if(usernameInput.getText() != null) {
                autoUploadFiles.getProperties().setProperty("username", usernameInput.getText());
            }
            if(uploadPathInput.getText() != null) {
                autoUploadFiles.getProperties().setProperty("uploadPath", uploadPathInput.getText());
            }
            autoUploadFiles.exit(saveSettingsCheckbox.isSelected());
        });
        cancelButton.setCancelButton(true);
        return cancelButton;
    }

    /**
     * Attempts to parse the {@link String} portString into an int value.
     * If the parse is invalid and throws an exception, it is caught
     * and a value of -1 is returned.
     * @param portString    {@link String} to be parsed into an int.
     * @return              The parsed value of the portString, or -1.
     */
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
