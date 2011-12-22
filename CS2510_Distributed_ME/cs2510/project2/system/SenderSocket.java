package cs2510.project2.system;

import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;

// Creates connection requests and loops over outbox queue,
// waiting to send messages to corresponding servers
public class SenderSocket {
    private int PORT;
    
    private LinkedList<Message> outbox;
    private String address;
    
    //keep track of every outgoing connection so we can end them later
    private ArrayList<Sender> senders = null;

    public SenderSocket(String address, int port) {
        this.outbox = new LinkedList<Message>();
        this.address = address;
        senders = new ArrayList<Sender>();
        this.PORT = port;
    }
    
    //add a new connection and start it
    public void addConnection(String address){
    	connect(address);
    }
    
    public void connect(String address) {
        // Create a connection with a server
        InetAddress machine_name = null;

        try {
        	//start a connection with the machine at address
	        machine_name = InetAddress.getByName(address);
	        Sender s = new Sender(new Socket(machine_name, PORT), address);
	        
	        //add sender to list of senders
	        senders.add(s);
	        
	        new Thread(s).start();

        } catch (Exception e) { System.err.println(e); }
    }
    
    //stop all outgoing connections
    public void stop(){
        if(senders != null)
            for(Sender s : senders)
                s.stop();
    }
    
    public synchronized void sendMessage(Message msg, String dest_addr){
        msg.setSourceAddr(this.address);
        msg.setDestAddr(dest_addr);
        outbox.add(new Message(msg));
    }
    
    private class Sender implements Runnable {
        // A listener is created for each connection request that the server receives
        private Socket server_socket;
        private String destination_machine;
        private boolean sending = true;
        
        public Sender(Socket server_socket, String destination_machine) {
            this.server_socket = server_socket;
            this.destination_machine = destination_machine;
        }   
        
        public void stop(){
            sending = false;
        }
        
        public void run() {
            Message msg = null;
            ObjectOutputStream out = null;
            System.out.println("Connection achieved at " + destination_machine);

            try {
                out = new ObjectOutputStream(server_socket.getOutputStream());

                while (sending) {
                    synchronized (outbox) {
                        msg = outbox.peek();
                        if (msg != null && msg.getDestAddr().equals(destination_machine)) {
                            outbox.remove();
                            out.writeObject(msg); // Sends message to the server
                            NameServer.MSG_SENT++;
                        }
                    }
                    Thread.sleep(500);
                }
            } catch (Exception e) {}
        }
    }
}
