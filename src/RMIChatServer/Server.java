/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RMIChatServer;

import RMIChatClient.ClientInterface;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class representing the server for the chat program Each peer will have a
 * server
 *
 * @author Devin Grant-Miles
 */
public class Server implements ServerInterface {

    private ArrayList<ClientInterface> serverClientList;

    public Server() throws RemoteException {
        serverClientList = new ArrayList();
    }

    @Override
    public ArrayList<ClientInterface> getClientList() {
        return serverClientList;
    }

    @Override
    public void setClientList(ArrayList<ClientInterface> clientList) {
        this.serverClientList = clientList;
    }

    //method to add a new chat client
    //adds new client stub to arraylist of clients and updates all existing clients
    @Override
    public void addChatClient(int port, String url) throws RemoteException, AccessException {
        Registry clientRegistry = LocateRegistry.getRegistry(url, port);

        try {
            ClientInterface client = (ClientInterface) clientRegistry.lookup("Client");
            this.serverClientList.add(client);
        } catch (NotBoundException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }

        //update all existing clients in the system
        int i = 0;
        while (i < serverClientList.size()) {
            serverClientList.get(i).updateClientList();
            i++;
        }
    }

    //method for removing a chat client
    @Override
    public void removeChatClient(String clientName) throws RemoteException {

        //removes the inputted client from server client list 
        int j = 0;
        while (j < this.serverClientList.size()) {
            if ((this.serverClientList.get(j).getClientName().compareTo(clientName)) == 0) {
                this.serverClientList.remove(this.serverClientList.get(j));
            }
            j++;
        }

        //updates remaining clients' lists 
        int i = 0;
        while (i < serverClientList.size()) {
            serverClientList.get(i).updateClientList();
            i++;
        }
    }

    //send messages to each client by accessing each clients remote getMessages method
    @Override
    public void sendMessages(String message) throws RemoteException {
        int i = 0;
        while (i < serverClientList.size()) {
            serverClientList.get(i).getMessages(message);
            i++;
        }
    }
}
