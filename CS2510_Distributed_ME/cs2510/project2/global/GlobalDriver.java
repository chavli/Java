package cs2510.project2.global;

import cs2510.project2.system.*;

public class GlobalDriver {
    public static void main(String args[]){
    	GlobalNameServer ns = new GlobalNameServer(Integer.parseInt(args[0]));
    	
    	//start the cli
    	new Thread(new CLI(ns)).start();
    }
}
