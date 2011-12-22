package cs2510.project3.system;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;

public class Utilities {
  private static final String LOG = "sudoku.log";
  public static final int PORT = 10007;
  public final static long TIMEOUT = 60000;  //1 minute
  public final static long FETCH_TIMEOUT = 5000;  //5 seconds. this is used to grab a quick overview of the system 
  
  //sudoku constants
  public final static int NUM_BOARDS = 5; //number of puzzle files per difficulty
  public final static int SUDOKU_DIM = 3; //edge size of board
  public final static int SUDOKU_NUM = 9; //a common value found thoughout sudoku
  
  public static ArrayList<Integer> getNeighborIds(int sid){
    ArrayList<Integer> ids = new ArrayList<Integer>();
    int nid;  //neighbor id
    
    //check all horizontal and vertical neighbors
    for(int i = -2; i <= 2; ++i)
      if(i != 0){
        //horizontal neighbor, check left to right
        nid = sid + i;
        if(validSectorId(nid) && sameSectorRow(sid, nid))
          ids.add(new Integer(nid));
        
        //vertical neighbors, check up to down
        nid = sid + (SUDOKU_DIM * i);
        if(validSectorId(nid) && sameSectorCol(sid, nid))
          ids.add(new Integer(nid));
      }

    return ids;
  }
  
  /*
   * Sector ID (sid) methods
   */
  
  //determine if a sector id is within the bounds of the board
  public static boolean validSectorId(int sid){
    return sid >= 0 && sid < SUDOKU_NUM;
  }
  
  public static boolean sameSectorRow(int sid, int sid2){
    return validSectorId(sid) &&
        validSectorId(sid2) &&
        (sid / 3) == (sid2 / 3);
  }
  
  public static boolean sameSectorCol(int sid, int sid2){
    return validSectorId(sid) &&
        validSectorId(sid2) &&
        (sid % 3) == (sid2 % 3);
  }
  
  
  
  public static void log(String str){
    try{
      FileWriter fstream = new FileWriter(LOG, true);
      BufferedWriter out = new BufferedWriter(fstream);
      out.write("str");
      out.close();
    }catch (Exception e){
      e.printStackTrace();
    }
  }
}
