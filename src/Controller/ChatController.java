package Controller;

import View.StartChatGUI;
import RMIChatClient.Client;
import RMIChatClient.ClientInterface;
import RMIChatServer.Server;
import RMIChatServer.ServerInterface;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class representing the application's startup window functionality
 *
 * @author Devin Grant-Miles
 */
public class ChatController {

    //GUI for start window
    private StartChatGUI view;

    public ChatController() {
        this.view = new StartChatGUI();
        this.view.setVisible(true);

        //action listener for join button
        view.getJoinButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                joinSystemEventHandler();
            }
        }
        );

        //action listener for create new button
        view.getCreateNewButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    establishNewSystemEventHandler();
                } catch (NotBoundException ex) {
                    Logger.getLogger(ChatController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        );
    }

    //join system method
    public void joinSystemEventHandler() {
        try {
            //Get the new peers name and the host URL to connect to
            String name = view.getNameField().getText();
            String hostURL = view.getUrlField().getText();
            Registry serverRegistry = LocateRegistry.getRegistry(hostURL);

            //get the new peers port to start own registry for client stub
            int port = Integer.parseInt(view.getPortNumberField().getText());
            LocateRegistry.createRegistry(port);

            //get server interface from host registry and create new client
            ServerInterface serverInterface = (ServerInterface) serverRegistry.lookup("Server");
            Client client = new Client(name, serverInterface, port);

            //get own registry and and own client stub           
            Registry thisRegistry = LocateRegistry.getRegistry(port);
            ClientInterface clientStub = (ClientInterface) UnicastRemoteObject.exportObject(client, port);//alternative is for server to extend UnicastRemoteObject
            thisRegistry.rebind("Client", clientStub);

            //add client to peer to peer system
            client.addClient();

            view.dispose();
        } catch (RemoteException ex) {
            Logger.getLogger(ChatController.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Could not join");
        } catch (NotBoundException ex) {
            Logger.getLogger(ChatController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //establish new system method
    public void establishNewSystemEventHandler() throws NotBoundException {
        try {
            //First peer establishing system creates server object 
            Server server = new Server();

            //Get port number and create registry
            int port = Integer.parseInt(view.getPortNumberField().getText());
            LocateRegistry.createRegistry(port);

            //Create a server stub and export/bind to registry 
            ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(server, port);
            Registry thisRegistry = LocateRegistry.getRegistry(port);
            thisRegistry.rebind("Server", stub);//binds if not already
            System.out.println("I am the server.");

            //Get the serverInterface proxy from the registry and connect own client
            ServerInterface serverInterface = (ServerInterface) thisRegistry.lookup("Server");
            String name = view.getNameField().getText();
            Client client = new Client(name, serverInterface, port);

            //Create a stub for the client side and register in registry
            ClientInterface clientStub = (ClientInterface) UnicastRemoteObject.exportObject(client, port);//alternative is for server to extend UnicastRemoteObject
            thisRegistry.rebind("Client", clientStub);

            //add the client to the peer to peer system
            client.addClient();
            view.dispose();
        } catch (RemoteException ex) {
            Logger.getLogger(ChatController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
