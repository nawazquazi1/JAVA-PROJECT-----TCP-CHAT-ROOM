import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
    private ArrayList<ConnectionHandler> connectionHandlersList;
    private ServerSocket server;
    private boolean done;
    private ExecutorService poll;

    public Server() {
        connectionHandlersList = new ArrayList<>();
        done = false;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            poll = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                ConnectionHandler connectionHandler = new ConnectionHandler(client);
                connectionHandlersList.add(connectionHandler);
                poll.execute(connectionHandler);
            }
        } catch (Exception e) {
            shutdown();
        }
    }

    public void broadCast(String message) {
        for (ConnectionHandler ch : connectionHandlersList) {
            if (ch != null) {
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown() {
        try {
            done = true;
            poll.shutdown();
            if (!server.isClosed()) {
                server.close();
            }
            for (ConnectionHandler ch : connectionHandlersList) {
                ch.shutdown();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    class ConnectionHandler implements Runnable {
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickName;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Please enter a nickname");
                nickName = in.readLine();
                System.out.println(nickName + " connected!");
                broadCast(nickName + " joined the chat!");
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/nick")) {
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2) {
                            broadCast(nickName + " renamed themselves to " + messageSplit[1]);
                            System.out.println(nickName + " renamed themselves to " + messageSplit[1]);
                            nickName = messageSplit[1];
                            out.println("SuccessFully Changed nickname to " + nickName);
                        } else {
                            out.println("No Nickname provided");
                        }
                    } else if (message.startsWith("/quit")) {
                        broadCast(nickName + " left the chat!");
                        shutdown();
                    } else {
                        broadCast(nickName + " : " + message);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown() {
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        System.out.println("server is starting.....");
        server.run();
    }
}

