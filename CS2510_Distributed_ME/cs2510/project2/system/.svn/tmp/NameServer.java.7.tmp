package cs2510.project2.system;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Iterator;
import java.util.ArrayList;
import java.net.InetAddress;
import cs2510.project2.system.Message.MessageType;
import cs2510.project2.system.LookUpTable;

public abstract class NameServer {
    //used for statistics and measurements
    public static long  START       =   0;  //start time in seconds
    public static int   MSG_SENT    =   0;  //total messages sent by the ns
    public static long  TOT_WAIT    =   0;  //total time spent waiting for the cs by all users
    public static int   CS_REQS     =   0;  //number of cs requests
    public static int   CACHE_MISS  =   0;  //total number of cache misses
    
    public static int   FT_CS_HOLD  =   0;  //number of users that died in the CS (i.e. FT_TOK_DEAD) 
    public static int   FT_CS_WAIT  =   0;  //number of users that died waiting for CS
    public static int   FT_USR_DEAD =   0;  //number of users that died
    public static int   FT_RES_DEAD =   0;  //number of resouces that died
    
    public String statsToString(){
        return "------ Nameserver stats ------\n" +
            "UID:\t\t\t" + this.uid + "\n" +
            "Uptime(s):\t\t" + ((START == 0) ? 0 : ((System.currentTimeMillis() / 1000) - START)) + "\n" +
            "Messages Sent:\t\t" + MSG_SENT + "\n" +
            "Cache Size:\t\t" + global_cache.getLength() + "\n" + 
            "Cache Misses:\t\t" + CACHE_MISS + "\n" + 
            "Avg CS wait:\t\t" + ((CS_REQS == 0) ? 0 : (1.0 * TOT_WAIT / CS_REQS)) + "\n" +
            "Resource Deaths:\t" + FT_RES_DEAD + "\n" +
            "General User Deaths:\t" + FT_USR_DEAD + "\n" +
            "User Deaths in CS:\t" + FT_CS_HOLD + "\n" +
            "User Deaths waiting CS:\t" + FT_CS_WAIT + "\n";
    }

    public abstract String statsToCSV();
        
    //end statistics 
    
    private static final int CACHE_SIZE = 20;
    
    protected LookUpTable<String, ObjectNode> lookup_table;
    protected LookUpTable<String, String> global_cache;	//NOTE: the global nameserver uses this as the lookup table
     
    protected ReceiverSocket receiver;
    protected SenderSocket sender;
    protected AddressLookup lookup;
    protected String address;
    protected String uid;
    protected String global_ns;
     
    //token based me stuff
    protected RaymondTree tree;
    protected RaymondNode root;
    
    protected boolean cache_enabled = true;

    //default constructor
    public NameServer(int port) {
        lookup_table = new LookUpTable<String, ObjectNode>("local");
        global_cache = new LookUpTable<String, String>("global", CACHE_SIZE);

        try {
            address = new String(InetAddress.getLocalHost().getHostName());
        } catch (Exception e) {}

        receiver = new ReceiverSocket(port);
        sender = new SenderSocket(this.address, port);
        lookup = new AddressLookup();
    }
    
    public void start() {
        new Thread(receiver).start();
    }
    
    //stop incoming and outgoing connections
    //end all nodes
    public void stop(){
        receiver.stop();
        sender.stop();
        
        for(ObjectNode n : lookup_table.values())
            n.stop();
    }
    
    protected void addToTable(String uid, ObjectNode node) {
        synchronized (lookup_table) {
            lookup_table.put(new String(uid), node);
        }
    }

    protected void removeFromTable(String uid) {
        synchronized (lookup_table) {
            lookup_table.remove(uid);
        }
    } 

    protected ObjectNode readTable(String uid) {
        ObjectNode node;
        synchronized (lookup_table) {
            if (lookup_table.containsKey(uid))
                node = lookup_table.get(uid);
            else
                node = null;
        }
        return node;
    }

    protected void addToCache(String uid, String machine) {
        synchronized (global_cache) {
            global_cache.put(new String(uid), new String(machine));
        }
    }

    protected void removeFromCache(String uid) {
        synchronized (global_cache) {
            global_cache.remove(uid);
        }
    } 

    protected String readCache(String uid) {
        String machine;
        synchronized (global_cache) {
            if (global_cache.containsKey(uid))
                machine = new String(global_cache.get(uid));
            else
                machine = null;
        }
        return machine;
    } 
    
    protected void setCacheSize(int length){
        synchronized (global_cache){
            global_cache.setLength(length);
        }
    }

    protected String getNodeUids() {
        String result = "";
        synchronized (global_cache) {
            for (String uid : global_cache.keySet()) {
                result += (uid + "#");
            }
        }
        if (result.length() == 0)
            result = null;
        return result;
    }

    protected String getUserNodeUids() {
        String result = "";
        synchronized (global_cache) {
            for (String uid : global_cache.keySet()) {
                if (uid.charAt(uid.length() - 2) == 'U')
                    result += (uid + "#");
            }
        }
        if (result.length() == 0)
            result = null;
        return result;
    }

    // returns <user_uid>#<user_address> pairs in an arraylist
    protected String getUserNodeEntries() {
        String pairs = "";
        Set<Map.Entry<String, String>> entries;
        HashMap<String, String> copy;

        synchronized (global_cache) {
            copy = new HashMap<String, String>(global_cache);
        }

        entries = copy.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            if (entry.getKey().indexOf("_U") >= 0)
                pairs += (entry.getKey() + "," + entry.getValue() + "#");
        }

        if (pairs.length() == 0)
            return null;
        else
            return pairs;
    }

    protected String getResourceNodeUids() {
        String result = "";
        synchronized (global_cache) {
            for (String uid : global_cache.keySet()) {
                if (uid.charAt(uid.length() - 2) == 'R')
                    result += (uid + "#");
            }
        }
        if (result.length() == 0)
            result = null;
        return result;
    }

    // Get any backup nodes for a given uid
    protected String getBackupNodeUids(String primary) {
        String to_find = (new String(primary)).replaceAll("_R", "_B");
        String backup_uids = "";

        synchronized (global_cache) {
            for (String uid : global_cache.keySet()) {
                if (uid.indexOf(to_find) >= 0)
                    backup_uids += (uid + "#");
            }
        }
        if (backup_uids.length() == 0)
            return null;
        return backup_uids;
    }

    // Returns a random resource
    protected String getRandResource() {
        Set<String> copy;
        String[] ar;
        String current;
        Random random;
        Iterator<String> iterator;

        // Have to create a copy so as to not modify the cache
        synchronized (global_cache) {
            copy = new HashSet<String>(global_cache.keySet());
        }

        for (iterator = copy.iterator(); iterator.hasNext();) {
            // Remove elements that aren't resources
            current = iterator.next();
            if (current.charAt(current.length() - 2) != 'R')
                iterator.remove();
        }

        if (copy.isEmpty())
            return null;
        else {
            ar = copy.toArray(new String[copy.size()]);
            if (ar.length == 1)
                return ar[0];
            else {
                random = new Random();
                return ar[random.nextInt(ar.length - 1)];
            }
        }   
    }

    protected TokenNode getRandToken() {
        Random random = new Random();
        TokenNode token = root.getToken( random.nextInt( root.getNumTokens() ) );
        return token;
    }

    //methods used by the CLI
    public void dumpTables(){
        synchronized(lookup_table){
            System.out.println(lookup_table.toString());
        }
        synchronized(global_cache){
            System.out.println(global_cache.toString());
        }
    }
    public boolean  killNode(String uid) {
        ObjectNode node = readTable(uid);
        if(node != null){
            node.stop();
            System.out.println("Killed node");
            return true;
        }
        System.out.println("Couldn't kill node");
        return false;
    }
   
    //abstract methods need to be implemented by child classes
    public abstract void dumpTree();
    public abstract void startSystem();

    // Pass instance of this class to each node that is created
    public class AddressLookup {
    	public static final String TO_GLOBAL = "global";
        public AddressLookup() {}
        //this method will handle looking up the location of dest_uid
        //1) check local nameserver
        //2) check global cache
        //3) ask global nameserver for info
        public synchronized void sendMessage(Message msg, String source_uid, String dest_uid) {
            String addr;
            String backup_uid;
            ObjectNode rcvr;
            Message request;

            msg.setSourceUid(source_uid);
            msg.setDestUid(dest_uid);

            // Some message types go directly to the global name-server with dest_uid of "global"
            if (dest_uid.equals(TO_GLOBAL)) {
                sender.sendMessage(new Message(msg), global_ns);
                return;
            }

            // Look in lookup_table
            rcvr = readTable(dest_uid);
            if (rcvr != null) {
                msg.setSourceAddr(address);
                msg.setDestAddr(address);
                rcvr.enqueueMessage(new Message(msg));
                return;
            }

            if (msg.getType() == MessageType.FILE_RD) {
                // Look for resource backups on the local machine
                backup_uid = new String(dest_uid + "_" + uid); 
                backup_uid = backup_uid.replaceAll("_R", "_B");
                
                rcvr = readTable(backup_uid);
                if (rcvr != null) {
                    System.out.println("FOUND BACKUP");
                    msg.setSourceAddr(address);
                    msg.setDestAddr(address);
                    rcvr.enqueueMessage(new Message(msg));
                    return;
                }
            }
            
            if(cache_enabled){
                // Look in cache for external address
                addr = readCache(dest_uid);
                if (addr != null) {
                    //dumpTables();
                    sender.sendMessage(new Message(msg), addr);
                    return;
                }
            }

            // Need to go to global n-s
            CACHE_MISS++;
            request = new Message((source_uid + "#" + dest_uid + "#" + msg.getBody() + "#" + msg.getType() + "#" + msg.getTimestamp()), 
                "none", MessageType.SYS_LOOKUP);
            sender.sendMessage(request, global_ns);
        }
    }
}
