package cs2510.project3.system;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;

import cs2510.project3.common.SystemMessage;

// Listens for connection requests.  Places received messages in an
// inbox for later processing
public class ReceiverSocket implements Runnable {
    private int                 PORT_NO;
    private boolean             accepting = true;
    private LinkedList<SystemMessage<Object>> inbox;
    ServerSocket srvr;
    
    // keep track of receivers so we can end them later
    private LinkedList<Receiver> receivers = null;
    private LinkedList<Thread> receiver_threads = null;
    
    public ReceiverSocket(int port) {
        inbox = new LinkedList<SystemMessage<Object>>();
        receivers = new LinkedList<Receiver>();
        receiver_threads = new LinkedList<Thread>();
        PORT_NO = port;
    }

    //remove and stop a single connection
    public void removeConnection(String address) throws IOException{
      for(Receiver r : receivers){
        if(r.getAddress().equals(address))
          r.stop();
      }
    }
    
    // stop listening for new incomming connections
    // and stop all current incoming connections
    public void stop() throws IOException{
        if(receivers != null && receiver_threads != null){
            for(Receiver r : receivers)
              r.stop();
        }
        if(srvr != null)
          srvr.close();
        accepting = false;
        
    }
   
    // Listen for connections from clients
    public void run() {
        // Socket skt = null;

        try {
            // Listen on port port_no
            srvr = new ServerSocket(PORT_NO);

            while (accepting) {
              Socket skt = srvr.accept(); //FIXME: this will throw an exception when app ends
              // Delegate a new thread to handle receiving of messages on this
              // socket
              Receiver r = new Receiver(skt, skt.getInetAddress().toString());
              receivers.add(r);
              Thread t = new Thread(r);
              receiver_threads.add(t);
              t.start();
            }
        }
        catch (Exception e) {
          e.printStackTrace();
        }
    }

    // methods for interacting with the inbox externally
    public int inboxLength() {
      int length = -1;
      synchronized(inbox){
        if (inbox != null)
          length = inbox.size();        
      }
      return length;
    }

    public SystemMessage<Object> getMessage() {
      SystemMessage<Object> msg = null;
      synchronized(inbox){
        if (inbox != null && inbox.size() > 0)
          msg = inbox.poll();
      }
      return msg;
    }

    // A listener is created for each connection request that the server
    // receives
    private class Receiver implements Runnable {
        private Socket  clientSocket;
        private boolean listening = true;
        private String address;
        private ObjectInputStream in = null;

        public Receiver(Socket clientSocket, String address) {
            this.clientSocket = clientSocket;
            this.address = address;
        }

        public void stop() throws IOException{
          clientSocket.close();
          if(in != null){
            in.close(); 
          }
          listening = false;
        }

        @SuppressWarnings("unchecked")
        public void run() {
          SystemMessage<Object> msg;
            try {
              in = new ObjectInputStream(clientSocket.getInputStream());
              while (listening) {
                // Handle the incoming messages from the socket
                msg =  (SystemMessage<Object>) in.readObject();
                synchronized (inbox) {
                  inbox.add(msg);
                }
              }
            }
            catch(EOFException e){}
            catch(SocketException e){}
            catch (IOException e) {
              e.printStackTrace();
            }
            catch(ClassNotFoundException e){
              e.printStackTrace();
            }
        }//end run
        
        //setters and getters
        public String getAddress(){ return this.address; }
    }
}
