/*
 *  SudokuCLI.java
 *  
 *  Cha Li
 *  12.1.2011
 *  
 *  Command Line Interface that allows the operator to interact with the SudokuServer
 */
package cs2510.project3.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import cs2510.project3.common.Board;
import cs2510.project3.common.Sector;
import cs2510.project3.common.Square;

public class SudokuCLI implements Runnable{
  private SudokuServer ss;
  private BufferedReader br;
  private boolean running = true;
  
  public SudokuCLI(SudokuServer server){
    this.ss = server;
    br = new BufferedReader(new InputStreamReader(System.in));
  }
  
  public void run() {
    String command;
    String[] args;
    Board board;
    System.out.println("======== SudokuServer CLI ========\ntype 'help' for help");    
    while(running){
      System.out.print("SudokuServer >> ");
      try {
        command = br.readLine();
        command = command.trim();
        args = command.split(" ");
        if("print".equals(args[0]) && args.length > 1){
          board = ss.getBoard();
          if("board".equals(args[1])){
            if(board != null)
              System.out.println(board.toString());
            else
              System.out.println("Game hasn't started.");
          }
          else if(args.length == 3 && "sector".equals(args[1])){
            if(board != null){
              Sector s = board.getSectors().get(Integer.parseInt(args[2]));
              System.out.println(s.toString());
            }
            else
              System.out.println("Game hasn't started.");
          }
          else if(args.length == 4 && "square".equals(args[1])){
            if(board != null){
              Sector s = board.getSectors().get(Integer.parseInt(args[2]));
              Square sq = s.getSquares().get(Integer.parseInt(args[3]));
              System.out.println(sq.toString());
            }
            else
              System.out.println("Game hasn't started.");
          }
        }
        else if("tables".equals(args[0])){
          System.out.println("==== Node Map ====\n" + ss.getNodeMap().toString());
          String s = (ss.getSectorMap() == null) ? "" : ss.getSectorMap().toString();
          System.out.println("==== Sector Map ====\n" +  s);
          System.out.println("==== State Lists ====");
          s = (ss.getActiveList() == null) ? "" : ss.getActiveList().toString();
          System.out.println("Active:\n" + s);
          s = (ss.getReadyList() == null) ? "" : ss.getReadyList().toString();
          System.out.println("Ready:\n" + ss.getReadyList().toString());
          s = (ss.getIdleList() == null) ? "" : ss.getIdleList().toString();
          System.out.println("Idle:\n" + ss.getIdleList().toString());
        }
        else if("start".equals(args[0]) && args.length == 2){
          if("easy".equals(args[1]))
            ss.start(args[1]);
          else if("med".equals(args[1]))
            ss.start("medium");
          else if("hard".equals(args[1]))
            ss.start(args[1]);
          else if("imp".equals(args[1]))
            ss.start("impossible");

          if((board = ss.getBoard()) != null)
            System.out.println(board.toString());
        }
        else if("throttle".equals(args[0]) && args.length == 2){
          int count = Integer.parseInt(args[1]);
          if(count < 0)
            System.out.println("Enter a postive number");
          else if(!ss.throttle(count))
            System.out.println("Failed to throttle " + count + " nodes");
          else
            System.out.println("Throttled " + count + " nodes");
          
        }
        else if("stat".equals(args[0])){
          System.out.println(ss.toString());
          //System.out.println("Polling System Status...");
          //ss.fetchSectors();
        }
        else if("exit".equals(args[0])){
          /*
          ss.stop();
          running = false;
          */
          System.exit(0);
        }
        else if("help".equals(args[0])){
          System.out.println("\n" +
            "start {easy | med | hard | imp}\n\tstart a game\n\n" +
            "throttle <int>\n\tset the number of idle nodes in the system\n\n" +
            "print {board | sector <id> | square <sector_id> <square_id> }\n\tprint status\n\n" + 
            "stat\n\tsystem status\n\n" +
            "tables\n\tdisplay current lookup tables\n\n" +
            "help\n\tdisplay this message\n\n" +
            "exit\n\tend the system\n\n");
      }
    }
    catch (IOException e) {
        e.printStackTrace();
    }      
    }
  }
  
  public void stop(){
    running = false;
  }

}
