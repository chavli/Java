package cs2510.project3.system;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;

import cs2510.project3.common.SystemMessage;

// Creates connection requests and loops over outbox queue,
// waiting to send messages to corresponding servers
public class SenderSocket {
    private LinkedList<SystemMessage<Object>> outbox;
    private String address;   //address of sender
    
    //keep track of every outgoing connection so we can end them later
    private ArrayList<Sender> senders = null;
    private ArrayList<Thread> sender_threads = null;

    public SenderSocket(String address) {
        this.outbox = new LinkedList<SystemMessage<Object>>();
        this.address = address;
        senders = new ArrayList<Sender>();
        sender_threads = new ArrayList<Thread>();
    }
    
    //add a new connection and start it
    public void addConnection(String address){
      connect(address);
    }
    
    //end and remove a connection
    public void removeConnection(String address) throws IOException{
      for(Sender s : senders)
        if(s.getAddress().equals(address))
          s.stop();
    }
    
    public boolean hasConnection(String address){
      boolean exists = false;
      for(Sender s : senders){
        if(s.getAddress().equals(address)){
          exists = true;
          break;
        }
      }
      return exists;
    }
    
    //establish a connection with the address
    public void connect(String address) {
      if(!hasConnection(address)){
        try {
	        Sender s = new Sender(new Socket(address, Utilities.PORT), address);
	        
	        //add sender to list of senders
	        senders.add(s);
	        Thread t = new Thread(s);
	        sender_threads.add(t);
	        t.start();

        } catch (Exception e) { e.printStackTrace(); }
      }
    }
    
    //stop all outgoing connections
    public void stop() throws IOException{
      while(outbox.size() > 0){}
      if(senders != null && sender_threads != null){
        for(Sender s : senders)
          s.stop();
      }
    }
    

    
    public synchronized void sendMessage(SystemMessage<Object> msg, String dest_addr){
        msg.setSourceAddr(this.address);
        msg.setDestAddr(dest_addr);
        outbox.add(msg);
    }
    
    private class Sender implements Runnable {
        // A listener is created for each connection request that the server receives
        private Socket server_socket;
        private String address;
        private boolean sending = true;
        private ObjectOutputStream out = null;
        
        public Sender(Socket server_socket, String address) {
            this.server_socket = server_socket;
            this.address = address;
        }   
        
        public void stop() throws IOException{
          if(out != null){ out.close(); }
          sending = false;
        }
        
        public void run() {
            SystemMessage<Object> msg = null;
            System.out.println("Connection achieved at " + address);

            try {
                out = new ObjectOutputStream(server_socket.getOutputStream());

                while (sending) {
                    synchronized (outbox) {
                        msg = outbox.peek();
                        if (msg != null && msg.getDestAddr().equals(address)) {
                            outbox.remove();
                            out.writeObject(msg); // Sends message to the server
                        }
                    }
                }
                out.close();
            } catch (Exception e) {
              e.printStackTrace();
            }
        }
        
        //setters and getters
        public String getAddress(){ return this.address; }
    }
}
