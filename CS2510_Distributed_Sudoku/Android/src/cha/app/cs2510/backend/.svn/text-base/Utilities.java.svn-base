package cha.app.cs2510.backend;

import java.util.ArrayList;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class Utilities {
	public static String getSystemIP(Context c){
        WifiManager wifi_manager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifi_info = wifi_manager.getConnectionInfo();
        return intToIP(wifi_info.getIpAddress());
	}
	
	private static String intToIP(int address){
		return 	(address & 0xFF) + "." + 
				((address >> 8 ) & 0xFF) + "." + 
				((address >> 16 ) & 0xFF) + "." + 
				((address >> 24 ) & 0xFF );
	}
	
	
  public final static int NUM_BOARDS = 5; //number of puzzle files per difficulty
  public final static int SUDOKU_DIM = 3; //edge size of board
  public final static int SUDOKU_NUM = 9; //a common value found thoughout sudoku
  public final static int PORT = 10007;    //port used by this system
  public final static long TIMEOUT = 60000;  //1 minute
  
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
}
