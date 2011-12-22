/*
 *  Square.java
 *  
 *  Cha Li
 *  11.30.2011
 * 
 * 
 *  Represents a single square in a sudoku board which can hold a single number
 */

package cs2510.project3.common;

import java.io.Serializable;
import java.util.LinkedList;

import cs2510.project3.system.Utilities;

public class Square implements Serializable{
  private static final long serialVersionUID = 2887358663428902228L;
  private int value;                      //current value held in this square. 0 represents no value
  private LinkedList<Integer> possible;            //possible values this square can hold
  private int sector_id, row_id, col_id;  //ids used to represent where this square is in the board
  
  public Square(int sector, int row, int col){
    this.value = 0;
    this.possible = new LinkedList<Integer>();
    for(int i = 1; i <= Utilities.SUDOKU_NUM; ++i)
      this.possible.add(i);
    this.sector_id = sector;
    this.row_id = row;
    this.col_id = col;
  }
    
  public Square(int value, 
                int sector, 
                int row, 
                int col){
    this.value = value;
    
    //empty list, since square is filled in
    this.possible = new LinkedList<Integer>();
    for(int i = 1; i <= Utilities.SUDOKU_NUM; ++i)
      this.possible.add(i);
    this.possible.remove(new Integer(value));
    
    this.sector_id = sector;
    this.row_id = row;
    this.col_id = col;
  }
  
  public String toString(){
    String str = "";
    
    str += "Square s:" + this.sector_id + " r:" + this.row_id + " c:" + this.col_id + "\n";
    str += "Current Value: " + this.value + "\n";
    str += "Possible Values: " + this.possible.toString() + "\n";
    str += "Filled In: " + this.isFilled() + "\n";
    return str;
  }
  
  public boolean removePossibility(int i){
    return this.possible.remove(new Integer(i));
  }
  
  public void clearPossibilities(){
    this.possible.clear();
  }
  
  //this should never be called unless the board makes a mistake
  public boolean addPossibility(int i){
    if(!this.possible.contains(new Integer(i)))
      return this.possible.add(i);
     
    return false;
  }
  
  //fill in the square with the first (last remaining) value in possibility
  public void fill(){
    setValue(possible.get(0));
  }
  
  //setters and getters
  public void setValue(int value){
    this.value = value;
    this.possible.clear();
  }
  
  
  public int getValue(){  return this.value;  }
  public int getNumPossibilities(){ return this.possible.size();  }
  public int getSectorId(){ return this.sector_id;  }
  public int getRowId(){ return this.row_id;  }
  public int getColId(){ return this.col_id;  }
  public boolean isFilled(){  return this.value != 0; }
}
