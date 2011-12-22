package cha.datastructs;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;

//I just made this class so we can print tables nicely
public class LookupTable <K, V> extends HashMap<K, V> implements Serializable{
    public static final int NO_LIM = Integer.MAX_VALUE;

    private static final long serialVersionUID = -5020507282755911530L;
    private String name;
    private int length;
    private CacheList<K> cache;     //linked list to keep track of most recently used hash keys
    //front [mru, ... , lru] back

    public LookupTable(String name){
        super();
        this.name = name; 
        this.length = NO_LIM;     //infinite length
        this.cache = new CacheList<K>();
    }
    
    public LookupTable(String name, int length){
        super();
        this.name = name;
        this.length = length;
        this.cache = new CacheList<K>();
    }

    @SuppressWarnings("unchecked")
	@Override
	//overriding get is weird...
    public V get(Object key){
        if(containsKey(key)){
            cache.get((K) key);
            return super.get(key);
        }
       return null;
    }

    @Override 
    public V put(K key, V value){
        if(this.length >= 0 && this.size() < this.length){
            //new entry is mru
            cache.add(key);
            return super.put(key, value);
        }
        else{
            //remove the oldest  entry to make space for the new entry
            //System.out.println("table limit reached, removing oldest entry.");
            cache.remove();
            cache.add(key);
            return super.put(key, value);
        }
    }

    @Override
    public V remove(Object key){
        if(containsKey(key)){
            cache.remove(key);
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
