package org.example.onllineauction;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class ClientAuction extends Application {

    static int port = 7777;
    private BufferedReader in;
    private PrintWriter out;
    private TextArea logTextArea;
    private TextField playerNameField;
    private TextField bidField;
    private Button bidButton;
    private Button stopButton;
    private boolean auctionStopped = false;

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setAlignment(Pos.CENTER);

        logTextArea = new TextArea();
        logTextArea.setEditable(false);
        root.getChildren().add(logTextArea);

        HBox playerNameBox = new HBox(10);
        playerNameBox.setAlignment(Pos.CENTER);
        Label playerNameLabel = new Label("Player Name: ");
        playerNameField = new TextField();
        playerNameBox.getChildren().addAll(playerNameLabel, playerNameField);

        HBox bidBox = new HBox(10);
        bidBox.setAlignment(Pos.CENTER);
        Label bidLabel = new Label("Your Bid: ");
        bidField = new TextField();
        bidButton = new Button("Bid");
        bidButton.setOnAction(e -> sendBid());
        bidBox.getChildren().addAll(bidLabel, bidField, bidButton);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        stopButton = new Button("Stop");
        stopButton.setOnAction(e -> stopAuction());
        buttonBox.getChildren().add(stopButton);

        root.getChildren().addAll(playerNameBox, bidBox, buttonBox);

        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.setTitle("Auction Client");
        primaryStage.setOnCloseRequest(e -> System.exit(0));
        primaryStage.show();

        try {
            String ipAddressString = "192.168.43.177";
            InetAddress ipAddress = InetAddress.getByName(ipAddressString);
            Socket socket = new Socket(ipAddress, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            new Thread(this::receiveMessages).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                String finalMessage = message;
                Platform.runLater(() -> {
                    logTextArea.appendText("Server Says: " + finalMessage + "\n");
                    if (finalMessage.contains("Auction stopped")) {
                        stopButton.setDisable(true);
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendBid() {
        if (!auctionStopped) {
            String playerName = playerNameField.getText().trim();
            String bid = bidField.getText().trim();
            if (!playerName.isEmpty() && !bid.isEmpty()) {
                out.println(playerName + " " + bid);
            }
        } else {
            logTextArea.appendText("Auction is already stopped.\n");
        }
    }

    private void stopAuction() {
        if (!auctionStopped) {
            auctionStopped = true;
            out.println("stop");
        } else {
            logTextArea.appendText("Auction is already stopped.\n");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
