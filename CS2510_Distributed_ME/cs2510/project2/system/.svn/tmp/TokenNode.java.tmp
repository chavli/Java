package cs2510.project2.system;

import java.util.LinkedList;

import java.io.File;

import cs2510.project2.system.Message.MessageType;
import cs2510.project2.system.NameServer.AddressLookup;


public class TokenNode extends ObjectNode{
    private final long TIMEOUT = 20000; //ms

    //uid of the resource this token protects
    private String ruid;	
    
    //if this token is being used to access the resource
    private boolean locked;
    
    //list of usernode uids waiting for tihs token
    private LinkedList<String> pending;

    public TokenNode(String tuid, String ruid, AddressLookup sender){
    	super(tuid, sender);
        type = ObjectType.TOKEN;
        this.ruid = ruid;
        locked = false;
        pending = new LinkedList<String>();
    }
    
    //lock or unlock the token
    public void lock(){ locked = true; }
    public void unlock(){ locked = false; }
    
    public String toString(){
        String str = "{ "+ ruid + " : " + locked +" } waiting: [";
        for(String uid : pending)
            str += uid + ", ";
        str += "\n]";
        return str;
    }
    
    //check if the resource node this token node protects still exists
    public boolean resourceExists(){
        File file = new File(this.ruid + ".cha");
        return file.exists();
    }
    
    private long notifyNext(){
		Message incoming, outgoing;
		MessageType type;
		String body, source_uid;
        long last_ack = Long.MAX_VALUE;


        if(pending.size() > 0){
            if(resourceExists()){
                source_uid = pending.poll();
                outgoing = new Message(ruid, "", MessageType.TOK_ACK);
                sender.sendMessage(outgoing, ouid, source_uid);
                last_ack = System.currentTimeMillis();
                return System.currentTimeMillis();
            }

            else{
                outgoing = new Message(this.ruid, "", MessageType.SYS_INVALIDATE_NODE);
                sender.sendMessage(outgoing, this.ouid, AddressLookup.TO_GLOBAL);
                return last_ack;
            }
        }
        else{
            locked = false;
            return last_ack;
        }       
    }

	@Override
	//Tokens don't do anything but listen for messages so they don't need a seperate 
	//message processor
	public void run() {
		Message incoming, outgoing;
		MessageType type;
		String body, source_uid;
        long last_ack = Long.MAX_VALUE;	
		//listen for token requests and releases
		while(alive){
            //if token hasn't received a release within 20secs, assume usernode is dead
            if((System.currentTimeMillis() - last_ack) >= TIMEOUT){
                NameServer.FT_CS_HOLD++;    //same as FT_TOK_DEAD
                System.out.println("TOKEN ASSUMING TOKEN HOLDING USER IS DEAD");
                last_ack = notifyNext();
            }

			if ((incoming = peekAndRemove()) != null) {
				type = incoming.getType();
				body = incoming.getBody();
				source_uid = incoming.getSourceUid();

				switch(type){
					case TOK_REQ:
						//body contains the uuid of the user that wants the token
						if(locked){
							pending.add(body);
						}
						else{
							//lock the token and
							//acknowledge the request, reply with the resource uid
                            
                            //only ack the request if the resource still exists (it might've died)
                            if(resourceExists()){
							    locked = true;
							    outgoing = new Message(ruid, "", MessageType.TOK_ACK);
							    sender.sendMessage(outgoing, ouid, body);
                                last_ack = System.currentTimeMillis();
                            }
                            //notify the resource is missing, and look for backups
                            else{
                                outgoing = new Message(this.ruid, "", MessageType.SYS_INVALIDATE_NODE);
                                sender.sendMessage(outgoing, this.ouid, AddressLookup.TO_GLOBAL);
                            }
						}
						break;
					case TOK_REL:
						//acknowledge the next node in the pending queue
						if(pending.size() > 0){
                            if(resourceExists()){
							    source_uid = pending.poll();
							    outgoing = new Message(ruid, "", MessageType.TOK_ACK);
							    sender.sendMessage(outgoing, ouid, source_uid);
                                last_ack = System.currentTimeMillis();
                            }

                            else{
                                outgoing = new Message(this.ruid, "", MessageType.SYS_INVALIDATE_NODE);
                                sender.sendMessage(outgoing, this.ouid, AddressLookup.TO_GLOBAL);
                            }
						}
						else{
							locked = false;
						}
						break;
                    case PING:
                        System.out.println("TOKEN NODE RECEIVED A PING");
                        if(pending.indexOf(source_uid) == -1)
                            pending.add(source_uid);
                        outgoing = new Message(this.ruid, "", MessageType.PONG);
                        sender.sendMessage(outgoing, this.ouid, source_uid);
                        break;
                    default:
                        System.out.println(this.ouid + " unrecognized message!\n" + incoming.toString() );
                        break;
				}
			}
		}
		
	}
	@Override
	public void stop() {
		alive = false;
	}
	
    //setters and getters
    public void addPending(String uuid) { pending.add(uuid); }
    public void setResourceUid(String uid){ ruid = uid; }

    public boolean isLocked(){ return locked; }
    public String getPending(){ return pending.poll(); }
    public int getNumPending(){ return pending.size(); }
    public String getResourceUid(){ return ruid; }
}


