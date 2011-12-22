package cs2510.project3.server;

public class SudokuDriver {

  public static void main(String[] args) {
    boolean real_time = true; //use the EDF scheduler
    
    SudokuServer server = new SudokuServer(real_time);
    
    //start the cli
    new Thread(new SudokuCLI(server)).start();
  }
}
