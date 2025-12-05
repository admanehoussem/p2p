package minip.p2pchat;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;

public class ChatPeer {
    private final int listenPort;
    private ServerSocket serverSocket;
    private final CopyOnWriteArrayList<Socket> peers = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private volatile boolean running = true;
    private Thread consoleThread;
    private final List<String> messageHistory = new CopyOnWriteArrayList<>();

    public ChatPeer(int listenPort) { this.listenPort = listenPort; }

    public void start() throws IOException {
        serverSocket = new ServerSocket(listenPort);
        System.out.println("Listening on port " + listenPort);
        executor.submit(this::acceptLoop);
        consoleThread = new Thread(this::consoleLoop);
        consoleThread.setDaemon(false);
        consoleThread.start();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket s = serverSocket.accept();
                peers.add(s);
                System.out.println("Peer connected: " + s.getRemoteSocketAddress());
                executor.submit(() -> readLoop(s));
            } catch (IOException e) {
                if (running) e.printStackTrace();
            }
        }
    }

    private void readLoop(Socket s) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                String formattedMsg = "[From " + s.getRemoteSocketAddress() + "] " + line;
                System.out.println(formattedMsg);
                // Store received message in history
                messageHistory.add(formattedMsg);
            }
        } catch (IOException e) {
            // ignore
        } finally {
            peers.remove(s);
            try { s.close(); } catch (IOException ignore) {}
            System.out.println("Peer disconnected: " + s.getRemoteSocketAddress());
        }
    }

    private void consoleLoop() {
        try (Scanner scanner = new Scanner(System.in)) {
            String line;
            while (running && scanner.hasNextLine()) {
                line = scanner.nextLine();
                if (line == null || !running) break;
                
                if (line.startsWith("/connect ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 3) {
                        String host = parts[1];
                        int port = Integer.parseInt(parts[2]);
                        connectToPeer(host, port);
                    } else {
                        System.out.println("Usage: /connect host port");
                    }
                } else if (line.equals("/exit")) {
                    shutdown();
                    break;
                } else if (line.equals("/history")) {
                    showHistory();
                } else if (!line.trim().isEmpty()) {
                    broadcast(line);
                    // Store sent message in history
                    messageHistory.add("[Sent] " + line);
                }
            }
        } catch (Exception e) {
            if (running) e.printStackTrace();
        }
    }

    private void connectToPeer(String host, int port) {
        try {
            Socket s = new Socket(host, port);
            peers.add(s);
            System.out.println("Connected to " + s.getRemoteSocketAddress());
            executor.submit(() -> readLoop(s));
        } catch (IOException e) {
            System.out.println("Failed to connect: " + e.getMessage());
        }
    }

    private void broadcast(String msg) {
        List<Socket> toRemove = new ArrayList<>();
        for (Socket s : peers) {
            try {
                if (s.isClosed() || !s.isConnected()) {
                    toRemove.add(s);
                    continue;
                }
                PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                out.println(msg);
            } catch (IOException e) {
                System.out.println("Failed to send to " + s.getRemoteSocketAddress() + ": " + e.getMessage());
                toRemove.add(s);
            }
        }
        peers.removeAll(toRemove);
    }

    private void showHistory() {
        System.out.println("\n=== Message History ===");
        if (messageHistory.isEmpty()) {
            System.out.println("No messages yet.");
        } else {
            for (String msg : messageHistory) {
                System.out.println(msg);
            }
        }
        System.out.println("=======================\n");
    }

    private void shutdown() {
        running = false;
        try { 
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); 
            }
        } catch (IOException ignored) {}
        for (Socket s : peers) {
            try { 
                if (s != null && !s.isClosed()) {
                    s.close(); 
                }
            } catch (IOException ignored) {}
        }
        peers.clear();
        if (consoleThread != null) {
            consoleThread.interrupt();
        }
        executor.shutdownNow();
        System.out.println("Shutting down.");
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java minip.p2pchat.ChatPeer <listenPort>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        ChatPeer cp = new ChatPeer(port);
        cp.start();
        
        // Attendre que le serveur s'arrête
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            cp.shutdown();
        }));
        
        // Garder le thread principal actif jusqu'à l'arrêt
        try {
            while (cp.running) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            cp.shutdown();
        }
    }
}
