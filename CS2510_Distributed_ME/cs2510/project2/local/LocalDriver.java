package cs2510.project2.local;

import cs2510.project2.system.CLI;

import java.io.*;

public class LocalDriver {
	public static void main(String args[]){
		String global;
		int  num_users, num_resources;
        long start_time;
        long run_time = 600000; //ms, 10min
        //long run_time = 60000; //ms, 1min

		if(args.length != 11){  
		  System.out.println("usage: LocalDriver <global nameserver addr> <num users> <num files>\n " +
		  		"LocalDriver oxygen.cs.pitt.edu 5 4");
		}
		else{
			global = args[0];
			num_users = Integer.parseInt(args[1]);
			num_resources = Integer.parseInt(args[2]);
            int port = Integer.parseInt(args[3]);
            int read = Integer.parseInt(args[4]);
            int write = Integer.parseInt(args[5]);
            int fail1 = Integer.parseInt(args[6]);
            int fail2 = Integer.parseInt(args[7]);
            int fail3 = Integer.parseInt(args[8]);
            int me_type = Integer.parseInt(args[9]);
            int cache = Integer.parseInt(args[10]);
			LocalNameServer ns = new LocalNameServer(num_users, num_resources, global, port, read, write, fail1, fail3, fail3, me_type, cache);
	        start_time = System.currentTimeMillis();
			//start the cli
			new Thread(new CLI(ns)).start();

            while((System.currentTimeMillis() - start_time) < run_time){}
            String data = ns.statsToCSV();
            ns.stop();
            
            try{
                File output = new File(global + "_" + port + ".csv")    ;
                FileWriter fw = new FileWriter(output, true);
                fw.write(data + "\n");
                fw.close();
                System.out.println("DONE");
            }catch(IOException e){ e.printStackTrace(); }
		}


		
	}
}
