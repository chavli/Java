package cs2510.project2.system;

import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;


// Listens for connection requests.  Places received messages in an
// inbox for later processing
public class ReceiverSocket implements Runnable {
    private int                 PORT_NO;
    private boolean             accepting = true;
    private LinkedList<Message> inbox;
    
    // keep track of receivers so we can end them later
    private LinkedList<Receiver> receivers = null;

    public ReceiverSocket(int port) {
        inbox = new LinkedList<Message>();
        receivers = new LinkedList<Receiver>();
        PORT_NO = port;
    }

    // stop listening for new incomming connections
    // and stop all current incoming connections
    public void stop() {
        if(receivers != null)
            for(Receiver r : receivers)
                r.stop();
        accepting = false;
    }

    // Listen for connections from clients
    public void run() {
        ServerSocket srvr = null;
        // Socket skt = null;

        try {
            // Listen on port port_no
            srvr = new ServerSocket(PORT_NO);

            while (accepting) {
                Socket skt = srvr.accept();
                // Delegate a new thread to handle receiving of messages on this
                // socket
                Receiver r = new Receiver(skt);
                receivers.add(r);
                new Thread(r).start();
            }
        }
        catch (Exception e) {
        }
    }

    // methods for interacting with the inbox externally
    public synchronized int inboxLength() {
        if (inbox != null)
            return inbox.size();
        return -1;
    }

    public synchronized Message getMessage() {
        if (inbox != null && inbox.size() > 0)
            return new Message(inbox.remove());
        return null;
    }

    // A listener is created for each connection request that the server
    // receives
    private class Receiver implements Runnable {
        private Socket  clientSocket;
        private boolean listening = true;

        public Receiver(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        public void stop() {
            listening = false;
        }

        public void run() {
            ObjectInputStream in = null;
            Message msg;
            try {
                in = new ObjectInputStream(clientSocket.getInputStream());
                while (listening) {
                    // Handle the incoming messages from the socket
                    msg = (Message) in.readObject();
                    synchronized (inbox) {
                        inbox.add(msg);
                    }
                }
            }
            catch (Exception e) {
            }
        }
    }
}
