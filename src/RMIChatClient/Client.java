package RMIChatClient;

import View.ChatGUI;
import RMIChatServer.Server;
import RMIChatServer.ServerInterface;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import static java.lang.System.exit;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class representing the server for the chat program 
 * Each peer has a client
 *
 * @author Devin Grant-Miles
 */
public class Client implements ClientInterface {

    private String clientName;
    private int clientPortNumber; //this exists so that other clients can connect after election. 
    private String hostURL;
    private String ownURL;
    private ServerInterface serverInterface;
    private ChatGUI view;
    private boolean participant;
    private Server server;
    private ArrayList<ClientInterface> clientList;

    public Client(String name, ServerInterface peerServer, int port) throws RemoteException {

        //Set GUI
        this.view = new ChatGUI();
        this.view.setVisible(true);

        //Instatiate server interface and client name + port
        this.serverInterface = peerServer;
        this.clientName = name;
        this.clientPortNumber = port;

        //this would change if the the program was being run on seperate machines
        this.hostURL = "localhost";
        this.ownURL = "localhost";

        //boolean for leader elections
        this.participant = false;

        clientList = new ArrayList();

        //action listener for send button
        view.getSendButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    //election starts here
                    System.out.println("Starting election");
                    startElection();

                    //elected server sends messages and updates each clients chat view
                    getServerInterface().sendMessages(clientName + ": " + view.getChatTextField().getText());
                    //reset chat view with no text
                    view.getChatTextField().setText("");
                    System.out.println("Election complete and message sent");
                } catch (RemoteException ex) {
                    Logger.getLogger(Client.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        );

        //action listener for quit button
        view.getQuitButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    leaveChat();
                    destroyChat();
                } catch (RemoteException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        );
    }

    //returns the client's current server interface
    public ServerInterface getServerInterface() {
        return serverInterface;
    }

    //add client to the peer to peer system (server client list)
    public void addClient() throws RemoteException {
        serverInterface.addChatClient(clientPortNumber, ownURL);
    }

    //update message view for this client
    @Override
    public void getMessages(String message) throws RemoteException {
        view.getChatTextArea().append(message + "\n");
    }

    //leave the chat and remove from all client and server lists
    public void leaveChat() throws RemoteException {
        serverInterface.removeChatClient(this.clientName);
    }

    //destroy the chat window
    public void destroyChat() throws RemoteException {
        this.view.dispose();
        exit(0);
    }

    //update this client list so peer to peer system is synced
    @Override
    public void updateClientList() throws RemoteException {
        int j = 0;
        ArrayList<ClientInterface> list = serverInterface.getClientList();
        this.clientList.clear();
        this.clientList.addAll(list);
    }

    public String getClientName() {
        return clientName;
    }

    //start the leader election
    public void startElection() {
        //set to true so only votes once
        this.participant = true;

        //got through client list until own name is found and the send election message to the next client on this list to simiulate ring arrangement
        for (int i = 0; i < this.clientList.size(); i++) {
            try {
                if (this.clientList.get(i).getClientName().compareTo(this.clientName) == 0) {
                    if (clientList.size() == (i + 1)) {
                        serverInterface.getClientList().get(0).recieveElectionMessage(clientName);
                    } else {
                        serverInterface.getClientList().get((i + 1)).recieveElectionMessage(clientName);
                    }
                }
            } catch (RemoteException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    //recieve election message method
    public void recieveElectionMessage(String message) throws RemoteException {

        System.out.println("Recieved election message");
        
        //elect by alphabetical order of client name for proof of concept
        if (message.compareTo(this.clientName) < 0) {
            System.out.println("I am smaller. Passing message on");

            this.participant = true;
            passMessageOn(message);//send to next client
        }
        
        //if message is own name then this peer is the leader so new server created
        if (message.compareTo(this.clientName) == 0) {
            this.server = new Server();
            //set that this is the server

            //bind new server stub to registry if not already
            ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(server, this.clientPortNumber);//alternative is for server to extend UnicastRemoteObject
            Registry thisRegistry = LocateRegistry.getRegistry(this.clientPortNumber);
            thisRegistry.rebind("Server", stub);//binds if not already

            //set the new server's client list with current peers
            server.setClientList(clientList);
            System.out.println("I am now the server");

            //send leader message around ring
            try {
                sendLeaderMessage(this.clientPortNumber);
            } catch (NotBoundException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //vote for self if election message is lower than own
        if (message.compareTo(this.clientName) > 0) {
            System.out.println("I am larger. Starting election for me.");

            //check that peer has not already voted
            if (!this.participant) {
                //start election i.e vote for self
                startElection();
            }
        }
    }

    //method for passing message on around ring if smaller than current election message
    public void passMessageOn(String message) {
        for (int i = 0; i < this.clientList.size(); i++) {
            try {
                if (this.clientList.get(i).getClientName().compareTo(this.clientName) == 0) {
                    if (clientList.size() == (i + 1)) {
                        serverInterface.getClientList().get(0).recieveElectionMessage(message);
                    } else {
                        serverInterface.getClientList().get((i + 1)).recieveElectionMessage(message);
                    }
                }
            } catch (RemoteException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    //leader message method for once leader is chosen
    public void sendLeaderMessage(int port) throws NotBoundException {
        for (int i = 0; i < this.clientList.size(); i++) {
            try {
                if (this.clientList.get(i).getClientName().compareTo(this.clientName) == 0) {
                    if (clientList.size() == (i + 1)) {
                        serverInterface.getClientList().get(0).recieveLeaderMessage(port);
                    } else {
                        serverInterface.getClientList().get((i + 1)).recieveLeaderMessage(port);
                    }
                }
            } catch (RemoteException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    //method for when leader message is recieved
    //each client connects to the new port number (no url required due to all on localhost)
    @Override
    public void recieveLeaderMessage(int port) throws RemoteException, NotBoundException {
        System.out.println("Leader message recieved");

        //if not this peer then connect to new leader server
        if (port != this.clientPortNumber) {
            System.out.println("Trying to connect to new server");
            connectNewServer(port);
            sendLeaderMessage(port);
        }
        
        //set voting flag back to false
        this.participant = false;
    }

    //connect new server method 
    public void connectNewServer(int port) throws RemoteException, NotBoundException {
        Registry peerRegistry = LocateRegistry.getRegistry(hostURL, port);
        this.serverInterface = (ServerInterface) peerRegistry.lookup("Server");
    }
}
