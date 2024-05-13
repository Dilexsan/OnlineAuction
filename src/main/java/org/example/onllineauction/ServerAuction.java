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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerAuction extends Application {

    private static final int PORT = 7777;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static String playerName = "";
    private static int startingAmount = 0;
    private static boolean auctionRunning = true;
    private static Map<String, Integer> bids = new ConcurrentHashMap<>();
    private TextArea logTextArea;
    private TextField playerNameField;
    private TextField startingAmountField;
    private static List<PrintWriter> allClients = new ArrayList<>(); // To keep track of all connected clients' PrintWriter objects

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setAlignment(Pos.CENTER);

        Label nameLabel = new Label("Player Name:");
        playerNameField = new TextField();
        Label amountLabel = new Label("Starting Amount:");
        startingAmountField = new TextField();

        Button startButton = new Button("Start Auction");
        startButton.setOnAction(e -> startAuction());

        logTextArea = new TextArea();
        logTextArea.setEditable(false);

        VBox inputBox = new VBox(10);
        inputBox.getChildren().addAll(nameLabel, playerNameField, amountLabel, startingAmountField, startButton);

        root.getChildren().addAll(inputBox, logTextArea);

        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.setTitle("Auction Server");
        primaryStage.setOnCloseRequest(e -> stopServer());
        primaryStage.show();
    }

    private void startAuction() {
        playerName = playerNameField.getText();
        startingAmount = Integer.parseInt(startingAmountField.getText());

        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                Platform.runLater(() -> logTextArea.appendText("\t\t IPL Auction Server\n\t\t=================\n\n"));
                Platform.runLater(() -> logTextArea.appendText("Auction for " + playerName + ". Starting amount: " + startingAmount + "\n"));

                while (true) {
                    Socket socket = serverSocket.accept();
                    Platform.runLater(() -> logTextArea.appendText("Client is connected.....\n\n"));
                    ClientHandler clientHandler = new ClientHandler(socket, logTextArea);
                    clients.add(clientHandler);
                    clientHandler.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void stopServer() {
        auctionRunning = false;
        for (ClientHandler client : clients) {
            client.sendToClient("Auction stopped. The winner is: " + findWinner());
        }
        Platform.exit();
        System.exit(0);
    }

    private static String findWinner() {
        return bids.entrySet().stream()
                .max(Comparator.comparing(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse("No winner");
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;
        private TextArea logTextArea;

        public ClientHandler(Socket socket, TextArea logTextArea) {
            this.socket = socket;
            this.logTextArea = logTextArea;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                allClients.add(out); // Add PrintWriter of this client to the list
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                out.println("Welcome to Auction for " + playerName + ". Starting amount: " + startingAmount);
                out.println("Enter your team name and bid amount in the format ' Team Name Amount': ");
                String input;
                while ((input = in.readLine()) != null) {
                    String[] inputArgs = input.split(" ");
                    if (input.equals("stop")) {
                        stopAuction();
                        return;
                    }

                    Platform.runLater(() -> logTextArea.appendText("Team " + inputArgs[0] + " says: " + inputArgs[1] + "\n"));

                    try {
                        if (inputArgs.length != 2) {
                            out.println("Invalid input format.");
                            continue;
                        }
                        username = inputArgs[0];
                        int bid = Integer.parseInt(inputArgs[1]);
                        if (bid < startingAmount) {
                            out.println("Bid amount must be greater than or equal to the starting amount.");
                            continue;
                        }

                        bids.put(username, bid);
                        out.println(username + " Bidding " + bid);

                        // Broadcast bid update to all clients
                        for (PrintWriter clientOut : allClients) {
                            clientOut.println("Team " + inputArgs[0] + " says: " + inputArgs[1]);
                        }

                    } catch (NumberFormatException e) {
                        out.println("Invalid input. Please enter a number as your bid.");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                    in.close();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        private void stopServer() {
            auctionRunning = false;
            String winner = findWinner();
            Platform.runLater(() -> logTextArea.appendText("Auction stopped. The winner is: " + winner + "\n"));
            for (ClientHandler client : clients) {
                client.sendToClient("Auction stopped. The winner is: " + winner);
            }
            Platform.exit();
            System.exit(0);
        }


        private void stopAuction() {
            auctionRunning = false;
            for (ClientHandler client : clients) {
                client.sendToClient("Auction stopped. The winner is: " + findWinner());
            }
        }

        private void sendToClient(String message) {
            out.println(message);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}