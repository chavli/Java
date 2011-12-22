/*
 *  Board.java
 *  Cha Li
 *  11.30.2011
 *  
 *  An entire Sudoku board.
 */
package cs2510.project3.common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;

import cha.app.cs2510.backend.Utilities;

public class Board {
  private LinkedList<Sector> sectors;
  private BufferedReader in;
  private Random random;
  private String filename;
  
  public Board(String difficulty){
    random = new Random();
    sectors = new LinkedList<Sector>();
    
    //pick a random board from the chosen difficulty
    int board = random.nextInt(Utilities.NUM_BOARDS) + 1;
    filename = "puzzles/" + difficulty + "/" + board + ".puz";
    try {
      String line;
      in = new BufferedReader(new FileReader(filename));
      
      //each line of the file represents a sector of the board. where line N
      //represents sector N. sectors are laid out in the following manner:
      //0 1 2
      //3 4 5
      //6 7 8
      int sid = 0;  //sector id
      while((line = in.readLine()) != null){
        Sector s = new Sector(line.split(" "), sid, sid / 3, sid % 3, Utilities.getNeighborIds(sid));
        sectors.add(s);
        ++sid;
      }
    }catch (IOException e){
      e.printStackTrace();
    }
  }
  
  public String toString(){
    String str = "Board: " + filename + "\n";
    //go through each row of sectors
    for(int i = 0; i < 9; i += 3){
      //go through each row within a sector
      for(int j = 0; j < 3; ++j){
        str += this.sectors.get(i).getRowValues(j).toString() + " " + 
            this.sectors.get(i + 1).getRowValues(j).toString() + " " + 
            this.sectors.get(i + 2).getRowValues(j).toString() + "\n";
      }
      str += "\n";
    }
    return str;
  }
  
  //setters and getters
  public LinkedList<Sector> getSectors(){ return this.sectors; }
  
}
