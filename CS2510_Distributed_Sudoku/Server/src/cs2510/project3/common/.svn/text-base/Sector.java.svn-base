/*
 *  Sector.java
 * 
 *  Cha Li
 *  11.30.2011
 * 
 *  A sector is a 3x3 region of squares. There are 9 total sectors on a 
 *  Sudoku board.
 *  
 *  The indicies used to access individual squares are as follows:
 *  0 1 2
 *  3 4 5
 *  6 7 8 
 */
package cs2510.project3.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

import cs2510.project3.system.Utilities;



public class Sector implements Serializable{
  private static final long serialVersionUID = 8990593569418337414L;
  private ArrayList<Square> squares;
  private ArrayList<Integer> neighbor_ids;
  private int sid, sector_row, sector_col;
  
  //values contains 9 numbers, representing the initial values for the 9 squares
  //0 represents empty
  public Sector(int values[], int sector_id){
    this.squares = new ArrayList<Square>();
    
    for(int i = 0; i < Utilities.SUDOKU_NUM; ++i)
      this.squares.add(new Square(values[i], sector_id, i / Utilities.SUDOKU_DIM, i % Utilities.SUDOKU_DIM));
      
    this.sid = sector_id;
    this.sector_row = 0;
    this.sector_col = 0;
    this.neighbor_ids = new ArrayList<Integer>();
  }
  
  public Sector(int values[],
                int sector_id,
                int sector_row,
                int sector_col,
                ArrayList<Integer> adj){
    this.squares = new ArrayList<Square>();
    
    for(int i = 0; i < Utilities.SUDOKU_NUM; ++i)
      this.squares.add(new Square(values[i], sector_id, i / Utilities.SUDOKU_DIM, i % Utilities.SUDOKU_DIM));
    
    this.sid = sector_id;
    this.sector_row = sector_row;
    this.sector_col = sector_col;
    this.neighbor_ids = adj;
  }

  public Sector(String values[],
    int sector_id,
    int sector_row,
    int sector_col,
    ArrayList<Integer> adj){
    this.squares = new ArrayList<Square>();
    
    for(int i = 0; i < Utilities.SUDOKU_NUM; ++i)
      this.squares.add(new Square(Integer.parseInt(values[i]), sector_id, i / Utilities.SUDOKU_DIM, i % Utilities.SUDOKU_DIM));
    
    this.sid = sector_id;
    this.sector_row = sector_row;
    this.sector_col = sector_col;   
    this.neighbor_ids = adj;
   }
  
  //return the nth row of the sector
  public ArrayList<Integer> getRowValues(int n){
    ArrayList<Integer> row = new ArrayList<Integer>();
    int start = 3 * n;
    for(int i = start; i < start + 3; ++i)
      row.add(this.squares.get(i).getValue());
    return row;
  }
  public ArrayList<Square> getRowSquares(int n){
    ArrayList<Square> row = new ArrayList<Square>();
    int start = 3 * n;
    for(int i = start; i < start + 3; ++i)
      row.add(this.squares.get(i));
    return row;
  }
  
  //return the nth column of the sector
  public ArrayList<Integer> getColumnValues(int n){
    ArrayList<Integer> col = new ArrayList<Integer>();
    for(int i = n; i < this.squares.size(); i += 3)
      col.add(this.squares.get(i).getValue());
    return col;    
  }
  public ArrayList<Square> getColumnSquares(int n){
    ArrayList<Square> col = new ArrayList<Square>();
    for(int i = n; i < this.squares.size(); i += 3)
      col.add(this.squares.get(i));
    return col;    
  }
  
  
  public String toString(){
    String str = "";
    str += "Sector " + this.sid + " [r:" + this.sector_row + " c:" + this.sector_col + "]\n";
    str += "Neighbors: ";
    for(Integer i : this.neighbor_ids)
      str += i + " ";
    str += "\n";
    
    for(int i = 0; i < Utilities.SUDOKU_NUM; ++i){
      str += this.squares.get(i).getValue() + " ";
      str += (i % 3 == 2) ? "\n" : "";
    }
    
    str += "Consistent: " + this.isSelfConsistent() + "\n";
    str += "Solved: " + this.isSolved() + "\n";
    
    return str;
  }
  
  /*
    methods for sector checking
  */
  public boolean isSolved(){
    boolean solved = true;
    //check if the board is consistent with itself and check that all squares
    //are filled in
    if(solved = isSelfConsistent())
      for(Square s : this.squares)
        solved &= s.isFilled();
      
    return solved;
  }
  
  //consistent means all filled squares are unique and unfilled
  //squares have enough valid values to be solved.
  
  public boolean isSelfConsistent(){
    boolean consistent = true;
    LinkedList<Integer> possible = new LinkedList<Integer>();
    for(int i = 1; i <= 9; ++i)
      possible.add(i);
    
    int num_empty = 0;
    for(Square s : this.squares){
      //check for duplicate values
      if(s.isFilled()){
        if(possible.contains(new Integer(s.getValue())))
          possible.remove(new Integer(s.getValue()));
        else
          consistent = false;
      }
      //check for empty squares with no more possible values
      else if(!s.isFilled()){
        if(s.getNumPossibilities() <= 0)
          consistent = false;
        ++num_empty;
      }
    }
    
    //check if number of empty squares exceeds possible values
    if(num_empty > possible.size())
      consistent = false;
    
    return consistent;
  }
  
  //setters and getters
  public int getId(){ return this.sid; }
  public ArrayList<Square> getSquares(){ return this.squares; }
  public ArrayList<Integer> getNeighborIds(){ return this.neighbor_ids; }
}
