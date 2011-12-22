package cs2510.project2.system;

import cs2510.project2.system.Message;
import cs2510.project2.system.Message.MessageType;
import cs2510.project2.system.NameServer.AddressLookup;
import cs2510.project2.system.RaymondNode;

import java.util.Random;
import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.Random;

public class UserNode extends ObjectNode implements Runnable {
    private static final int TREE = 1;
    private static final int TOKENLESS = 2;

    // Common for both ME types
    private UserMessageProcessor ump;
    private int ME_TYPE;
    private int readThresh;
    private int writeThresh;

    // Tokenless stuff
    private CyclicBarrier barrierA, barrierB, barrierC, barrierD;
    private int clock;
    private String resource;
    private ArrayList<String> users;
    private ArrayList<String> queued;
    private ArrayList<String> yet_to_ack;
    private ArrayList<String> yet_to_pong;
    private int cs_requested;
    private boolean in_cs;
    private boolean been_ponged = false;
   
    //Random number generator for deciding actions
    private Random random;

    //Test stuff
    public int in_fault, waiting_fault, random_tree_fault;
    public long start_wait, end_wait;

    public UserNode(String uid, AddressLookup sender, int me_type, int read, int write, int in_cs, int waiting_cs, int random_tree) {
        super(uid, sender);
        this.type = ObjectType.USER; 
        this.ME_TYPE = me_type;
        this.readThresh = read;
        this.writeThresh = read + write;
        this.random = new Random(); 

        in_fault = in_cs;
        waiting_fault = waiting_cs;
        random_tree_fault = random_tree;

        //tokenless stuff
        this.barrierA = new CyclicBarrier(2);
        this.barrierB = new CyclicBarrier(2);
        this.barrierC = new CyclicBarrier(2);
        this.barrierD = new CyclicBarrier(2);
        this.clock = 0;
        this.resource = null;
        this.users = new ArrayList<String>();
        this.queued = new ArrayList<String>();
        this.yet_to_ack = new ArrayList<String>();
        this.yet_to_pong = new ArrayList<String>();
        this.cs_requested = -1;
        this.in_cs = false;
        
        //start the message listener
        ump = new UserMessageProcessor(this.ouid);
        new Thread(ump).start();
    }
    
    @Override
    public String toString() {
        return "User: " + this.ouid;
    }

    // Starts the message processor
    public void start() {
        new Thread(this).start();
    }
        
    public void stop(){
        NameServer.FT_USR_DEAD++;
        alive = false;
        if( ump != null )
            ump.stop();
    }
     
    public void run() {
        int move;

        while (alive) {
            // Sleep for 10 seconds before doing anything
            move = random.nextInt(100);

            /* Token-less run */
            requestResource();
            if (move > writeThresh) {
                try { Thread.sleep(1000 + random.nextInt(9000)); } catch (Exception e) {}
            }
            else if (move > readThresh) { // Write
                if (this.ME_TYPE == TREE) {
                    if (alive)
                        requestToken();
                }
                else {
                    if (alive)
                        requestUsers();
                    if (alive)
                        requestME();
                    if (alive)
                        writeResource();
                    if (alive)
                        releaseResource();
                }
            }
            else { // Read
                if (alive)
                    readResource();
            }
        }
    }

    private class UserMessageProcessor implements Runnable {
        private boolean processing;
        private String uid;

        public UserMessageProcessor(String uid) {
            this.processing = true;
            this.uid = uid;
        }

        public void stop() {
            processing = false;
        }

        public void run() {
            Message incoming, outgoing;
            MessageType type;
            String source_uid, dest_uid, body, attachment;
            int ts;

            int i = 0;
            while (processing) {
                if ((incoming = peekAndRemove()) != null) {
                    //System.out.println("Node received message\n" + incoming.toString());

                    type = incoming.getType();
                    source_uid = new String(incoming.getSourceUid());
                    body = incoming.getBody();
                    attachment = incoming.getAttachment();
                    ts = incoming.getTimestamp();
                    
                    //update logical clock
                    if (ts >= getClock())
                        setClock(ts + 1);
                        
                    switch (type) {
                        case TOK_ACK:
                            //body contains the uid of the resource this usernode has been
                            //given exclusive access to
                            System.out.println(this.uid + " given access to " + body);
 
                            setResource(body);
                            if (getBarrierB().getNumberWaiting() == 1)
                                triggerB();

                            break;
                        case RA_REQ:
                            if (inCS(body)) {
                                enqueueUser(source_uid);
                            }
                            else if (!wantCS(body)) {
                                setClock(getClock() + 1);
                                outgoing = new Message(body, "It's all yours", MessageType.RA_ACK, getClock());
                                sender.sendMessage(outgoing, this.uid, source_uid);
                            }
                            else if (getCsRequested() < ts) {
                                enqueueUser(source_uid);
                            }
                            else if (ts < getCsRequested()) {
                                setClock(getClock() + 1);
                                outgoing = new Message(body, "It's all yours", MessageType.RA_ACK, getClock());
                                sender.sendMessage(outgoing, this.uid, source_uid);
                            }
                            else if (this.uid.compareTo(source_uid) < 0) {
                                enqueueUser(source_uid);
                            }
                            else {
                                setClock(getClock() + 1);
                                outgoing = new Message(body, "It's all yours", MessageType.RA_ACK, getClock());
                                sender.sendMessage(outgoing, this.uid, source_uid);
                            }
                            break;
                        case RA_ACK:
                            gotAck(source_uid);
                            if (acksToGet() == 0) {
                                if (getBarrierB().getNumberWaiting() == 1)
                                    triggerB();
                            }
                            break;
                        case NODE_RANDRESOURCE_R:
                            setResource(attachment);
                            if (getBarrierA().getNumberWaiting() == 1)
                                triggerA();
                            break;
                        case NODE_RANDRESOURCE_NULL:
                            // Retry
                            try { Thread.sleep(1000); } catch (Exception e) {}
                            outgoing = new Message("", "", MessageType.SYS_RANDRESOURCE);
                            sender.sendMessage(outgoing, this.uid, "global");
                            break;
                        case NODE_ALLUSER_R:
                            setUsers(attachment);
                            if (getBarrierA().getNumberWaiting() == 1)
                                triggerA();
                            break;
                        case NODE_ALLUSER_NULL:
                            // Retry
                            try { Thread.sleep(1000); } catch (Exception e) {}
                            outgoing = new Message("", "", MessageType.SYS_ALLUSER);
                            sender.sendMessage(outgoing, this.uid, "global");
                            break;
                        case FILE_RD_R:
                            // Dump the file contents
                            System.out.println(this.uid + ": printing resource " + getResource() + " contents\n" + body);
                            break;
                        case PING:
                            // Pong them back
                            //System.out.println(incoming.toString());
                            setClock(getClock() + 1);
                            outgoing = new Message("I'm here", "", MessageType.PONG, getClock());
                            sender.sendMessage(outgoing, this.uid, source_uid);
                            break;
                        case PONG:
                            //System.out.println(incoming.toString());
                            // Token ponged
                            if (ME_TYPE == TREE) {
                                if (getBarrierC().getNumberWaiting() == 1)
                                    triggerC();
                            }
                            // User ponged
                            else {
                                gotPong(source_uid);
                                if (pongsToGet() == 0) {
                                    if (getBarrierC().getNumberWaiting() == 1)
                                        triggerC();
                                }
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }
    
    //some things the usernode is allowed to do
    /* ---token-based--- */
    private void requestToken(){
        Message request = new Message("", "", MessageType.SYS_RANDTOKEN); //get a random token
        System.out.println(this.ouid + " is requesting a token.");
        sender.sendMessage(request, this.ouid, AddressLookup.TO_GLOBAL);
        boolean got_cs;

        if (random.nextInt(100) < random_tree_fault) {
            NameServer.FT_USR_DEAD++;
            alive = false;
            return;
        }

        start_wait = System.currentTimeMillis();
        got_cs = false;
        while (!got_cs) {
            if (random.nextInt(100) < waiting_fault) {
                NameServer.FT_CS_WAIT++;
                alive = false;
                return;
            }
            got_cs = triggerB(); // wait for n - 1 acks
            this.barrierB.reset();

            if (!got_cs) {
                got_cs = triggerC(); // wait for pongs
                this.barrierC.reset(); 
            }
        }

        end_wait = System.currentTimeMillis();
        NameServer.TOT_WAIT += (end_wait - start_wait);
        NameServer.CS_REQS++;
        //Write to this.resource
        writeResource();

        //Release the token
        String dest_token = new String(this.resource);
        dest_token = dest_token.replaceAll("_R", "_T");

        request = new Message(dest_token, "", MessageType.TOK_REL);
        sender.sendMessage(request, this.ouid, AddressLookup.TO_GLOBAL);
    }

    // Use this barrier when there is no need for fault tolerance
    // i,e, waiting for requests from the global name-server
    protected void triggerA() {
        try {
            this.barrierA.await();
        } catch (InterruptedException ex) { ex.printStackTrace();
        } catch (BrokenBarrierException ex) { ex.printStackTrace(); 
        }
    }

    // Use this barrier when waiting for acks in token-less, or waiting for token in tree-based 
    protected boolean triggerB() {
        Message msg;
        String destination;
        try {
            this.barrierB.await(60L, TimeUnit.SECONDS);
        } catch (InterruptedException ex) { ex.printStackTrace();
        } catch (BrokenBarrierException ex) { ex.printStackTrace(); 
        } catch (TimeoutException ex) {
            msg = new Message("Are you there", "", MessageType.PING);
            // Ping the token holder
            if (this.ME_TYPE == TREE) {
                destination = new String(this.resource).replaceAll("_R", "_T");
                sender.sendMessage(msg, this.ouid, destination);
            }
            // Ping anyone who hasn't acked to you
            else {
                this.yet_to_pong = new ArrayList<String>(this.yet_to_ack);
                System.out.println("yet to ack: " + yet_to_ack.toString() + "\nyet to pong: " + yet_to_pong.toString());
                for (String user : this.yet_to_ack) {
                        sender.sendMessage(msg, this.ouid, user);
                }
            }
            return false; // you don't have the CS
        }
        
        return true; // you have the CS
    }

    // Use this barrier while waiting for pongs from users
    protected boolean triggerC() {
        Message msg;
        String dead_token;
        try {
            this.barrierC.await(60L, TimeUnit.SECONDS);
        } catch (InterruptedException ex) { ex.printStackTrace();
        } catch (BrokenBarrierException ex) { ex.printStackTrace(); 
        } catch (TimeoutException ex) {
            // Didn't receive pongs from appropriate people.  Inform GNS that they are dead
            if (this.ME_TYPE == TREE) {
                dead_token = new String(this.resource).replaceAll("_R", "_T");
                msg = new Message(dead_token, "", MessageType.SYS_INVALIDATE_NODE);
                sender.sendMessage(msg, this.ouid, AddressLookup.TO_GLOBAL);
            }
            else {
                for (String dead_to_me : this.yet_to_pong) {
                    msg = new Message(dead_to_me, this.ouid, MessageType.SYS_INVALIDATE_NODE);
                    sender.sendMessage(msg, this.ouid, AddressLookup.TO_GLOBAL);

                    System.out.println("Assuming someone is dead");
                    gotAck(dead_to_me);
                }

                if (acksToGet() == 0) {
                    // You get CS
                    return true;
                }
            }
        }

        this.been_ponged = true;
        return false; // They are still alive
    }

    // Sends a request to the global ns for a random resource
    private void requestResource() {
        setClock(this.clock + 1);
        Message msg = new Message("", "", MessageType.SYS_RANDRESOURCE, this.clock);
        sender.sendMessage(msg, this.ouid, AddressLookup.TO_GLOBAL);

        // Wait for response
        triggerA();
        this.barrierA.reset();
    }

    /* READ functions */
    // Reads the resource for some time, maybe dumps its contents
    private void readResource() {
        setClock(getClock() + 1);
        Message msg = new Message(this.ouid + " reading resource ", "", MessageType.FILE_RD, this.clock);
        sender.sendMessage(msg, this.ouid, this.resource);

        // Sleep for some time, and read again
        try {
            Thread.sleep(1000 + random.nextInt(9000));
            setClock(getClock() + 5);
        } catch (Exception e) {}
        
        setClock(getClock() + 1);
        msg = new Message(this.ouid + " reading resource ", "", MessageType.FILE_RD, this.clock);
        sender.sendMessage(msg, this.ouid, this.resource);
    }

    /* WRITE functions */
    // Sends a request to the global ns for all user nodes in the system
    private void requestUsers() {
        setClock(this.clock + 1);
        Message msg = new Message("", "", MessageType.SYS_ALLUSER, this.clock);
        sender.sendMessage(msg, this.ouid, "global");


        // Wait for response
        triggerA();
        this.barrierA.reset();
    }

    // Sends a request for ME to every user in the system
    private void requestME() {
        boolean got_cs;
        setClock(this.clock + 1);
        this.cs_requested = this.clock;
        Message msg = new Message(this.resource, "", MessageType.RA_REQ, this.clock);
    
        for (String user : this.users) {
            if (!this.ouid.equals(user)) {
                sender.sendMessage(msg, this.ouid, user);
                this.yet_to_ack.add(new String(user));
            }
        }

        start_wait = System.currentTimeMillis();
        if (this.users.size() > 1) {
            System.out.println("Waiting for CS");

            got_cs = false;
            while (!got_cs) {
                if (random.nextInt(100) < waiting_fault) {
                    NameServer.FT_CS_WAIT++;
                    alive = false;
                    return;
                }
                got_cs = triggerB(); // wait for n - 1 acks
                this.barrierB.reset();

                if (!got_cs) {
                    got_cs = triggerC(); // wait for pongs
                    this.barrierC.reset(); 
                }
            }
        }
        end_wait = System.currentTimeMillis();
        NameServer.TOT_WAIT += (end_wait - start_wait);
        NameServer.CS_REQS++;
        this.in_cs = true;
    }

    // Writes to a resource
    private void writeResource() {
        //write data to the resource
        Message msg;

        setClock(getClock() + 1);
        msg = new Message(this.ouid + " entering CS", "", MessageType.FILE_WR, this.clock);
        sender.sendMessage(msg, this.ouid, this.resource);
        
        // Sleep for 5 seconds, then finish using resource
        System.out.println("Node " + this.ouid + " in CS");

        if (random.nextInt(100) < in_fault) {
            NameServer.FT_USR_DEAD++;
            alive = false;
            return;
        }

        try {
            Thread.sleep(1000 + random.nextInt(9000));
            setClock(getClock() + 5);
        } catch (Exception e) {}
        
        //write final data to resource
        setClock(getClock() + 1);
        msg = new Message(this.ouid + " leaving CS", "", MessageType.FILE_WR, this.clock);
        sender.sendMessage(msg, this.ouid, this.resource);

        //write to GNS, telling it that primary copy has been written to
        setClock(getClock() + 1);
        msg = new Message(this.resource, "", MessageType.SYS_INVALIDATE_COPY, this.clock);
        sender.sendMessage(msg, this.ouid, AddressLookup.TO_GLOBAL);
    }

    // Sends an ACK to all nodes that it has queued
    private void releaseResource() {
        System.out.println("Leaving CS");

        this.in_cs = false;
        this.cs_requested = -1;

        setClock(this.clock + 1);
        Message msg = new Message(this.resource, "It's all yours", MessageType.RA_ACK, this.clock);
        
        for (String user : this.queued)
            sender.sendMessage(msg, this.ouid, user);

        resetMEVars();
    }

    // Reset ME var values
    private void resetMEVars() {
        this.resource = null;
        this.users.clear();
        this.queued.clear();
        this.yet_to_pong.clear();
        this.yet_to_ack.clear();
    }

    protected boolean wantCS(String rUid) {
        return ((this.cs_requested != -1) && (this.resource.equals(rUid)));
    }
    
    protected boolean inCS(String rUid) {
        return (this.in_cs && this.resource.equals(rUid));
    }

    //setters (used by message processor)
    protected synchronized void setClock(int val) { this.clock = val; }
    protected void setResource(String rUid) { this.resource = new String(rUid); }
    protected void enqueueUser(String user) { this.queued.add(new String(user)); }
    protected void setCsRequested(int val) { this.cs_requested = val; }
    protected void setUsers(String userList) { 
        String[] tokens = userList.split("#");

        for (int i = 0; i < tokens.length; ++i) {
            this.users.add(tokens[i]);
        }
    }
    protected void gotAck(String acker) {
        this.yet_to_ack.remove(acker);
    }
    protected void gotPong(String ponger) {
        this.yet_to_pong.remove(ponger);
    }

    // getters
    protected CyclicBarrier getBarrierA() { return this.barrierA; }
    protected CyclicBarrier getBarrierB() { return this.barrierB; }
    protected CyclicBarrier getBarrierC() { return this.barrierC; }
    protected CyclicBarrier getBarrierD() { return this.barrierD; }
    protected String getResource() { return this.resource; }
    protected int getClock() { return this.clock; }
    protected int getNumUsers() { return this.users.size(); }
    protected int acksToGet() { return this.yet_to_ack.size(); }
    protected int pongsToGet() { return this.yet_to_pong.size(); }
    protected int getCsRequested() { return this.cs_requested; }
}
