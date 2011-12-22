package cs2510.project2.system;

import java.util.LinkedList;

import cs2510.project2.system.NameServer.AddressLookup;

public abstract class ObjectNode implements Runnable {
    protected LinkedList<Message> messages;
    protected ObjectType type;
    protected String ouid;                  //object unique id
    protected boolean alive;                //node is alive
    protected AddressLookup sender;         //used for node to X communication


    public static enum ObjectType{
        USER,
        RESOURCE,
        TOKEN
    }
  
    public ObjectNode(String uid, AddressLookup sender) {
        this.messages = new LinkedList<Message>();
        this.ouid = uid;
        this.alive = true;
        this.sender = sender;
    }
    
    //message queue methods
    public synchronized void enqueueMessage(Message msg){
        messages.add(msg);
    }
    
    protected Message peekAndRemove() {
        Message top;
        synchronized (messages) {
            top = messages.poll();
        }
        return top;
    }
    
    //need to be defined by child classes
    public abstract void run();
    public abstract void stop();
    public abstract String toString();

    //setters and getters
    public void setUid(String uid){ this.ouid = uid; }
    public String getUid(){ return this.ouid; }
}
