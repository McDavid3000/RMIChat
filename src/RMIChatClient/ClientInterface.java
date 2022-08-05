package RMIChatClient;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Class representing the client interface extends remote
 *
 * @author Devin Grant-Miles
 */
public interface ClientInterface extends Remote {

    public void getMessages(String message) throws RemoteException;

    public void updateClientList() throws RemoteException;

    public String getClientName() throws RemoteException;

    public void recieveElectionMessage(String message) throws RemoteException;

    public void recieveLeaderMessage(int port) throws RemoteException, NotBoundException;
}
