package cs2510.project2.global;

import java.util.LinkedList;
import java.util.Random;

import cs2510.project2.system.LookUpTable;
import cs2510.project2.system.Message;
import cs2510.project2.system.NameServer;
import cs2510.project2.system.RaymondNode;
import cs2510.project2.system.RaymondTree;
import cs2510.project2.system.Message.MessageType;
import cs2510.project2.system.TokenNode;

public class GlobalNameServer extends NameServer{
	//the global nameserver has every local nameserver as a direct child
	private static final int BRANCHES = Integer.MAX_VALUE;

    //global message processor
	private GlobalMessageProcessor gmp = null;
    
    //random number generator
    Random random;

    public GlobalNameServer(int port){
    	super(port);

        uid = "GNS_" + System.currentTimeMillis(); 
        setCacheSize(LookUpTable.NO_LIM);

	    System.out.println("Global NameServer Location: " + this.address);
	    
        //seed generator
        random = new Random(System.currentTimeMillis());
            
	    //create the tree used for raymond's token algorithm
	    tree = new RaymondTree(BRANCHES);
	    
        //add the global ns to the root of the raymond tree
        root = new RaymondNode(this.address);
        tree.addNode(root); 

        //start message listener and processor
	    start();
    }
    
    //starts the nameservers message listener and the message processor
    public void start(){
    	//start the message listener
    	super.start();
	
    	//create the message processor
    	gmp = new GlobalMessageProcessor();
    	
        //start the message processor
        new Thread(gmp).start();    
    }
    
    //stop the global message processor
    //call superclass to end all connections and nodes
    public void stop(){
        super.stop();
        if(gmp != null)
            gmp.stop();
    }
    

    //used by the cli
    @Override
    public void dumpTree() {
       System.out.println(tree.toString());
    }
    
    @Override
    public void startSystem(){
        START = System.currentTimeMillis() / 1000;
        Message outgoing;
        root = tree.getRoot();
        for(RaymondNode child : root.getChildren()){
            outgoing = new Message("", "", MessageType.SYS_START);
            System.out.println("starting " + child.getName());
            sender.sendMessage(outgoing, child.getName());
        }
    }

    @Override
    public String statsToCSV(){
        return "GNS";
    }


    //processes messages queued in the receiver socket's inbox
    private class GlobalMessageProcessor implements Runnable{
        private boolean processing = true;
        
        public GlobalMessageProcessor() {}
        
        public void stop(){
            processing = false;
        }
        
		public void run() {
    	    Message incoming, outgoing;
    	    MessageType type;
			String body, source_uid, source_addr, dest_addr ,result, backups;
            String[] parsed;
            TokenNode token;
            LinkedList<RaymondNode> children;

			while(processing){
				if(receiver != null){
                    if( (incoming = receiver.getMessage() ) != null){
						System.out.println("Global NameServer received a message\n" + incoming.toString() + "\n");

						type = incoming.getType();
                        source_uid = incoming.getSourceUid();
                        source_addr = incoming.getSourceAddr();
                        body = incoming.getBody();
                        
						//process message based on type
						switch(type){
							//new local nameserver joins system
							case SYS_NEW:	
								//create a connection with the new machine
								sender.addConnection(source_addr);
								
								//create a response message (body, response, type)
								outgoing = new Message(body, "Welcome to the system", MessageType.SYS_NEW_R);
							    
								//send a response to the new local ns
								sender.sendMessage(outgoing, source_addr);
                                
                                //notify all nameservers of new nameserver
                                children = tree.getRoot().getChildren();
                                for(RaymondNode child : children){
                                    outgoing = new Message(source_addr, "none", MessageType.SYS_NEW_INFORM);
                                    sender.sendMessage(outgoing, child.getName());
                                }

                                //add uid of new local nameserver to raymond tree
                                tree.addNode(new RaymondNode(source_addr));
								break;
                            
                            //a new node (user or resource) has joined the system
						    case SYS_JOIN:
                                //the body contains the uid of the new node
                                addToCache(body, source_addr);

                                //NOTE: if a token joined create a dummy token in the root that just stores the location of
                                //the actual token
                                //dummy token doesn't receive message, and doesn't become locked
                                if(body.indexOf("_T") >= 0)
                                	root.addToken(new TokenNode(body, source_addr, null));
                                
                                break;
							//local nameserver is asking for the location of a resource
							case SYS_LOOKUP:
								//body is of the form <requesting node>#<requested node>#<original message body>#<original message type>
								parsed = body.split("#");
                                result = readCache(parsed[1]);
                                
                                if(result == null) //FIXME
                                    outgoing = new Message(body, "", MessageType.SYS_LOOKUP_NULL);
                                else
                                    outgoing = new Message(body, result, MessageType.SYS_LOOKUP_R);

                                //send a response to the local ns
                                System.out.println("GNS sending message to source_addr: " + source_addr + "\n" + outgoing.toString());
                                sender.sendMessage(outgoing, source_addr);
							    break;
                            case SYS_ALLUSER:
                                //send ALL the user nodes to a specific node
                                result = getUserNodeEntries();
                                //result = getUserNodeUids();

                                if (result == null)
                                    outgoing = new Message(body, "", MessageType.NODE_ALLUSER_NULL);
                                else 
                                    outgoing = new Message(body, result, MessageType.NODE_ALLUSER_R);

                                outgoing.setDestUid(source_uid);
                                sender.sendMessage(outgoing, source_addr);
                                break;
                            case SYS_ALLRESOURCE:
                                //send ALL the resource nodes 
                                result = getResourceNodeUids();

                                if (result == null)
                                    outgoing = new Message(body, "", MessageType.NODE_ALLRESOURCE_NULL);
                                else
                                    outgoing = new Message(body, result, MessageType.NODE_ALLRESOURCE_R);

                                outgoing.setDestUid(source_uid);
                                sender.sendMessage(outgoing, source_addr);
                                break;
                            case SYS_INVALIDATE_COPY:
                                // Primary has been written to.  Invalidate backups
                                // body is of the form 101_R
                                // look for backups (of the form 101_B#_###)
                                backups = getBackupNodeUids(body);
                                
                                //NOTE: if we have one null check, backups still gets through even it it's null
                                //it make no sense
                                // System.out.println(">>>> " + (backups != null) + " " + backups);
                                if (backups != null) {
                                   // System.out.println("!!!!!!!!!!!!1 " + (backups != null) + " "  +  backups);
                                    if(backups != null){
                                        parsed = backups.split("#");
                                        outgoing = new Message(body, "", MessageType.SYS_REMOVE_COPY);
                                        for (int i = 0; i < parsed.length; i++) {
                                            System.out.println("Removing " + parsed[i] + " from my cache");
                                            sender.sendMessage(outgoing, readCache(parsed[i]));
                                            removeFromCache(parsed[i]);
                                        }
                                    }
                                }
                                break;
                            case SYS_INVALIDATE_NODE:
                                //body is ouid of dead object node
                                
                                //a resource is being invalidated because it died)
                                if(body.indexOf("_R") > -1){
                                    result = body.replaceAll("_R", "_T");
                                    dest_addr = readCache(body);
                                    System.out.println(">>> INVALIDATING RESOURCE cache remove: " + body + " " + result);
                                    removeFromCache(body);      //remove resource reference from global table
                                    removeFromCache(result);    //remove token reference from global table
                                    
                                    //tell local NS to kill token of missing resource
                                    //result is the tuid of token
                                    outgoing = new Message(result, "", MessageType.SYS_TOK_KILL);
                                    sender.sendMessage(outgoing, dest_addr);
                                    
                                    //invalidate all local caches
                                    children = tree.getRoot().getChildren();
                                    for(RaymondNode child : children){
                                        outgoing = new Message(body, "", MessageType.SYS_INVALIDATE_CACHE);
                                        sender.sendMessage(outgoing, child.getName());
                                    } 
                                    
                                    System.out.println(">>>  all nodes notified of invalidation");
                                        
                                    //result now holds uid of backup
                                    backups = getBackupNodeUids(body);

                                    if(backups != null){

                                        //backup to be promoted is always the first backup in this list
                                        result = backups.split("#")[0]; //result is the uid of a backup
                                        System.out.println("Backup Found! " + result);
                                        outgoing = new Message(result, "", MessageType.SYS_NEW_PRIMARY);
                                        sender.sendMessage(outgoing, readCache(result));
                                    }                                    
                                    else
                                        System.out.println("Resource: " + body + " is gone forever!! :(");
                                }
                                //a token is being invalidated (because it died)
                                else if(body.indexOf("_T") > -1){
                                    System.out.println(">>> INVALIDATING TOKEN: " + body);
                                    //which local nameserver did the token die on
                                    dest_addr = readCache(body);
                                    
                                    //remove reference from global lookup table
                                    removeFromCache(body);
                                    
                                    outgoing = new Message(body, "", MessageType.SYS_TOK_NEW);
                                    sender.sendMessage(outgoing ,dest_addr);

                                    System.out.println(">>> Regenerating Token: " + body + " on " + dest_addr);
                                }
                                else if (body.indexOf("_U") >= 0) {
                                    // User has died.  Recognized by some other user
                                }
                                else {
                                    System.out.println("Who has died?  Not a user, token, or resource");
                                }
                                break;
                            /*//Token ME messages
                            case TOK_REQ:
                                //randomly pick a dummy token to get the address of the real token
                                token = root.getToken( random.nextInt( root.getNumTokens() ) );
                                
                                //set body to uuid requesting token
                                outgoing = new Message(source_uid, "", MessageType.TOK_REQ);
                                outgoing.setDestUid(token.getUid());
                                sender.sendMessage(outgoing, token.getResourceUid());
                                break;
                            */
                            case TOK_REL:
                                //body contains the uid of the resource that is now available
                                token = root.getTokenByUid(body);
                                 
                                //forward the release to the actual token
                                outgoing = new Message("", "", MessageType.TOK_REL);
                                outgoing.setDestUid(token.getUid());
                                sender.sendMessage(outgoing, token.getResourceUid());
                                break;
                                
                            case SYS_RANDRESOURCE:
                                //send a random resource node
                                result = getRandResource();

                                if (result == null)
                                    outgoing = new Message(body, "", MessageType.NODE_RANDRESOURCE_NULL);
                                else
                                    outgoing = new Message(body, result, MessageType.NODE_RANDRESOURCE_R);

                                outgoing.setDestUid(source_uid);
                                sender.sendMessage(outgoing, source_addr);
                                break;

                            case SYS_RANDTOKEN:
                                //send a random token node
                                token = getRandToken();

                                //set body to uuid requesting token
                                outgoing = new Message(source_uid, "", MessageType.TOK_REQ);
                                outgoing.setDestUid(token.getUid());
                                sender.sendMessage(outgoing, token.getResourceUid());
                                break;

							case NO_TYPE:
							    break;
                            default:
                                System.out.println("Global NameServer unrecognized message!\n" );
                                break;
						}
					}
				}
			}
		}
    }
}
//this is a lot of closing brackts
