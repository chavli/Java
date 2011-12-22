package cs2510.project2.system;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;

import java.io.IOException;
import java.util.Random;

import cs2510.project2.system.Message.MessageType;
import cs2510.project2.system.NameServer.AddressLookup;


public class ResourceNode extends ObjectNode implements Runnable {
    private String file_path = "default/file/path";
    private File file;
    private boolean gc = false;
    
    //file IO
    private FileWriter fw;
    private FileReader fr;

    public ResourceNode (String uid, AddressLookup sender) {
        super(uid, sender);
        this.type = ObjectType.RESOURCE;
        this.file_path = this.ouid + ".cha";

        //create the file this node represents
        try{
            this.file = new File(file_path);
            file.createNewFile();
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    // Creates backup files
    public ResourceNode (String uid, AddressLookup sender, String data) {
        super(uid, sender);
        this.type = ObjectType.RESOURCE;
        this.file_path = uid + ".cha";

        //create the file this node represents
        try{
            this.file = new File(file_path);
            file.createNewFile();
            fw = new FileWriter(this.file, true);
            fw.write(data + "\n");
            fw.close();
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }
  
    @Override
    public String toString(){
        return "Resource: " + file_path;
    }
  
    public void stop(){
        NameServer.FT_RES_DEAD++;
        System.out.println("Deleted FILE: " + this.file_path);
        file.delete();
        alive = false;
    }
  
    //do something
    public void run() {
        Message incoming;
        while(alive){
            if( (incoming = peekAndRemove()) != null ){
                String body, dest_uid, source_uid;
                char []buf;
                MessageType type;
                Message reply;

                type = incoming.getType();
                source_uid = incoming.getSourceUid(); 
                switch(type){
                    case FILE_RD:        
                        try{
                            fr = new FileReader(this.file);
                            buf = new char[(int)this.file.length()];
                            fr.read(buf);
                            body = new String(buf);
                            fr.close();

                            reply = new Message(body, "", MessageType.FILE_RD_R);
                            sender.sendMessage(reply, this.ouid, source_uid);

                        }catch(IOException ex){
                            ex.printStackTrace();
                        }

                        break;
                    case FILE_WR:
                        //body contains the data to write to the file
                        //purposely leave out writer-thread safety because at this point ME
                        //is suppose to be guaranteed
                        body = incoming.getBody(); 
                        
                        try{
                            fw = new FileWriter(this.file, true);
                            fw.write(body + "\n");
                            fw.close();

                            reply = new Message("" + body.length(), "", MessageType.FILE_WR_R);
                            sender.sendMessage(reply, this.ouid, source_uid);
                        }catch(IOException ex){
                            ex.printStackTrace();
                        }
                        break;
                    default:
                        System.out.println(this.ouid + ": Unrecognized file command");
                        break;
                }//end switch
            }

        }//end loop
        while (true) {}
    }
}
