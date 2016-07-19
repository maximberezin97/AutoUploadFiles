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
    public UploaderTask(Controller controller) {
        this();
        this.controller = controller;
    }
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
    private UploaderTaskResult executeFileUpload() throws Exception {
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
                    if(!isCancelled()) {
                        currentFile = it.next();
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

    private FTPSClient getRegFtpsClient(boolean isExplicit) {
        return new FTPSClient(isExplicit) {
            @Override
            public boolean storeFile(String remote, InputStream local) throws IOException {
                return super.storeFile(remote, local);
            }
        };
    }
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
    private PrintStream newPrintStream() {
        return new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                updateString(String.valueOf((char) b));
            }
        });
    }
    private CopyStreamAdapter newCopyStreamAdapter() {
        return new CopyStreamAdapter() {
            @Override
            public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
                updateProgress(totalBytesTransferred, currentFile.length());
            }
        };
    }
    public boolean cancelFtpUpload() throws IOException {
        return ftp.abort();
    }
    private String getSuccessMessage(long millis) {
        StringBuilder message = new StringBuilder();
        if(files.size() == 1) {
            message.append(files.size()+" file was successfully uploaded over ");
        } else {
            message.append(files.size()+" files were successfully uploaded over ");
        }
        long seconds = millis/1000;
        if(seconds >= 60) {
            long minutes = seconds/60;
            seconds = seconds%60;
            if(minutes == 1) {
                message.append(minutes+" minute");
            } else {
                message.append(minutes+" minutes");
            }
            if(seconds > 1) {
                message.append(" and "+seconds+" seconds");
            } else if(seconds == 1) {
                message.append(" and "+seconds+" second");
            }
            message.append(".");
        } else {
            if(seconds == 1) {
                message.append(seconds+" second.");
            } else {
                message.append(seconds+" seconds.");
            }
        }
        return message.toString();
    }
    public final StringProperty stringProperty() {
        return stringProperty;
    }
    public final String getString() {
        return stringProperty.get();
    }
    public final void setString(String set) {
        stringProperty.set(set);
    }
    public final void appendString(String set) {
        setString(getString()+set);
    }
    public final void updateString(String set) {
        if(Platform.isFxApplicationThread()) {
            appendString(set);
        } else {
            Platform.runLater(() -> {
                appendString(set);
            });
        }
    }
}
