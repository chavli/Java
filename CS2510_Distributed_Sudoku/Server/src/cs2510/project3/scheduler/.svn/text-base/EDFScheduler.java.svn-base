package cs2510.project3.scheduler;

import java.util.LinkedList;
import java.util.Random;

import cs2510.project3.common.SystemMessage;
import cs2510.project3.common.SystemMessage.SystemMessageType;
import cs2510.project3.server.SudokuServer.MessageProcessor;

public class EDFScheduler implements Runnable{
  
  //scheduling varibles
  private boolean scheduling;
  private boolean active;
  private LinkedList<EDFProcess> processes;
  //hold on to finished nodes so their stats can be printed later
  private LinkedList<EDFProcess> finished;
  private EDFProcess current;
  private float sys_utilization;
  
  //a offset counter used to keep track of how much time the scheduler is inactive
  //the scheduler becomes inactive when the system is handling / recovering from 
  //a fault
  private long time_offset;
  private long last_active;
  private long idle_ticks;
  
  //the message processor used to send messages
  private MessageProcessor message_proc;
  SystemMessage<Object> outgoing;
 
  //random number generator
  private Random random;
  
  //upper limits used for random numbers
  private final static long LOWER_WCET = 1000;  //1s 
  private final static long UPPER_WCET = 3000;  //5s 
  private final static long LOWER_FREQ = 1000; //3s
  private final static long UPPER_FREQ = 10000; //10s
      
  public EDFScheduler(MessageProcessor mp){
    this.processes = new LinkedList<EDFProcess>();
    this.finished = new LinkedList<EDFProcess>();
    this.sys_utilization = 0;
    this.scheduling = false;
    this.active = true;
    this.random = new Random();
    this.message_proc = mp;
    
    this.time_offset = 0;
    this.last_active = 0;
    this.idle_ticks = 0;
  }

  //remove a process from the scheduler. utilization goes down as a result
  public void removeProcess(String name){
    synchronized(processes){
      for(EDFProcess process : processes){
        if(process.getData().equals(name)){
          processes.remove(process);
          finished.add(process);
          if(process.equals(current))
            current = null;
          sys_utilization -= process.getUtilization();
          break;
        }
      }
    }
  }
  
  //admission control, returns whether or not the process was added
  public boolean addProcess(String name){
    long wcet = (Math.abs(random.nextLong()) % UPPER_WCET) + LOWER_WCET;
    long period = wcet + (Math.abs(random.nextLong()) % UPPER_FREQ) + LOWER_FREQ;
    EDFProcess node = new EDFProcess(name, wcet, period);
    return admissionControl(node, (float)wcet / period);
  }
  
  //returns whether or not the node was accepted into the EDF queue
  //given the utilization of the new process Ui, if the combined utilization
  //of the system, Us, and new process is < than accept.
  //
  //  accept: Ui + Us < 1.0
  private boolean admissionControl(EDFProcess process, float utilization){
    if(sys_utilization + utilization >= 1.0){
      System.out.println("ADMISSION CONTROL -- Rejected: \n" + process.toString());
      return false;
    }
    else{
      sys_utilization += utilization;
      System.out.println("ADMISSION CONTROL -- Accepted: \n" + process.toString());
      admitProcess(process);
      return true;
    }
  }
  
  //add an EDF process to the EDF queue based on its deadline
  private void admitProcess(EDFProcess process){
    synchronized(processes){
      
      if(processes.isEmpty()){
        if(process.isDone()){
          process.setStartTime(System.currentTimeMillis() + (process.getFrequency() - process.getExecTime()) - time_offset);
          process.initialize();
        }
        else
          process.setStartTime(System.currentTimeMillis() - time_offset);
        processes.add(process);
      }
      
      //figure out where this node belongs in the EDF queue
      else{
        //if the process is done, schedule it based on its frequency
        if(process.isDone()){
          process.setStartTime(System.currentTimeMillis() + (process.getFrequency() - process.getExecTime()) - time_offset);
          process.initialize();
        }
        
        boolean added = false;
        for(int i = 0; i < processes.size(); i++){
          if(process.getDeadline() < processes.get(i).getDeadline()){
            added = true;
            processes.get(i).PREEMPT++;
            processes.add(i, process);
            break;
          }
        }
        if(!added)
          processes.addLast(process);
      }
    }
  }
  
  public String toString(){
    return "System Utilization: " + sys_utilization + "\n" +
        "Online: " + active + "\n" +
        "Scheduling: " + scheduling + "\n" +
        "Current EDF Node: \n " + ((current == null) ? "none" : current.toString()) + "\n" +
        "EDF Queue: " + processes.toString() + "\n";
  
  }
  
  //setters and getters
  public float getSystemUtilization(){  return sys_utilization; }
  
  
  //this is the scheduler
  public void run() {
    last_active = System.currentTimeMillis();
    while(active){
      synchronized(processes){
        if(scheduling){
          time_offset += idle_ticks;
          last_active = System.currentTimeMillis();
          idle_ticks = 0;
          
          //first sanity check, are there processes to work on?
          if(current != null && !processes.isEmpty()){
            //if the current process is not the head, then it has been preempted
            if(current.isDone() || !current.equals(processes.peek())){
              
              //keep track of times this process was pre-empted
              if( !current.equals(processes.peek()) )
                current.PREEMPT++;
              
              //tell current node to stop running
              processes.remove(current);
              outgoing = new SystemMessage<Object>("EDF Scheduler: IDLE", SystemMessageType.NODE_SLEEP);
              message_proc.send(outgoing, current.getData());
 
              admitProcess(current);
              
              //set current to the new head process and start it
              current = processes.peek();
              
              //System.out.println("2 " + current.getStartTime());
              
              //check if the process is scheduled to run
              if(current.getStartTime() + time_offset <= System.currentTimeMillis()){
                outgoing = new SystemMessage<Object>("EDF Scheduler: WAKE", SystemMessageType.NODE_WAKE);
                message_proc.send(outgoing, current.getData());
                current.initialize();
              }
              else
                current = null;
            }
            //current thread is still has earliest deadline, make it tick
            else
              current.tick();
            
          }//end sanity check
          else if(current == null && !processes.isEmpty()){
            current = processes.peek();
            //only wake it up if it's scheduled to start at this time
            if(current.getStartTime() + time_offset <= System.currentTimeMillis()){
              outgoing = new SystemMessage<Object>("EDF Scheduler: WAKE", SystemMessageType.NODE_WAKE);
              message_proc.send(outgoing, current.getData());
            }
            //no nodes to schedule
            else
              current = null;
          }
          
        }//end check
        else
          idle_ticks = System.currentTimeMillis() - last_active;
      }//end synch
      
      //sleep for a ms, this is so the tick function works correctly
      try {
        Thread.sleep(1);
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
    }//end active loop
  }
  
  public void start(){
    synchronized(processes){ scheduling = true; }
  }
  
  public void stop(){
    scheduling = false;
    active = false;
    
    //print final status of nodes
    System.out.println("==== FINAL PROCESS STATS ====");
    for(EDFProcess process : finished)
      System.out.println(process.toString() + "\n");
    
    synchronized(processes){ this.processes.clear(); }
    synchronized(finished){ this.finished.clear(); }
    sys_utilization = 0;
  };
  
  /*
   * these are only used when the system is handling and recovering from
   * faults. they shouldn't be used because you feel like it.
   * 
   */
  public void pause(){
    synchronized(processes){ scheduling = false; };
  }
  
  public void resume(){
    synchronized(processes){
      scheduling = true;
    }
  }
}
