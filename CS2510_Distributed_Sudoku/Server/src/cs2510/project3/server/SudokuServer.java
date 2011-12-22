package cs2510.project3.server;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Random;

import cs2510.project3.common.Board;
import cs2510.project3.common.LookUpTable;
import cs2510.project3.common.Sector;
import cs2510.project3.common.SystemMessage;
import cs2510.project3.common.SystemMessage.SystemMessageType;
import cs2510.project3.scheduler.EDFScheduler;
import cs2510.project3.system.ReceiverSocket;
import cs2510.project3.system.SenderSocket;
import cs2510.project3.system.Utilities;

public class SudokuServer {
  // maps node_ids to network addresses
  private LookUpTable<String, String>  node_map;

  // different states nodes can be in
  private Boolean                      state_lock = new Boolean(false);
  private LinkedList<String>           active_list;
  private LinkedList<String>           ready_list;
  private LinkedList<String>           idle_list;

  // maps sector_ids to network addresses
  private LookUpTable<Integer, String> sector_map;

  // the message processor
  private MessageProcessor             message_proc;

  //the EDF scheduler
  private EDFScheduler                 scheduler;
  
  // the server
  private SudokuServer                 sudoku_server;

  // address of this server
  private String                       address;

  // the sudoku board
  private Board                        sudoku;

  // random number generator
  private Random                       random;

  // has the system started solving the board
  private boolean                      EDF_ACTIVE;
  
  private String                       name       = "SudokuServer";
  
  //statistics tracking
  private float TOTAL_POWER = 0;
  private int TOTAL_MSGS = 0;
  private String PWR_HIST = "";
  
  public SudokuServer(boolean use_edf) {
    EDF_ACTIVE = use_edf;
    node_map = new LookUpTable<String, String>("Node Map");

    // keep track of node states
    active_list = new LinkedList<String>();
    idle_list = new LinkedList<String>();
    ready_list = new LinkedList<String>();
    
    sudoku_server = this;
    
    message_proc = new MessageProcessor();
    new Thread(message_proc).start();
    
    if(EDF_ACTIVE){
      scheduler = new EDFScheduler(message_proc);
    }
  }

  // start the game
  public boolean start(String difficulty) {
    boolean retval = false;
    if (node_map.size() == 0)
      System.out.println("No Nodes Connected");
    else {
      // generate the board
      sudoku = new Board(difficulty);
      
      // distribute the sectors
      if(EDF_ACTIVE)
        new Thread(scheduler).start();
      distributeSectors(new LinkedList<Sector>(sudoku.getSectors()), node_map.values());
    }
    return retval;
  }

  // end the server and stop all nodes
  public void stop() {
    if (message_proc != null)
      message_proc.stop();

    sector_map.clear();
    node_map.clear();
    synchronized (state_lock) {
      active_list.clear();
      idle_list.clear();
      ready_list.clear();
    }
  }

  // set the percentage of nodes this system will use
  public boolean throttle(float percent) {
    return false;
  }

  // set the number of nodes this system will use
  public boolean throttle(int nodes) {
    boolean success = true;
    SystemMessage<Object> outgoing;
    // number of nodes to be throttled exceeds total nodes in the system
    synchronized (state_lock) {
      if (nodes > idle_list.size() + active_list.size() + ready_list.size())
        success = false;
      else {
        if (nodes > idle_list.size()) {
          // first idle active nodes
          while (!active_list.isEmpty() && nodes > idle_list.size()) {
            String address = active_list.poll();
            idle_list.add(address);
            outgoing = new SystemMessage<Object>("", SystemMessageType.NODE_SLEEP);
            message_proc.send(outgoing, address);
          }

          // then idle ready nodes if we have to
          while (!ready_list.isEmpty() && nodes > idle_list.size()) {
            String address = active_list.poll();
            idle_list.add(address);
            outgoing = new SystemMessage<Object>("", SystemMessageType.NODE_SLEEP);
            message_proc.send(outgoing, address);
          }
        }
        // if nodes < idle count, wake some up
        else {
          while (nodes < idle_list.size() && !idle_list.isEmpty())
            ready_list.add(idle_list.poll());
        }
      }
    }
    return success;
  }

  // polls the system for the current state and redistributes
  // it.
  // 1. ask each node for it's state
  // 2. try to merge states into 1 megastate, otherwise choose the most solved
  // 3. split up sectors and redistribute among connected nodes
  private void distributeSectors(LinkedList<Sector> sectors, Collection<String> addresses) {
    SystemMessage<Object> outgoing;
    LinkedList<Sector> partition;
    LinkedList<String> ready_now;
    sector_map = new LookUpTable<Integer, String>("Sector Map");
    random = new Random();

    ready_now = new LinkedList<String>(addresses);

    synchronized (state_lock) {
      // update state info
      active_list.clear();
      for (String s : ready_list)
        if (!ready_now.contains(s))
          ready_now.add(s);
      ready_list.clear();

      // remove any overlap with the idle_list
      ready_now.removeAll(idle_list);

      // if now available nodes, force a node to be ready
      if (ready_now.isEmpty()) {
        System.out.println("All nodes idle, forcing one into active state.");
        ready_now.add(idle_list.poll());
      }

      int pieces = (int) Math.floor((double) sectors.size() / ready_now.size());
      int remainder = sectors.size() % ready_now.size();
      int extra = 0;

      // for each connected node
      for (String address : ready_now) {
        partition = new LinkedList<Sector>();
        // evenly hand out the sectors

        extra = (remainder-- > 0) ? 1 : 0;

        for (int i = 0; i < pieces + extra; ++i) {
          int index = random.nextInt(sectors.size());
          Sector s = sectors.remove(index);
          partition.add(s);
          sector_map.put(new Integer(s.getId()), address);
        }

        // send everything
        outgoing = new SystemMessage<Object>("", SystemMessageType.SYS_DIST);
        outgoing.setAttachment(partition);
        message_proc.send(outgoing, address);

        // update the active list
        active_list.add(address);

      }
    }
    
    //if using EDF, distribute the sector map to all nodes but let the scheduler
    //decide which node can continue running
    if(EDF_ACTIVE){
      for (String address : ready_now) {
        outgoing = new SystemMessage<Object>("", SystemMessageType.SYS_SECMAP);
        outgoing.setAttachment(sector_map);
        message_proc.send(outgoing, address);
      }
      scheduler.resume();
    }
    //otherwise, start all nodes in parallel with the new info
    else{
      for (String address : ready_now) {
        outgoing = new SystemMessage<Object>("", SystemMessageType.SYS_START);
        outgoing.setAttachment(sector_map);
        message_proc.send(outgoing, address);
      }
    }
  }

  // get the state of all sectors
  public void fetchSectors() {
    SystemMessage<Object> outgoing;
    for (Entry<String, String> entry : node_map.entrySet()) {
      outgoing = new SystemMessage<Object>("", SystemMessageType.NODE_FETCH);
      message_proc.send(outgoing, entry.getValue());
    }
  }
  
  public String toString(){
    String stat =  "-------- SYSTEM STATUS --------\n" + 
        "EDF Scheduler Enabled: " + (scheduler != null) + "\n";
    if(scheduler != null)
      stat  += scheduler.toString();
    
    return stat;
  }

  // setters and getters
  public String getAddress() { return this.address; }
  public String getName() { return this.name; }
  public Board getBoard() { return this.sudoku; }
  public LookUpTable<String, String> getNodeMap() { return this.node_map; }
  public LookUpTable<Integer, String> getSectorMap() { return this.sector_map; }
  public LinkedList<String> getActiveList() { return this.active_list; }
  public LinkedList<String> getIdleList() { return this.idle_list; }
  public LinkedList<String> getReadyList() { return this.ready_list; }

  // inner classes for message handling
  public class MessageProcessor implements Runnable {
    private SenderSocket   sender;
    private ReceiverSocket receiver;
    private boolean        processing;

    public MessageProcessor() {
      processing = true;
      receiver = new ReceiverSocket(Utilities.PORT);

      // start listening
      new Thread(receiver).start();

      // start the sender socket with no connections
      sender = new SenderSocket(getAddress());
    }

    public void send(SystemMessage<Object> msg, String dest) {
      sender.sendMessage(msg, dest);
    }

    @SuppressWarnings("unchecked")
    public void run() {
      // used to process incoming messages
      SystemMessage<Object> incoming, outgoing;
      SystemMessageType type;
      String body, src_addr, src_uid;
      String args[];
      Object attachment;
      LinkedList<Sector> sectors = new LinkedList<Sector>();
      LinkedList<String> responses = new LinkedList<String>();

      // variables to keep track of when certain events happen
      long last_done = Long.MAX_VALUE;
      long last_stuck = Long.MAX_VALUE;
      long last_fetch = Long.MAX_VALUE;

      while (processing) {
        // check if any timeouts have expired
        if (System.currentTimeMillis() - last_done > Utilities.TIMEOUT) {
          System.out.println("Possible Dead Nodes, redistributing sectors.");
          sudoku_server.distributeSectors(new LinkedList<Sector>(sudoku.getSectors()), responses);
          responses = new LinkedList<String>();
          last_done = Long.MAX_VALUE;
        }
        if (System.currentTimeMillis() - last_stuck > Utilities.TIMEOUT) {
          System.out.println("Possible Dead Nodes, redistributing sectors.");
          distributeSectors(new LinkedList<Sector>(sudoku.getSectors()), responses);
          responses = new LinkedList<String>();
          last_stuck = Long.MAX_VALUE;
        }
        if (System.currentTimeMillis() - last_fetch > Utilities.FETCH_TIMEOUT) {
          System.out.println("Fetched all sector data: ");
          System.out.println(sudoku.toString());
          
          System.out.println("Power Usage History:\n");
          System.out.println(sudoku_server.PWR_HIST + "\n");
          System.out.println("Total Power Used: " + sudoku_server.TOTAL_POWER + "\n");
          System.out.println("Total Messages Passed: " + sudoku_server.TOTAL_MSGS + "\n");
          
          sudoku_server.PWR_HIST = "";
          sudoku_server.TOTAL_MSGS = 0;
          sudoku_server.TOTAL_POWER = 0;
          last_fetch = Long.MAX_VALUE;
        }

        // respond to different types of incoming messages
        if (receiver != null) {
          incoming = receiver.getMessage();
          if (incoming != null) {
            System.out.println(incoming.toString());
            // break out pieces of the message
            type = incoming.getType();
            body = incoming.getBody();
            attachment = incoming.getAttachment();
            src_addr = incoming.getSourceAddr();
            src_uid = incoming.getSourceUid();

            switch (type) {
              case NODE_FETCH_R:
                last_fetch = System.currentTimeMillis();
                if (!responses.contains(src_addr)) {
                  responses.add(src_addr);
                }
                sectors = (LinkedList<Sector>) attachment;
                
                /*
                 * The body of the message is a comma separated list containing:
                 * [0] - power usage samples
                 * [1] - current amount of power used
                 * [2] - current number of messages sent
                 * [3] - current number of sector redistributions
                 */
                args = body.split(":");
                sudoku_server.PWR_HIST += src_addr + " -- " + args[0] + "\n";
                sudoku_server.TOTAL_MSGS += Integer.parseInt(args[2]);
                sudoku_server.TOTAL_POWER += Float.parseFloat(args[1]);
                
                
                // stuck node returns state of sectors, use these to update
                // board
                for (Sector s : sectors)
                  sudoku.replaceSector(s);
                break;
              
              //a NODE_STUCK message represents different events that lead to the same action,
              //a sector redistribution.
              //  1. nodes are honestly stuck, try redistributing them to see if any progress 
              //    can be made (unlikely)
              //  2. a node has left the system and progress in one, or more, sectors has ceased.
              //    since sectors depend on other sectors, missing sectors will cause the board to
              //    stop making progress. this case is requires fault recovery
              case NODE_STUCK:
                //when the first stuck message is received, the scheduler is paused so the system
                //can recover. the scheduler is resumed at the end of distributeSectors()
                if(EDF_ACTIVE)
                  scheduler.pause();
                
                last_stuck = System.currentTimeMillis();
                System.out.println(src_addr + " is stuck.");
                if (!responses.contains(src_addr)) {
                  responses.add(src_addr);
                }
                sectors = (LinkedList<Sector>) attachment;
                // stuck node returns state of sectors, use these to update
                // board
                for (Sector s : sectors)
                  sudoku.replaceSector(s);
                break;
              case NODE_DONE:
                last_done = System.currentTimeMillis();
                //if using EDF, remove the finished process from the scheduler
                if(EDF_ACTIVE)
                  scheduler.removeProcess(src_addr);
                
                if (!responses.contains(src_addr)) {
                  responses.add(src_addr);
                }
                
                /*
                 * The body of the message is a comma separated list containing:
                 * [0] - power usage samples
                 * [1] - current amount of power used
                 * [2] - current number of messages sent
                 * [3] - current number of sector redistributions
                 */
                args = body.split(":");
                sudoku_server.PWR_HIST += src_addr + " -- " + args[0] + "\n";
                sudoku_server.TOTAL_MSGS += Integer.parseInt(args[2]);
                sudoku_server.TOTAL_POWER += Float.parseFloat(args[1]);
                
                sectors = (LinkedList<Sector>) attachment;

                // update the servers board
                for (Sector s : sectors) {
                  sudoku.replaceSector(s);
                }

                if (sudoku.isSolved()) {
                  System.out.println("==== SOLVED ====");
                  System.out.println(sudoku.toString());
                  
                  System.out.println("Power Usage History:\n");
                  System.out.println(sudoku_server.PWR_HIST + "\n");
                  System.out.println("Total Power Used: " + sudoku_server.TOTAL_POWER + "\n");
                  System.out.println("Total Messages Passed: " + sudoku_server.TOTAL_MSGS + "\n");
                  
                  sudoku_server.PWR_HIST = "";
                  sudoku_server.TOTAL_MSGS = 0;
                  sudoku_server.TOTAL_POWER = 0;
                  
                  if(EDF_ACTIVE)
                    scheduler.stop();
                  
                  sudoku_server.stop();
                }

                break;
              case SYS_JOIN:
                //if EDF is being used, admission control decides if new nodes can join
                
                if(!EDF_ACTIVE || scheduler.addProcess(src_addr)){
                  // add an outgoing connection to the new node
                  sender.addConnection(src_addr);
  
                  // create an entry for the new node
                  sudoku_server.node_map.put(src_uid, src_addr);
  
                  // mark this node as ready
                  synchronized (sudoku_server.state_lock) {
                    sudoku_server.ready_list.add(src_addr);
                  }
  
                  outgoing = new SystemMessage<Object>(name + ": welcome to the system!",
                      SystemMessageType.SYS_JOIN_R);
                  sender.sendMessage(outgoing, src_addr);
                }
                break;

              case SYS_LEAVE:
                //if EDF is active, the process has to be removed from the system
                if(EDF_ACTIVE)
                 scheduler.removeProcess(src_addr);
                
                sudoku_server.node_map.remove(src_uid);
                try {
                  sender.removeConnection(src_addr);
                  receiver.removeConnection(src_addr);
                }
                catch (IOException e) {
                  System.out.println("Unable to end connection: " + src_addr);
                }
                break;
            }
          }
        }
      }
    }// end loop

    // stop the processor
    public void stop() {
      // notify all nodes server is dead
      for (String address : node_map.values()) {
        SystemMessage<Object> outgoing = new SystemMessage<Object>(name + ": Server Terminated",
            SystemMessageType.SYS_END);
        System.out.println("Ending: " + address);
        sender.sendMessage(outgoing, address);
      }
      // end the processor
      processing = false;

    }
  }// end class
}
