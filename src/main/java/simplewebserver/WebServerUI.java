/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package simplewebserver;

import javafx.application.Application;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.prefs.Preferences;
import javafx.application.Platform;
/**
 *
 * @author Ramifta
 */

public class WebServerUI extends Application {
    private WebServer webServer;
    private TextField filePathField;
    private TextField logsPathField;
    private TextField portField;
    private TextArea logArea;
    private final Preferences preferences = Preferences.userNodeForPackage(WebServerUI.class);
    private Button startButton;
    private Button stopButton;
    private Timeline logUpdater;
    private Set<String> displayedLogs = new HashSet<>();


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Simple Web Server");

        BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(10));
        borderPane.setStyle("-fx-background-color: #C8A2C8;");

        Label titleLabel = new Label("Simple Web Server");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        titleLabel.setAlignment(Pos.CENTER);
        BorderPane.setMargin(titleLabel, new Insets(10, 0, 10, 0));
        borderPane.setTop(titleLabel);
        BorderPane.setAlignment(titleLabel, Pos.CENTER);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Label portLabel = new Label("Port:");
        portField = new TextField(preferences.get("port", "8000")); //untuk mengambil nilai port 8000 sebagai nilai default jika belum pernah diisi inputan
        portField.setPrefWidth(200);
        grid.add(portLabel, 0, 1);
        grid.add(portField, 1, 1);

        Label pathLabel = new Label("File Path:");
        filePathField = new TextField(preferences.get("filePath", "D:\\Web\\Files"));
        filePathField.setPrefWidth(200);
        Button browseButton = new Button("Browse");
        browseButton.setOnAction(e -> browseFilePath(primaryStage));
        grid.addRow(2, pathLabel, filePathField, browseButton);

        Label logsPathLabel = new Label("Logs Path:");
        logsPathField = new TextField(preferences.get("logsPath", "D:\\Web\\logs"));
        logsPathField.setPrefWidth(200);
        Button logsBrowseButton = new Button("Browse");
        logsBrowseButton.setOnAction(e -> browseLogsPath(primaryStage));
        grid.addRow(3, logsPathLabel, logsPathField, logsBrowseButton);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(450);
        logArea.setPrefWidth(650);
        grid.add(logArea, 0, 4, 3, 1);

        startButton = new Button("Start");
        startButton.setStyle("-fx-background-color: #A65CA6; -fx-text-fill: white; -fx-font-weight: bold;");
        startButton.setOnAction(e -> startWebServer());

        stopButton = new Button("Stop");
        stopButton.setStyle("-fx-background-color: #A65CA6; -fx-text-fill: white; -fx-font-weight: bold;");
        stopButton.setOnAction(e -> stopWebServer());
        stopButton.setDisable(true);

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(startButton, stopButton);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        borderPane.setBottom(buttonBox);
        borderPane.setCenter(grid);

        Scene scene = new Scene(borderPane, 650, 450);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        
    }

    private void browseFilePath(Stage primaryStage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select File Path");
        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        if (selectedDirectory != null) {
            filePathField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    private void browseLogsPath(Stage primaryStage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Logs Path");
        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        if (selectedDirectory != null) {
            logsPathField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    private void startWebServer() {
        String filePath = filePathField.getText();
        String logsPath = logsPathField.getText();
        int port = Integer.parseInt(portField.getText());

        preferences.put("filePath", filePath);
        preferences.put("logsPath", logsPath);
        preferences.put("port", String.valueOf(port)); 
        //untuk menampilkan nilai port terakhir yang tersimpan kemudian dijalankan ketika server dimulai.
        

        if (webServer == null || !webServer.isAlive()) {
            if (!logArea.getText().isEmpty()) {
                appendToLog("\n");
            }

            webServer = new WebServer(filePath, logsPath, port);
            new Thread(() -> webServer.start()).start();
            appendToLog(String.format("[%s] Server started on port %d\n", new Date(), port));
            startButton.setDisable(true);
            stopButton.setDisable(false);

            // Start log updater
            startLogUpdater();
        } else {
            System.out.println("Server already running.");
        }
    }

    private void stopWebServer() {
        if (webServer != null && webServer.isAlive()) {
            webServer.stopServer();
            appendToLog(String.format("[%s] Server stopped\n", new Date()));
            stopButton.setDisable(true);
            startButton.setDisable(false);

            // Stop log updater
            stopLogUpdater();
        }
    }

    private void startLogUpdater() {
        logUpdater = new Timeline(new KeyFrame(Duration.seconds(2), e -> readLogs()));
        logUpdater.setCycleCount(Timeline.INDEFINITE);
        logUpdater.play();
    }

    private void stopLogUpdater() {
        if (logUpdater != null) {
            logUpdater.stop();
        }
    }


    private void readLogs() {
        if (webServer != null) {
            List<String> logs = webServer.loadAccessLogs();
            if (!logs.isEmpty()) {
                for (String log : logs) {
                    // Periksa apakah log telah ditampilkan sebelumnya
                    if (!displayedLogs.contains(log)) {
                        appendToLog(log + "\n");
                        displayedLogs.add(log); // Tambahkan log ke set displayedLogs
                    }
                }
            }
        }
    }

    private void appendToLog(String message) {
        Platform.runLater(() -> logArea.appendText(message));
    }

}
