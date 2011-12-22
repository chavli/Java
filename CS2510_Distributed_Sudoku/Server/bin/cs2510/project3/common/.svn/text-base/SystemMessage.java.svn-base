/*
 * Server-Side
 */
package cs2510.project3.common;

import java.io.Serializable;

public class SystemMessage<K> implements Serializable {

    private static final long serialVersionUID = -2386442515397149813L;
    public static enum SystemMessageType {
      NO_TYPE,
      SYS_JOIN,     //new node has joined
      SYS_JOIN_R,   //response to a new node joining
      SYS_DIST,     //distributing a sector
      SYS_SECMAP,   //distributing sector map
      SYS_START,    //system is starting
      SYS_RESTART,  //restart system
      SYS_END,      //main server stopped      
      SYS_LEAVE,    //node left the system
      
      NODE_FETCH,   //fetch all the sector data of a node
      NODE_FETCH_R,   //response to NODE_FETCH
      NODE_STATE,   //state information about an other node
      NODE_SLEEP,   //put a node to sleep
      NODE_WAKE,    //wake a node up
      NODE_DONE,    //node finished solving its sectors
      NODE_STUCK,   //node is unable to make forward progress
      
      SUDOKU_PUT,   //a new number has been added to the board
      SUDOKU_ASK,   //asking for a sector
      SUDOKU_ASK_R  //sector response
    }

    // Uids and Addresses should only be used for message delivery
    // They should also be set every time a message is passed
    private SystemMessageType type;
    private String source_uid, source_addr, dest_uid, dest_addr;
    private String body;
    private K attachment;
    
    public SystemMessage(){
        this.type = SystemMessageType.NO_TYPE;
        this.source_uid = "";
        this.source_addr = "127.0.0.1";
        this.dest_uid = "";
        this.dest_addr = "127.0.0.1";
        this.body = "";
        this.attachment = null;
    }

    public SystemMessage(String body, SystemMessageType type){
        this.source_uid = "";
        this.source_addr = "127.0.0.1";
        this.dest_uid = "";
        this.dest_addr = "127.0.0.1";
        this.body = new String(body);
        this.type = type;
        this.attachment = null;
    }

    @Override
    public String toString(){
        String str = "[\n-- Message Header --\n" +
            "Type:\t\t" + this.type + "\n" +
            "Source UID:\t" + this.source_uid + "\n" +
            "Source Addr:\t" + this.source_addr + "\n" +
            "Dest UID:\t" + this.dest_uid + "\n" +
            "Dest Addr:\t" + this.dest_addr + "\n" +
            "-- Message Body --\n" + this.body + "\n";
        
        if(this.attachment != null)
          str += "-- Attachment --\n" + this.attachment.toString() + "\n";
        
        str += "\n]";
        return str;
    }

    //setters and getters
    public void setSourceUid(String uid) { this.source_uid = uid; }
    public void setSourceAddr(String addr) { this.source_addr = addr; }
    public void setDestUid(String uid) { this.dest_uid = uid; }
    public void setDestAddr(String addr) { this.dest_addr = addr; }
    public void setBody(String message) { this.body = message; }
    public void setAttachment(K attachment) { this.attachment = attachment; }
    public void setType(SystemMessageType type) { this.type = type; }

    public String getSourceUid() { return this.source_uid; }
    public String getSourceAddr() { return this.source_addr; }
    public String getDestUid() { return this.dest_uid; }
    public String getDestAddr() { return this.dest_addr; }
    public String getBody() { return this.body; }
    public K getAttachment() { return this.attachment; }
    public SystemMessageType getType() { return this.type; }
}
