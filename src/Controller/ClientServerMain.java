package Controller;

import java.rmi.RemoteException;

/**
 * Main method for chat program
 *
 * @author Devin Grant-Miles
 */
public class ClientServerMain {

    public static void main(String[] args) throws RemoteException {
        ChatController controller = new ChatController();
    }
}
