package minip.p2pchat;

import java.io.*;
import java.net.*;

public class TestClient {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java minip.p2pchat.TestClient <host> <port> <message>");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String msg = args[2];
        try (Socket s = new Socket(host, port);
             PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
            out.println(msg);
            System.out.println("Sent message to " + host +":"+port);
        } catch (IOException e) {
            System.err.println("Failed: " + e.getMessage());
            System.exit(1);
        }
    }
}
