package org.example.onllineauction;

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

public class ServerAuction {

    private static final int PORT = 7777;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static int startingAmount = 0;
    private static String playername = "";
    private static boolean auctionRunning = true;
    private static Map<String, Integer> bids = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("\t\t IPL Auction Server");
        System.out.println("\t\t=================\n\n");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            BufferedReader serverInput = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Enter the Player name and starting amount (e.g., Playername 100'): ");
            String startCommand = serverInput.readLine();
            String[] startArgs = startCommand.split(" ");
            if (startArgs.length != 2) {
                System.out.println("Invalid start command.");
                return;
            }
            playername = startArgs[0];
            startingAmount = Integer.parseInt(startArgs[1]);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client is connected.....\n\n");
                ClientHandler clientHandler = new ClientHandler(socket);
                clients.add(clientHandler);
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                out.println("Welcome to Auction for " + playername + ". Starting amount: " + startingAmount);
                out.println("Enter your username and bid amount in the format 'username amount': ");
                String input;
                while ((input = in.readLine()) != null) {
                    System.out.println("Team " + input + " Says: " + input);

                    if (input.equals("stop")) {
                        stopAuction();
                        return;
                    }

                    try {
                        String[] inputArgs = input.split(" ");
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
                        out.println("Bid placed successfully.");
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

        private void stopAuction() {
            auctionRunning = false;
            String winner = findWinner();
            System.out.println("Auction stopped. The winner is: " + winner);
            for (ClientHandler client : clients) {
                client.out.println("Auction stopped. The winner is: " + winner);
            }
        }

        private String findWinner() {
            return bids.entrySet().stream()
                    .max(Comparator.comparing(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .orElse("No winner");
        }
    }
}
