/*
 * CacheList.java
 * 
 * Cha Li
 * 15 December 2011
 * 
 * A linked data structure that implements a MRU cache.
 * 
 */

package cha.datastructs;

import java.io.Serializable;
import java.util.LinkedList;

public class CacheList<E> extends LinkedList<E> implements Serializable{

  private static final long serialVersionUID = 1205825369281221582L;
  
  private long cur_free, max_slots;
  
  public CacheList(){
    super();
    max_slots = cur_free = Long.MAX_VALUE;
    
  }
  
  public CacheList(long a_slots){
    super();
    max_slots = cur_free = a_slots;
  }
  
  @Override
  public synchronized boolean add(E a_item){
    boolean retval = false;
    if(cur_free > 0){
      cur_free--;
      super.addFirst(a_item);
      retval = true;
    }
    else{
      super.removeLast();
      super.addFirst(a_item);
      retval = true;
    }
    
    return retval;
  }
  
  @Override
  public synchronized void addLast(E a_item){
    if(cur_free > 0){
      cur_free--;
      super.addLast(a_item);
    }
    else{
      super.removeLast();
      super.addLast(a_item);
    }
  }
  
  @Override
  public synchronized E get(int a_index){
    E l_item = super.get(a_index);
    if(l_item != null){
      super.remove(l_item);
      super.addFirst(l_item);
    }
    return l_item;
  }
  
  public synchronized E get(E a_item){
    int l_index = super.indexOf(a_item);
    return this.get(l_index);
  }
  
  public synchronized void resize(long a_slots){
    if(a_slots <= 0)
      return;
    
    //increase size
    if(a_slots >= max_slots){
      cur_free += (a_slots - max_slots);
      max_slots = a_slots;
    }
    //decrease size
    else{
      while(a_slots < (max_slots - cur_free)){
        super.removeLast();
        cur_free++;
      }
      
      cur_free = a_slots - (max_slots - cur_free);
      max_slots = a_slots;
    }
  }
  
  @Override
  public synchronized void clear(){
    cur_free = max_slots;
    super.clear();
  }
  
  @Override
  public synchronized E poll(){
    E l_item = super.poll();
    if(l_item != null)
      cur_free++;
    
    return l_item;
  }
  
  @Override
  public synchronized E pop(){
    E l_item = super.pop();
    if(l_item != null)
      cur_free++;
    
    return l_item;
    
  }
  
  @Override
  public synchronized void push(E a_item){
    this.addLast(a_item);
  }
  
  @Override
  public synchronized E remove(){
    return this.remove(super.size() - 1);
  }
  
  @Override
  public synchronized E remove(int a_index){
    E l_item = super.remove(a_index);
    if(l_item != null)
      cur_free++;
    
    return l_item;
  }
  
  @Override
  public synchronized E removeFirst(){
    return this.remove(0);
  }
  
  @Override
  public synchronized E removeLast(){
    return this.remove(super.size() - 1);
  }
  
  @Override
  public synchronized String toString(){
    String str = "CacheList Max: " + this.max_slots + " [\n";
    for(E item : this){
      str += item.toString() + "\n";
    }
    str += "]";
    
    return str;
  }
  
}