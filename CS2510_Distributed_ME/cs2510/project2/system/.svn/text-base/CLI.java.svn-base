package cs2510.project2.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class CLI implements Runnable{
    private NameServer ns;
    private BufferedReader br;
    private boolean running = true;
    
    public CLI(NameServer ns){
        this.ns = ns;
        br = new BufferedReader(new InputStreamReader(System.in));
    }

    public void stop(){
        running = false;
    }
    
    public void run() {
        String command;
        String[] args;
        System.out.println("======== NameServer CLI ========\ntype 'help' for help");
        while(running){
            System.out.print(ns.address + ">> ");
            try {
                command = br.readLine();
                args = command.split(" ");
                if("tables".equals(args[0]))
                    ns.dumpTables();
                else if("trees".equals(args[0])){
                    ns.dumpTree();
                }
                else if ("kill".equals(args[0])) {
                    if(!ns.killNode(args[1]))
                        System.out.println(args[1] + " not found.");
                }
                else if("help".equals(args[0])){
                    System.out.println("\n" +
                            "start\t-\tstart the distributed system\n" + 
                    		"tables\t-\tdisplay current lookup tables\n" +
                    		"tree\t-\tdisplay current raymond tree\n" +
                    		"kill <uid>\t-\tkill a node by its uid\n" +
                            "stats\t-\tprint nameserver stats\n" + 
                    		"help\t-\tdisplay this message\n" +
                    		"exit\t-\tend this name server\n");
                }
                else if("start".equals(args[0])){
                    ns.startSystem();
                }
                else if("stats".equals(args[0])){
                    System.out.println(ns.statsToString());
                }
                else if("exit".equals(args[0])){
                    ns.stop();
                    break;
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        //TODO: end the program more gracefully --> end all threads
        System.exit(0); 
    }
}
