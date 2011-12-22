package cs2510.project2.local;

import cs2510.project2.system.*;
import cs2510.project2.system.Message.MessageType;

import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;

import java.util.ArrayList;

public class LocalNameServer extends NameServer {
    //the branch factor for the raymond tree
    private static final int BRANCHES = 2;
    
    private LocalMessageProcessor lmp = null;
    private ArrayList<UserNode> all_users;
    private int total_users;

    // The constructor starts the receiver thread.  The sender can be started with a call
    // to connect() on the LocalNameServer object.
    public LocalNameServer(int num_users, int num_resources, String global_ns, int port, int read, int write, int fail1, int fail2, int fail3, int me_type, int cache_val) {
        super(port);
        uid = "LNS_" + System.currentTimeMillis();
        String node_uid;
        Message msg;
        total_users = num_users; 
    	System.out.println("Local NameServer Location: " + this.address + " UID: " + this.uid);
        this.global_ns = global_ns;
        
        //start message listener and processor
        start();

        //add global nameserver connection
        sender.addConnection(this.global_ns);

        //notify the global server this local server joined the system
        msg = new Message(this.address, "none", MessageType.SYS_NEW);
        sender.sendMessage(msg, global_ns);
        
        //create the Raymond Tree
        tree = new RaymondTree(BRANCHES);

        if (cache_val == 0)
            cache_enabled = false;
        else
            setCacheSize(cache_val);

        //create specified number of user nodes and tell global ns
        UserNode unode;
        all_users = new ArrayList<UserNode>();
        for(int i = 0; i < num_users; ++i){
            //create the name and the node for the user
            node_uid = new String(this.uid + "_U" + i);   
            unode = new UserNode(node_uid, lookup, me_type, read, write, fail1, fail2, fail3);
            all_users.add(unode);
               
            //add new node to lookup table and node list
            addToTable(node_uid, unode);
            
            //add the new UserNode to the raymond tree
            RaymondNode rays = new RaymondNode(node_uid);
            tree.addNode(rays);
            
            //tell the global ns a new user has joined
            msg = new Message(node_uid, "none", MessageType.SYS_JOIN);
            sender.sendMessage(msg, this.global_ns);
        }

        //create specified number of resource nodes and tell global ns
        ResourceNode rnode;
        TokenNode tnode;
        String ruid, tuid;
        root = tree.getRoot();
        for(int i = 0; i < num_resources; ++i){
            //create the name and node for the resource
            ruid = new String(this.uid + "_R" + i);   
            rnode = new ResourceNode(ruid, lookup);
            new Thread(rnode).start();

            //add resource node to lookup table and node list
            addToTable(ruid, rnode);

            //tell the global ns a new resource is available
            msg = new Message(ruid, "", MessageType.SYS_JOIN);
            sender.sendMessage(msg, this.global_ns);
            
            //create a token protecting the resource, and repeat the same process as above
            tuid = new String(this.uid + "_T" + i);
            tnode = new TokenNode(tuid, ruid, lookup);
            new Thread(tnode).start();
            root.addToken(tnode);
            addToTable(tuid, tnode);
            msg = new Message(tuid, "", MessageType.SYS_JOIN);
            sender.sendMessage(msg, this.global_ns);
        }
    }

    // Starts the message listener and the message processor
    public void start(){
    	//start the message listener
    	super.start();
    	
    	lmp = new LocalMessageProcessor();
    	
        //start the message processor
        new Thread(lmp).start();    
    }
    
    //stop the message processor and all nodes
    public void stop(){
        //TODO: tell global nameserver this is going down
        super.stop();
        if(lmp != null)
            lmp.stop();
    }
    
    @Override
    public void dumpTree() {
       System.out.println(tree.toString());
    }
    
    public void startSystem(){
        System.out.println("I'm not the Global Name Server");
    }
    
    @Override
    //CSV is 
    //number of users, messages sent, average cs wait, resource deaths, general user deaths, user deaths in cs, user deaths waiting, cache misses
    public String statsToCSV(){
        return total_users + "," + MSG_SENT + "," + (1.0 * TOT_WAIT / CS_REQS) + "," + FT_RES_DEAD + "," + FT_USR_DEAD + "," + FT_CS_HOLD + "," + FT_CS_WAIT + "," + CACHE_MISS;
    }


    // Processes messages queued in the receiver socket's inbox
    private class LocalMessageProcessor implements Runnable{
        private boolean processing = true;
        public LocalMessageProcessor() {}
        
        public void stop(){
            processing = false;
        }
        
		public void run() {
    	    Message incoming, outgoing;
    	    MessageType type;
    	    String body, attachment, source_addr, source_uid, dest_uid, temp, result;
            TokenNode token;
    	    String parsed[], parsed2[];
    	    ObjectNode node;
            ResourceNode rnode;

            File file;
            FileReader fr;
            char []buf;

    	    while(processing){
    	        if(receiver != null){
    	            incoming = receiver.getMessage();
    	            if(incoming != null){
                        //System.out.println("Local NS received message\n" + incoming.toString());

                        body = incoming.getBody();
                        attachment = incoming.getAttachment();
                        dest_uid = incoming.getDestUid();
                        source_uid = incoming.getSourceUid();
                        source_addr = incoming.getSourceAddr();
    	                type = incoming.getType();
          			
    	                //process message based on type
    	                switch(type){
                            case SYS_START:
                                START = System.currentTimeMillis() / 1000;
                                System.out.println("Starting all user nodes");
                                for(UserNode unode : all_users)
                                    unode.start();
                                break;
    	                    case SYS_NEW:
    	                        //data is connection request from a local n-s
                                sender.addConnection(source_addr);
                                break;
    	                    case SYS_NEW_R:
    	                        break;
                            case SYS_NEW_INFORM:
                                //new name-server to connect to
                                sender.addConnection(body);
                                outgoing = new Message("Connection request", "none", MessageType.SYS_NEW);
                                sender.sendMessage(outgoing, body); 
                                break;
                            case SYS_LOOKUP_R:
                                //response from lookup request
                                //body is of the form <requesting node>#<requested node>#<original message body>#<original message type>#<original timestamp>
                                parsed = incoming.getBody().split("#");

                                //add result to cache
                                addToCache(parsed[1], attachment);
 
                                //now we can re-send the the node's original message to the appropriate destination
                                outgoing = new Message(parsed[2], "none", MessageType.valueOf(parsed[3]), Integer.parseInt(parsed[4]));
                                outgoing.setSourceUid(parsed[0]);
                                outgoing.setDestUid(parsed[1]);

                                sender.sendMessage(outgoing, attachment);
                                break;
                            case SYS_LOOKUP_NULL:
                                //response from lookup request
                                //notify node that the requested node doesn't exist
                                parsed = incoming.getBody().split("#");
                                temp = parsed[0];
                                node = readTable(temp);
                                node.enqueueMessage(new Message(incoming));
                                break;
                            
                            //Resource messages
                            //received a read response, create a local copy of the file
                            case SYS_REMOVE_COPY:
                                result = ((new String(body)).replaceAll("_R", "_B") + "_" + uid);

                                System.out.println("LocalNS attempting to remove backup " + result);
                                node = readTable(result);
                                if(node != null){
                                    node.stop();
                                    removeFromTable(result);
                                }
                                break;
                            //re-create a token (token died, but it's resource still exists)
                            case SYS_TOK_NEW:
                                //body contains the tuid of the token, since the name is the same as 
                                //the token that died, no references have to be updated
                                 
                                token = new TokenNode(body, body.replaceAll("_T", "_R"), lookup);
                                new Thread(token).start();
                                outgoing = new Message(body, "", MessageType.SYS_JOIN);
                                sender.sendMessage(outgoing, global_ns);

                                break;
                            //remove a token if the resource it protects is gone
                            case SYS_TOK_KILL:
                                //body is the token uid
                                token = (TokenNode)readTable(body);
                                if(token != null){
                                    token.stop();
                                    removeFromTable(body);

                                    //remove the actual token
                                    root = tree.getRoot();
                                    root.removeToken(body.replaceAll("_T", "_R"));
                                        
                                    System.out.println(">>> Token Removed: " + body);
                                }
                                break;
                            //remove invalid entries from the cache
                            case SYS_INVALIDATE_CACHE:
                                if(body.indexOf("_R") > -1)
                                    removeFromCache(body.replaceAll("_R", "_T"));
                                 removeFromCache(body);
                                break;
                            //promote a backup to original file status
                            case SYS_NEW_PRIMARY:
                                //result will hold the data in the backup
                                //body is the uid of the backup 101_B0_102
                                try{
                                    System.out.println(">>> Promoting: " + body);
                                    node = readTable(body);
                                    removeFromTable(body);
                                    file = new File(body + ".cha");
                                    fr = new FileReader(file);
                                    buf = new char[(int)file.length()];
                                    fr.read(buf);
                                    result = new String(buf);
                                    fr.close();
            
                                    System.out.println(">>> killing old resource node.");

                                    //stop the resource node of the backup
                                    //+ delete backup file
                                    node.stop();
                                    
                                    //create a new resource node
                                    body = body.replaceAll("_B", "_R");             //101_R0_102
                                    body = body.substring(0, body.lastIndexOf("_"));//101_R0

                                    //create new resource node and add it to local ns
                                    rnode = new ResourceNode(body, lookup, result);
                                    new Thread(rnode).start();
                                    addToTable(body, rnode);

                                    System.out.println(">>> created new resource node.");
                                    //tell global ns about new file
                                    outgoing = new Message(body, "", MessageType.SYS_JOIN);
                                    sender.sendMessage(outgoing, global_ns);
                                    
                                    //create new token
                                    //result stores the token uid
                                    result = body.replaceAll("_R", "_T");
                                    token = new TokenNode(result, body, lookup);
                                    new Thread(token).start();
                                    
                                    System.out.println(">>> created new token");
                                    //add token to list of tokens
                                    root = tree.getRoot();
                                    root.addToken(token);

                                    addToTable(result, token);
                                     
                                    //tell global ns about new token
                                    outgoing = new Message(result, "", MessageType.SYS_JOIN);
                                    sender.sendMessage(outgoing, global_ns);

                                }catch(IOException ex){
                                    ex.printStackTrace();
                                }

                                break;

                            case FILE_RD_R:
                                source_uid = new String(source_uid);   
                                source_uid = source_uid.replaceAll("_R", "_B");

                                //create resource node
                                result = new String(source_uid + "_" + uid);
                                rnode = new ResourceNode(result, lookup, body);
                                new Thread(rnode).start();
                                
                                //add resource node to lookup table and node list
                                addToTable(result, rnode);

                                //notify global ns of backup
                                outgoing = new Message(result, "", MessageType.SYS_JOIN);
                                sender.sendMessage(outgoing, global_ns);

                                //forward the request to the requestor
                                node = readTable(dest_uid);
                                node.enqueueMessage(new Message(incoming));
                                break;

                            case NO_TYPE:
                                break;
                            case NODE_ALLUSER_R:
                                // Cache results, then deliver to node
                                parsed = attachment.split("#");
                                result = "";

                                // parsed consists of <user_uid>,<user_address> pairs
                                for (int i = 0; i < parsed.length; ++i) {
                                    parsed2 = parsed[i].split(",");

                                    if (!address.equals(parsed2[1])) {
                                        addToCache(parsed2[0], parsed2[1]);
                                    }
                                    result += (parsed2[0] + "#");
                                }
                                //System.out.println("Original attachment: " + attachment + "\nDelivering: " + result);

                                incoming.setAttachment(result);
                                node = readTable(dest_uid);
                                if (node != null)
                                    node.enqueueMessage(new Message(incoming));
                                break;
                                
                            default:
                                // Theoretically, all messages targeted to a specific node can be simply delivered to that node's
                                // inbox
                                node = readTable(dest_uid);
                                if(node != null)//FIXME
                                    node.enqueueMessage(new Message(incoming));
                                break;
    	                }
    	            }
    	        }
    	    }//end of loop
		}
    }
}
