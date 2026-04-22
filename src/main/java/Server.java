import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

import Checkers.Pieces;
import javafx.application.Platform;
import javafx.scene.control.ListView;

public class Server {
    int count = 1;
    ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
    TheServer server;
    private Consumer<Serializable> SERVERLOG;

    private ArrayList<Players> players;
    private ArrayList<GameSession> activeGames;
    private HashMap<ClientThread, GameSession> sessions;

    Server(Consumer<Serializable> call) {
        SERVERLOG = call;
        server = new TheServer();
        server.start();
        players = new ArrayList<>();
        activeGames = new ArrayList<>();
        sessions = new HashMap<>();
    }

    public class TheServer extends Thread {
        public void run(){ //Start the server and wait for clients
            try(ServerSocket mysocket = new ServerSocket(5555);){
                System.out.println("Server is waiting for a client!");


                while(true) {
                    ClientThread c = new ClientThread(mysocket.accept(), count);
                    SERVERLOG.accept(new Message(Message.serverMessage,"client has connected to server: " + "client #" + count, "Server"));
                    clients.add(c);
                    players.add(new Players(c));
                    c.start();
                    count++;
                }
            }//end of try
            catch(Exception e) {
            SERVERLOG.accept(new Message(Message.serverMessage,"Server socket did not launch","Server"));
            }
        }//end of while
    }
        class ClientThread extends Thread {
        Socket connection;
        int count;
        String username;

        ObjectInputStream in;
        ObjectOutputStream out;

        ClientThread(Socket connection, int count) {
            this.connection = connection;
            this.count = count;
        }

            public void updateClients(Message message) { //Server sends a message to every client
                for(int i = 0; i < clients.size(); i++) {
                    ClientThread t = clients.get(i);
                    try {
                        t.out.writeObject(message);
                    }
                    catch(Exception e) {
                        System.out.println("Error writing to clients");
                    }
                }
            }

        public void run(){
            try{
                in = new ObjectInputStream(connection.getInputStream());
                out = new ObjectOutputStream(connection.getOutputStream());
                connection.setTcpNoDelay(true);
            }
            catch(Exception e){
                System.out.println("Streams not open");
            }
            while(true){
                try{
                    Object received = in.readObject();
                    if (received instanceof Checkers.Move){ //Player moves a piece
                        System.out.println("Move received from client");
                        Checkers.Move playerMove = (Checkers.Move)  received;
                        GameSession session = sessions.get(this);
                        System.out.println("Session: " + session);
                        System.out.println("Move piece: " + playerMove.getPiece());
                        System.out.println("From: " + playerMove.getpRow() + "," + playerMove.getpCol() +
                                " To: " + playerMove.getnRow() + "," + playerMove.getnCol());

                        if (session.rules.isValidMove(playerMove)){ //valid move check
                            session.playerOne.getClientThread().out.writeObject(playerMove);
                            session.playerTwo.getClientThread().out.writeObject(playerMove);
                            SERVERLOG.accept(new Message(Message.serverMessage, playerMove.getPiece().getColor().toString() +" Moved their piece!", "Server"));

                            boolean isJump = Math.abs(playerMove.getnRow() - playerMove.getpRow()) == 2; //check for multi jumps
                            if (isJump) {
                                ArrayList<Checkers.Move> extraJumps = session.rules.getMultiJumps(
                                        playerMove.getnRow(), playerMove.getnCol(), playerMove.getPiece().getColor());
                                for (Checkers.Move jump : extraJumps) {
                                    session.playerOne.getClientThread().out.writeObject(jump);
                                    session.playerTwo.getClientThread().out.writeObject(jump);
                                }
                            }
                            Pieces.Color winner = session.rules.checkForWinner();
                            if ( winner != null && winner.toString().equals("BLACK")){
                                session.playerOne.getClientThread().out.writeObject(new Message(Message.serverMessage, session.playerTwo.getClientThread().username + "Wins!", "Server"));
                                session.playerTwo.getClientThread().out.writeObject(new Message(Message.serverMessage, session.playerTwo.getClientThread().username + "Wins!","Server"));
                            }
                            else if(winner != null && winner.toString().equals("RED")){
                                session.playerOne.getClientThread().out.writeObject(new Message(Message.serverMessage, session.playerOne.getClientThread().username + "Wins!","Server"));
                                session.playerTwo.getClientThread().out.writeObject(new Message(Message.serverMessage, session.playerOne.getClientThread().username + "Wins!","Server"));
                            }
                        }
                        else { //move is invalid send error message in log and client
                            SERVERLOG.accept(new Message(Message.serverMessage, playerMove.getPiece().getColor().toString() +" Made an illegal move! Redo!", "Server" ));
                            out.writeObject(new Message (Message.error, "Invalid move! please try again!", "Server"));
                        }
                    }

                    else if(received instanceof Message){ //Player texts to another player or to create a new user
                        Message message = (Message) received;
                        if(message.getMsgType().equals(Message.createUser)) { //When user clicks on "Create User" send them here
                            if (checkUsername(message.getClient())){
                                SERVERLOG.accept(new Message(Message.serverMessage,"Username already exists!","Server"));
                                out.writeObject(new Message(Message.error, "Username already exists!", "Server"));
                            }
                            else { //Alerts everyone and the server that someone has joined the server
                                this.username = message.getClient();
                                SERVERLOG.accept(new Message(Message.serverMessage,this.username + " has joined the server!", "Server"));
                                updateClients(new Message(Message.serverMessage, message.getClient() + " has joined the server!", "Server"));
                                sendActiveUsers();
                            }
                        }
                        else if (message.getMsgType().equals(Message.sendToAll)) { //Send a message to all users
                            updateClients(new Message(Message.sendToAll, message.getMessage(), this.username));
                        }
                        else if (message.getMsgType().equals(Message.sendToIndvidual)) { //send a message to a certain person
                            Message indvidualMsg = new  Message(Message.sendToIndvidual, message.getMessage(), this.username,  message.getTarget());
                            for (ClientThread c : clients) {
                                if (c.username.equals(message.getTarget())) {
                                    c.out.writeObject(indvidualMsg);
                                }
                            }
                        }
                        else if (message.getMsgType().equals(Message.sendToGroup)) { //send a message to a group
                            Message groupMsg = new  Message(Message.sendToGroup, message.getMessage(), this.username, message.getGroupMembers());
                            for (ClientThread c : clients) {
                                if (message.getGroupMembers().contains(c.username)) {
                                    c.out.writeObject(groupMsg);
                                }
                            }
                        }
                        else if (message.getMsgType().equals(Message.status)){
                            for (Players p : players) {
                                if (p.getClientThread().equals(this)){
                                    p.setStatus(Players.Status.READY_TO_PLAY);
                                }
                            }
                        }
                        else if (message.getMsgType().equals(Message.challenge)) {
                            for (ClientThread c : clients) {
                                if (c.username != null && c.username.equals(message.getTarget())) {
                                    c.out.writeObject(new Message(Message.challenge,
                                            this.username + " wants to play!",
                                            this.username,
                                            message.getTarget()));
                                }
                            }
                        }
                        else if (message.getMsgType().equals(Message.challengeResponse)) {
                            if (message.getMessage().equals("Accept")) {
                                Players challenger = null;
                                Players accepter = null;
                                for (Players p : players) {
                                    if (p.getClientThread().username != null &&
                                            p.getClientThread().username.equals(message.getTarget())) {
                                        challenger = p;
                                    }
                                    if (p.getClientThread().equals(this)) {
                                        accepter = p;
                                    }
                                }
                                if (challenger != null && accepter != null) {
                                    GameSession newSession = new GameSession(new Checkers.Board(), challenger, accepter);
                                    activeGames.add(newSession);
                                    sessions.put(challenger.getClientThread(), newSession);
                                    sessions.put(accepter.getClientThread(), newSession);
                                    challenger.setStatus(Players.Status.IN_GAME);
                                    accepter.setStatus(Players.Status.IN_GAME);
                                    challenger.getClientThread().out.writeObject(new Message(Message.startGame, "RED", challenger.getClientThread().username, accepter.getClientThread().username));
                                    accepter.getClientThread().out.writeObject(new Message(Message.startGame, "BLACK", accepter.getClientThread().username, challenger.getClientThread().username));
                                    SERVERLOG.accept(new Message(Message.serverMessage,
                                            challenger.getClientThread().username + " vs " +
                                                    accepter.getClientThread().username + " game started!", "Server"));

                                }
                            } else if (message.getMessage().equals("Decline")) {
                                // Notify the challenger their challenge was declined
                                Players challenger = null;
                                Players accepter = null;

                                for (Players p : players) {
                                    if (p.getClientThread().username != null &&
                                            p.getClientThread().username.equals(message.getTarget())) {
                                        challenger = p;
                                    }
                                    if (p.getClientThread().equals(this)) {
                                        accepter = p;
                                    }
                                }
                                challenger.getClientThread().out.writeObject(new Message(Message.challengeResponse, "Decline", challenger.getClientThread().username, accepter.getClientThread().username));
//                                for (ClientThread c : clients) {
//                                    if (c.username != null && c.username.equals(message.getTarget())) {
//                                        c.out.writeObject(new Message(Message.serverMessage,
//                                                this.username + " declined your challenge!", "Server"));
//                                    }
//                                }
//                                SERVERLOG.accept(new Message(Message.serverMessage,
//                                        this.username + " declined a challenge!", "Server"));
                            }
                        }
                    }
                }
                catch(Exception e){
                    System.out.println("Exception: " + e.getClass().getName() + " - " + e.getMessage());
                    e.printStackTrace();
                    SERVERLOG.accept(e);                }
            }
        }
            public boolean checkUsername(String username){
                for(ClientThread c : clients){
                    if ( c.username != null && c.username.equals(username)){ //Check if username is taken return true if it is
                        return true;
                    }
                }
                return false;
            }

            public void sendActiveUsers(){ //create a new list that puts all client thread's username to the list
                ArrayList<String> activeUsers = new ArrayList<>();
                for(ClientThread c : clients){
                    if (c.username != null){
                        activeUsers.add(c.username);
                    }
                }
                updateClients(new Message(Message.userList, "Active Users", "Server", activeUsers));
            }
        }
    }


