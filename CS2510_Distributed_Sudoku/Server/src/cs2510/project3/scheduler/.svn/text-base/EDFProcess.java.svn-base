package cs2510.project3.scheduler;

public class EDFProcess{
  private String data;   
  
  //in milliseconds
  private long exec_time;   //worst case execution time
  private long frequency;   //how often this runs
  private long start_time;  //the time this process initially started
  private long remaining;   //remaining processing time
  
  //statistics stuff
  public int EXEC = 0;      //number of times this task was scheduled
  public int PREEMPT = 0;   //number of times this task was pre-empted
  
  public EDFProcess(String data, long exec, long freq){
    this.exec_time = exec;
    this.frequency = freq;
    this.data = data;       //usually a name
    this.start_time = 0;
    this.remaining = 0;
    
  }
  
  //stringification
  public String toString(){
    return "Data:\t\t" + this.data + "\nWCET:\t\t" + this.exec_time + "\n" +
    		"Frequency:\t" + this.frequency + "\nUtilization:\t" + (float)this.exec_time / this.frequency + 
    		"\nScheduled: " + this.EXEC + "\nPre-Empted: " + this.PREEMPT;
  }
  
  public void initialize(){ 
    this.remaining = this.exec_time;
    this.EXEC++;
  }  
  public void tick(){ this.remaining--; }
  
  //setters and getters
  public void setStartTime(long start){ this.start_time = start; }
  
  
  public String getData(){ return this.data; }
  public long getStartTime(){ return this.start_time; }
  public long getExecTime(){ return this.exec_time; }
  public long getFrequency(){ return this.frequency; }
  public long getDeadline() { return this.start_time + this.frequency; }
  public float getUtilization(){ return (float)this.exec_time / this.frequency; }
  
  public boolean isDone(){ return this.remaining <= 0; }
  
}
