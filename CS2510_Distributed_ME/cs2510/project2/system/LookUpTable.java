package cs2510.project2.system;

import java.util.HashMap;
import java.util.LinkedList;

//I just made this class so we can print tables nicely
public class LookUpTable <K, V> extends HashMap<K, V>{
    public static final int NO_LIM = Integer.MAX_VALUE;

   /**
     * auto-generated by eclipse
     */
    private static final long serialVersionUID = -5020507282755911530L;
    private String name;
    private int length;
    private LinkedList<K> mru;     //linked list to keep track of most recently used hash keys
    //front [mru, ... , lru] back

    public LookUpTable(String name){
        super();
        this.name = name; 
        this.length = NO_LIM;     //infinite length
        this.mru = new LinkedList<K>();
    }
    
    public LookUpTable(String name, int length){
        super();
        this.name = name;
        this.length = length;
        this.mru = new LinkedList<K>();
    }
    
    @SuppressWarnings("unchecked")
	@Override
	//overriding get is weird...
    public V get(Object key){
        if(containsKey(key)){
            //mark key as mru
            mru.remove(mru.indexOf(key));
            mru.addFirst((K) key);
            return super.get(key);
        }
       return null;
    }

    @Override 
    public V put(K key, V value){
        if(this.length >= 0 && this.size() < this.length){
            //new entry is mru
            mru.addFirst(key);
            return super.put(key, value);
        }
        else{
            //remove the oldest  entry to make space for the new entry
            //System.out.println("table limit reached, removing oldest entry.");
            K oldest = mru.removeLast();
            remove(oldest);
            
            mru.addFirst(key);
            return super.put(key, value);
        }
    }

    @Override
    public V remove(Object key){
        if(containsKey(key)){
            mru.remove(key);
            return super.remove(key);
        }
        return null;
    }

    @Override
    public String toString(){
        String str = "Table: " + this.name + " Max Length:" + this.length + "\n";
        for(K key : this.keySet())
            str += key.toString() + " -> " + this.get(key).toString() + "\n";
        return str;
    }
    
    //setters and getters
    public void setLength(int l){ this.length = l; }
    public int getLength(){ return this.length; }
}
