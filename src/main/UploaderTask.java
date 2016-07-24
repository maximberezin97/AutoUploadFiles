package main;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.io.CopyStreamAdapter;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * An extension of JavaFX's {@link Task} to be run in a new thread separate from
 * the JavaFX Application Thread. Upon call, creates an FTP client instance,
 * configures connection to the server, and uploads the file(s) to the server.
 * Returns an {@link UploaderTaskResult} that contains the message and alert type
 * for the {@link Controller}, whether successful or failed.
 */
public class UploaderTask extends Task<UploaderTaskResult> {
    private Controller controller;
    private String hostname;
    private int port;
    private String username;
    private String password;
    private String uploadPath;
    private boolean reuseSsl;
    private boolean passiveMode;
    private boolean implicit;
    private boolean printErrors;
    private List<File> files;
    private File currentFile;
    private FTPSClient ftp;
    private StringProperty stringProperty;

    /**
     * Empty constructor for {@link UploaderTask}. Most values are set to empty non-null,
     * file and FTP values left null.
     */
    public UploaderTask() {
        this.controller = null;
        this.hostname = "";
        this.port = -1;
        this.username = "";
        this.password = "";
        this.uploadPath = "";
        this.reuseSsl = true;
        this.passiveMode = true;
        this.implicit = true;
        this.files = null;
        this.ftp = null;
        this.currentFile = null;
        this.stringProperty = new SimpleStringProperty("");
    }

    /**
     * Constructor for {@link UploaderTask}, calls empty constructor,
     * then sets provided {@link Controller}.
     * @param controller Controller class of the program.
     */
    public UploaderTask(Controller controller) {
        this();
        this.controller = controller;
    }

    /**
     * Constructor for {@link UploaderTask}. Calls {@link Controller} constructor,
     * then sets FTP values.
     * @param controller    Controller class of the program.
     * @param hostname      Hostname of the FTP server.
     * @param port          Port of the FTP server.
     * @param username      Username for the FTP server login.
     * @param password      Password for the FTP server login.
     * @param uploadPath    Path on FTP server to upload the file(s) to.
     * @param reuseSsl      Reuse or create new SSL context.
     * @param passiveMode   Passive or active mode connection.
     * @param implicit      Implicit or explicit connection.
     * @param printErrors   Print errors in console or {@link UploaderWindow}, used for debugging.
     * @param files         File(s) to upload to FTP server.
     */
    public UploaderTask(Controller controller, String hostname, int port, String username, String password, String uploadPath,
                        boolean reuseSsl, boolean passiveMode, boolean implicit, boolean printErrors, List<File> files) {
        this(controller);
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
        this.uploadPath = uploadPath;
        this.reuseSsl = reuseSsl;
        this.passiveMode = passiveMode;
        this.implicit = implicit;
        this.printErrors = printErrors;
        this.files = files;
    }

    /**
     * Override of {@link Task}.call(), starts the task in the thread.
     * @return Result of task to be shown in an alert by {@link Controller}.
     */
    @Override
    protected UploaderTaskResult call() {
        UploaderTaskResult result;
        try {
            result = executeFileUpload();
        } catch(Exception e) {
            e.printStackTrace();
            result = new UploaderTaskResult("Error encountered during file upload.", Alert.AlertType.ERROR);
        }
        return result;
    }

    /**
     * Creates the FTP client instance and uploads the file(s) to the FTP server.
     * Uses the designated FTP values and print stream.
     * @return              {@link UploaderTaskResult} of the FTP file upload to be shown in an alert by {@link Controller}.
     * @throws IOException  If thrown by FTP client command functions.
     */
    private UploaderTaskResult executeFileUpload() throws IOException {
        UploaderTaskResult result;
        long timeStart;
        long timeEnd;
        boolean fileStored = false;

        if(reuseSsl) {
            ftp = getAltFtpsClient(implicit);
        } else {
            ftp = getRegFtpsClient(implicit);
        }

        PrintStream printStream = newPrintStream();
        controller.redirectOutput(printStream, printErrors);
        ftp.addProtocolCommandListener(new PrintCommandListener(printStream, true));
        ftp.setCopyStreamListener(newCopyStreamAdapter());

        updateTitle("Connecting to FTP server...");
        ftp.connect(hostname, port);
        if(!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
            result = new UploaderTaskResult("Could not connect to FTP server.", Alert.AlertType.ERROR);
        } else {
            updateTitle("Logging into FTP server...");
            if(!ftp.login(username, password)) {
                result =  new UploaderTaskResult("Incorrect username/password.", Alert.AlertType.ERROR);
            } else {
                updateTitle("Configuring FTP connection...");
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                ftp.setFileTransferMode(FTP.BINARY_FILE_TYPE);
                if(passiveMode) {
                    ftp.execPBSZ(0);
                    ftp.execPROT("P");
                    ftp.enterLocalPassiveMode();
                }
                ftp.changeWorkingDirectory(uploadPath);
                InputStream fileStream;
                timeStart = System.currentTimeMillis();
                for(Iterator<File> it = files.iterator(); it.hasNext();) {
                    currentFile = it.next();
                    if(!isCancelled()) {
                        updateTitle("Uploading file \""+currentFile.getName()+"\"...");
                        fileStream = new FileInputStream(currentFile);
                        fileStored = ftp.storeFile(currentFile.getName(), fileStream);
                        fileStream.close();
                    }
                    if(!fileStored) break;
                }
                timeEnd = System.currentTimeMillis();
                if(fileStored) {
                    updateTitle("Finished uploading to FTP server.");
                    result = new UploaderTaskResult(getSuccessMessage(timeEnd-timeStart), Alert.AlertType.INFORMATION);
                } else {
                    result = new UploaderTaskResult("FTP file upload failed.", Alert.AlertType.ERROR);
                }
            }
            ftp.logout();
        }
        ftp.disconnect();
        return result;
    }

    /**
     * Returns an {@link FTPSClient} instance as provided by the Apache Commons Net library.
     * @param isExplicit    Explicit or implicit connection.
     * @return              {@link FTPSClient} with standard storeFile().
     */
    private FTPSClient getRegFtpsClient(boolean isExplicit) {
        return new FTPSClient(isExplicit) {
            @Override
            public boolean storeFile(String remote, InputStream local) throws IOException {
                return super.storeFile(remote, local);
            }
        };
    }

    /**
     * Returns an {@link FTPSClient} instance that creates a new SSL data socket for storeFile().
     * @param isExplicit    Explicit or implicit connection.
     * @return              {@link FTPSClient} with overridden _prepareDataSocket_().
     */
    private FTPSClient getAltFtpsClient(boolean isExplicit) {
        return new FTPSClient(isExplicit) {
            @Override
            protected void _prepareDataSocket_(final Socket socket) throws IOException {
                if (socket instanceof SSLSocket) {
                    final SSLSession session = ((SSLSocket) _socket_).getSession();
                    final SSLSessionContext context = session.getSessionContext();
                    try {
                        final Field sessionHostPortCache = context.getClass().getDeclaredField("sessionHostPortCache");
                        sessionHostPortCache.setAccessible(true);
                        final Object cache = sessionHostPortCache.get(context);
                        final Method putMethod = cache.getClass().getDeclaredMethod("put", Object.class, Object.class);
                        putMethod.setAccessible(true);
                        final Method getHostMethod = socket.getClass().getDeclaredMethod("getHost");
                        getHostMethod.setAccessible(true);
                        Object host = getHostMethod.invoke(socket);
                        final String key = String.format("%s:%s", host, String.valueOf(socket.getPort())).toLowerCase(Locale.ROOT);
                        putMethod.invoke(cache, key, session);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    /**
     * Returns a new {@link PrintStream} that updates the {@link StringProperty}
     * of this uploader task upon calling the write(int) function.
     * @return  {@link PrintStream} with overridden write(int).
     */
    private PrintStream newPrintStream() {
        return new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                updateString(String.valueOf((char) b));
            }
        });
    }

    /**
     * Returns a new {@link CopyStreamAdapter} that updates the Progress property
     * of this uploader task upon calling the bytesTransferred(long, int, long) function.
     * Called when {@link FTPSClient}.storeFile() stores an array of bytes to the FTP server.
     * @return {@link CopyStreamAdapter} with overridden bytesTransferred(long, int, long).
     */
    private CopyStreamAdapter newCopyStreamAdapter() {
        return new CopyStreamAdapter() {
            @Override
            public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
                updateProgress(totalBytesTransferred, currentFile.length());
            }
        };
    }

    /**
     * Attempts to cancel the FTP upload by sending the "ABOR" command to the FTP server.
     * @return              Successful or failed "ABOR" command execution.
     * @throws IOException  If thrown by FTP client command function.
     */
    public boolean cancelFtpUpload() throws IOException {
        return ftp.abort();
    }

    /**
     * Creates a message to show in an alert if the FTP file upload was successful.
     * @param millis    Time length of FTP file upload in milliseconds.
     * @return          {@link String} of the message for the alert.
     */
    private String getSuccessMessage(long millis) {
        StringBuilder message = new StringBuilder();
        int fileQuantity = files.size();
        message.append(fileQuantity);
        if(fileQuantity == 1) {
            message.append(" file was successfully uploaded over ");
        } else {
            message.append(" files were successfully uploaded over ");
        }
        long seconds = millis/1000;
        if(seconds >= 60) {
            long minutes = seconds/60;
            seconds = seconds%60;
            message.append(minutes);
            if(minutes == 1) {
                message.append(" minute");
            } else {
                message.append(" minutes");
            }
            message.append(" and ");
            message.append(seconds);
            if(seconds > 1) {
                message.append(" seconds");
            } else if(seconds == 1) {
                message.append(" second");
            }
            message.append(".");
        } else {
            message.append(seconds);
            if(seconds == 1) {
                message.append(" second.");
            } else {
                message.append(" seconds.");
            }
        }
        return message.toString();
    }

    /**
     * Returns the {@link StringProperty} contained in this uploader task.
     * This is a manually implemented {@link StringProperty} and is not
     * one of the {@link javafx.beans.property.Property} values automatically
     * created for a standard {@link Task}, such as progress, title, or message.
     * May be removed in the future and replaced with message, which will need
     * its updateMessage(String) overridden to run under {@link Platform}.runLater().
     * When previously used, message did not update with the console properly.
     * @return  {@link StringProperty} of this uploader task.
     */
    public final StringProperty stringProperty() {
        return stringProperty;
    }

    /**
     * Returns the string contained in the {@link StringProperty}.
     * @return  String in the {@link StringProperty}.
     */
    public final String getString() {
        return stringProperty.get();
    }

    /**
     * Sets the value of the {@link StringProperty}, removing its previous value.
     * @param set   The new value of the {@link StringProperty}.
     */
    public final void setString(String set) {
        stringProperty.set(set);
    }

    /**
     * Appends the value of the set parameter to the previous value of the {@link StringProperty}.
     * @param set   The value to add to the {@link StringProperty}.
     */
    public final void appendString(String set) {
        setString(getString()+set);
    }

    /**
     * Appends the value of the set parameter to the previous value of the {@link StringProperty}.
     * Uses {@link Platform}.runLater() if there is information crossing threads.
     * @param set   The value to add to the {@link StringProperty}.
     */
    public final void updateString(String set) {
        if(Platform.isFxApplicationThread()) {
            appendString(set);
        } else {
            Platform.runLater(() -> appendString(set));
        }
    }
}
