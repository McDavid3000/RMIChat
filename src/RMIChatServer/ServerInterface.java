/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RMIChatServer;

import RMIChatClient.ClientInterface;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * Class representing the server interface extends remote
 *
 * @author Devin Grant-Miles
 */
public interface ServerInterface extends Remote {

    public void addChatClient(int port, String url) throws RemoteException;

    public void removeChatClient(String clientName) throws RemoteException;

    public void sendMessages(String message) throws RemoteException;

    public ArrayList<ClientInterface> getClientList() throws RemoteException;

    public void setClientList(ArrayList<ClientInterface> clientList) throws RemoteException;
}
