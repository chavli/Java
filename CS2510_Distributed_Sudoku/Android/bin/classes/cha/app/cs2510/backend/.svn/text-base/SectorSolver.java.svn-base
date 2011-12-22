package cha.app.cs2510.backend;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Random;

import cs2510.project3.common.LookUpTable;
import cs2510.project3.common.Sector;
import cs2510.project3.common.Square;
import cs2510.project3.common.SystemMessage;
import cs2510.project3.common.SystemMessage.SystemMessageType;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import cha.app.cs2510.frontend.SudokuActivity;

public class SectorSolver implements Runnable {
  private final static String          TAG           = "SectorSolver";
  private boolean                      solving, active, cleaned;

  private Handler                      frontend;
  private MessageProcessor             message_proc;
  private Thread                       mp_thread;
  private String                       uid;

  private Random                       random;

  private LookUpTable<Integer, String> sector_map;
  //the sectors this node is in charge of solving
  private LinkedList<Sector>           sectors;                      

  private long                         last_filled;

  // a queue used to hold external sectors sent to this node as a response
  // to SUDOKU_ASK messages
  private LinkedList<Sector>           external;
  
  //address of this node
  private String                       node_address;
  
  //address of the sudoku server
  private String                       server_address;
  
  // this
  private SectorSolver                 solver;

  // statistics tracking
  private static float                 WATTAGE       = 0.0f;  //"power" used
  private static int                   MESSAGES      = 0;     //messages passed
  private static int                   REDISTS       = 0;     //sector redistributions
  
  //sampling rate of power usage
  private static long                  SAMPLING_RATE = 1000;       
  private LinkedList<String>           samples;
  private long                         last_sample   = 0;

  // power usage things
  private static float                 SEND_UPKEEP   = .5f;
  private static float                 RECV_UPKEEP   = .7f;
  private static float                 ACTIVE_UPKEEP = 2.0f;
  private static float                 IDLE_UPKEEP   = .3f;

  public SectorSolver(String client_uid, String client_addr, String server_addr, Handler h) {
    this.solver = this;
    this.uid = client_uid;
    this.node_address = client_addr;
    this.sectors = new LinkedList<Sector>();
    this.external = new LinkedList<Sector>();
    this.samples = new LinkedList<String>();
    this.random = new Random();
    this.last_filled = System.currentTimeMillis();
    this.server_address = server_addr;

    // allows communication with the frontend
    this.frontend = h;

    printToFrontend("Initializing Solver");
    this.solving = true;
    this.active = false;
    this.cleaned = false;
    
    // start the message processor used to communicate with the system
    // and other nodes
    // 1. start listening for connections
    // 2. tell the server this node is connected
    message_proc = new MessageProcessor(client_addr, server_addr);
    mp_thread = new Thread(message_proc);
    mp_thread.start();
  }

  // this is where the solving code goes
  public void run() {
    
    last_sample = System.currentTimeMillis();
    
    while (solving) {
      //Log.i(TAG, "WATTAGE: " + WATTAGE);
      
      //check if it's time to sample the power usage
      if(System.currentTimeMillis() - last_sample >= SAMPLING_RATE){
        samples.add("" + WATTAGE);
        last_sample = System.currentTimeMillis();
      }
      
      if (active) {
        WATTAGE += ACTIVE_UPKEEP * sectors.size();

        // if no progress has been made in a set time interval
        // give up
        if (System.currentTimeMillis() - this.last_filled > Utilities.TIMEOUT) {
          active = false;
          printToFrontend("==== NO PROGRESS MADE, STOPPING.====");

          // tell server node is stuck
          SystemMessage<Object> outgoing = new SystemMessage<Object>("info goes here",
              SystemMessageType.NODE_STUCK);
          outgoing.setAttachment(sectors);
          message_proc.sendMessage(outgoing, server_address);
          cleaned = false;
        }

        synchronized (sectors) {
          if (!solved()) {
            // check if any squares in local sectors can be filled in
            for (Sector sector : sectors) {
              for (Square square : sector.getSquares()) {
                if (square.getNumPossibilities() == 1) {
                  square.fill();
                  this.last_filled = System.currentTimeMillis();

                  cleanSector(sector, square.getValue());

                  printToFrontend("Placed " + square.getValue() + " @ Sector " + sector.getId()
                      + " row: " + square.getRowId() + " col: " + square.getColId());
                  // notify neighbors of this
                  for (int sid : sector.getNeighborIds())
                    notifySector(sector.getId(), sid, square.getValue(), square.getRowId(),
                        square.getColId());
                }
              }
            }

            // check if any external sectors are waiting to be processed
            synchronized (external) {
              Sector ext, loc;
              if ((ext = external.peek()) != null) {
                external.poll();
                printToFrontend("Synchronizing with external sector: " + ext.getId());
                for (int sid : ext.getNeighborIds())
                  if ((loc = getSector(sid)) != null)
                    synchronizeSectors(loc, ext);
              }
            }

            // check the neighbors of a random local square and see if any
            // more values can be cleared
            if(!sectors.isEmpty()){
              int index = random.nextInt(sectors.size());
              Sector s = sectors.get(index);
              if (!s.isSolved()) {
                printToFrontend("Polling neighbors of local sector: " + s.getId());
                for (int sid : s.getNeighborIds())
                  fetchSector(sid);
              }
            }
          }

          // this node has finished it's work, notify the server
          if (solved()) {
            active = false;
            printToFrontend("===== DONE =====");
            for (Sector sector : sectors)
              printToFrontend(sector.toString());

            // tell server node is done
            SystemMessage<Object> outgoing = new SystemMessage<Object>(samples.toString() + 
                ":" + WATTAGE + ":" + MESSAGES + ":" + REDISTS, SystemMessageType.NODE_DONE);
            outgoing.setAttachment(sectors);
            message_proc.sendMessage(outgoing, server_address);
          }

        }// end synch
      }
      // not active
      else {
        WATTAGE += IDLE_UPKEEP * sectors.size();
      }
      try {
        Thread.sleep(1000);
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /*
    SOLVER CONTROL
  */
  // end the solver
  public void stop() {
    active = false;
    solving = false;

    if (message_proc != null)
      message_proc.stop();

    WATTAGE = 0.0f;
    MESSAGES = 0;
    REDISTS = 0;
  }

  // called to begin the solving (as opposed to just starting the thread)
  private void initializeSolver() {
    REDISTS++;
    printToFrontend("Initial Local Sector Cleaning");
    // initial value removal
    synchronized (sectors) {
      for (Sector sector : sectors) {
        for (Square square : sector.getSquares())
          if (square.isFilled()) {
            cleanSector(sector, square.getValue());

            // notify other sectors that this square is filled in
            for (int sid : sector.getNeighborIds())
              notifySector(sector.getId(), sid, square.getValue(), square.getRowId(),
                  square.getColId());
          }
      }
    }

    active = true;
    printToFrontend("Solving Started");
  }

  // called to begin the solving (as opposed to just starting the thread)
  private void endSolver() {
    Message notification = frontend.obtainMessage(SudokuActivity.END);
    frontend.sendMessage(notification);
  }

  /*
    SUDOKU SOLVING FUNCTIONS
  */

  // returns true if all sectors under this solvers control are solved
  private boolean solved() {
    boolean done = true;

    if (sectors.size() == 0)
      done = false;
    else {
      synchronized (sectors) {
        for (Sector sector : sectors)
          done &= sector.isSolved();
      }
    }

    return done;
  }

  // remove possible values from squares based on a filled in value
  private void cleanSector(Sector sector, int filled) {
    if (sector != null)
      synchronized (sectors) {
        for (Square square : sector.getSquares())
          if (square.isFilled())
            square.clearPossibilities();
          else
            square.removePossibility(filled);
      }
  }

  private void cleanSectorRow(Sector sector, int row, int filled) {
    if (sector != null)
      synchronized (sectors) {
        for (Square s : sector.getRowSquares(row))
          if (!s.isFilled())
            s.removePossibility(filled);
      }
  }

  private void cleanSectorCol(Sector sector, int col, int filled) {
    if (sector != null)
      synchronized (sectors) {
        for (Square s : sector.getColumnSquares(col))
          if (!s.isFilled())
            s.removePossibility(filled);
      }
  }

  // synchronize the values of s1 with s2
  // s1 tends to be a local sectorm and s2 an external sector
  private void synchronizeSectors(Sector s1, Sector s2) {
    synchronized (sectors) {
      if (Utilities.sameSectorCol(s1.getId(), s2.getId())) {
        for (int col = 0; col < Utilities.SUDOKU_DIM; ++col)
          for (int value : s2.getColumnValues(col))
            cleanSectorCol(s1, col, value);
      }
      else if (Utilities.sameSectorRow(s1.getId(), s2.getId())) {
        for (int row = 0; row < Utilities.SUDOKU_DIM; ++row)
          for (int value : s2.getRowValues(row))
            cleanSectorRow(s1, row, value);
      }
    }
  }

  /*
    SECTOR SHARING FUNCTIONS
  */

  // notify a sector of a new number placed on the board
  private void notifySector(int src_sid, // id of the source sector
      int dest_sid, // id of the destination sector
      int value, // filled in value
      int row, // row the filled in value belongs to in the source sector
      int col) { // column the filled in value belongs to in the source sector
    String neighbor = sector_map.get(new Integer(dest_sid));

    // the sector is on the local node
    if (neighbor.equals(this.node_address)) {
      if (Utilities.sameSectorCol(src_sid, dest_sid)) {
        cleanSectorCol(getSector(dest_sid), col, value);
      }
      else if (Utilities.sameSectorRow(src_sid, dest_sid)) {
        cleanSectorRow(getSector(dest_sid), row, value);
      }
    }
    // sector is external
    else {
      SystemMessage<Object> outgoing = new SystemMessage<Object>(src_sid + "," + dest_sid + ","
          + value + "," + row + "," + col, SystemMessageType.SUDOKU_PUT);
      message_proc.sendMessage(outgoing, neighbor);
    }

  }

  // fetch a neighboring sector
  private void fetchSector(int target_sid) {
    String neighbor = sector_map.get(new Integer(target_sid));
    // neighbor is internal
    if (neighbor.equals(this.node_address)) {
      Sector s = getSector(target_sid);
      synchronized (external) {
        external.add(s);
      }
    }
    // neighbor is external
    else {
      SystemMessage<Object> outgoing = new SystemMessage<Object>("" + target_sid,
          SystemMessageType.SUDOKU_ASK);
      message_proc.sendMessage(outgoing, neighbor);
    }

  }

  // print messages to the frontend gui
  private void printToFrontend(String str) {
    Message notification = frontend.obtainMessage(SudokuActivity.PRINT);
    Bundle data = new Bundle();
    data.putString("data", str);
    notification.setData(data);
    frontend.sendMessage(notification);
  }

  private Sector getSector(int sid) {
    Sector s = null;
    synchronized (sectors) {
      for (Sector sector : sectors) {
        if (sector.getId() == sid) {
          s = sector;
          break;
        }
      }
    }
    return s;
  }

  // setters and getters
  public LinkedList<Sector> getSectors() {
    return this.sectors;
  }

  /*
    Separate daemon devoted to handling communication
  */
  public class MessageProcessor implements Runnable {
    private SenderSocket   sender;
    private ReceiverSocket receiver;
    private Thread         receiver_thread;

    private String         server_addr;
    private boolean        processing;

    public MessageProcessor(String client_addr, String server_addr) {
      processing = true;

      sender = new SenderSocket(client_addr);
      receiver = new ReceiverSocket(Utilities.PORT);

      this.server_addr = server_addr;

      // start listening
      receiver_thread = new Thread(receiver);
      receiver_thread.start();

      solver.printToFrontend("Receiver Socket Started");

      // add server to outgoing
      sender.addConnection(server_addr);
      solver.printToFrontend("Connected to: " + server_addr);

      // tell server this node has joined
      SystemMessage<Object> outgoing = new SystemMessage<Object>("", SystemMessageType.SYS_JOIN);
      outgoing.setSourceUid(uid);
      sendMessage(outgoing, server_addr);
    }

    public void sendMessage(SystemMessage<Object> outgoing, String dest) {
      WATTAGE += SEND_UPKEEP;
      MESSAGES += 1;
      sender.sendMessage(outgoing, dest);
    }

    public SystemMessage<Sector> getMessage() {
      return null;
    }

    @SuppressWarnings("unchecked")
    public void run() {
      // used to incoming messages
      SystemMessage<Object> incoming, outgoing;
      SystemMessageType type;
      String body, src_addr;
      String args[];
      Sector s;
      Object attachment;
      solver.printToFrontend("Message Processor Started");
      while (processing) {

        if (receiver != null) {
          incoming = receiver.getMessage();
          if (incoming != null) {
            WATTAGE += RECV_UPKEEP;
            type = incoming.getType();
            body = incoming.getBody();
            src_addr = incoming.getSourceAddr();
            attachment = incoming.getAttachment();

            switch (type) {
              case NODE_WAKE:
                solver.printToFrontend("==== NODE SET TO ACTIVE ====");
                if(!cleaned){
                  cleaned = true;
                  solver.initializeSolver();
                }
                else  
                  solver.active = true;
                break;
              case NODE_SLEEP:
                solver.printToFrontend("==== NODE SET TO IDLE ====");
                solver.active = false;
                break;
              case NODE_FETCH:
                outgoing = new SystemMessage<Object>(samples.toString() + ":" + WATTAGE +
                    ":" + MESSAGES + ":" + REDISTS, SystemMessageType.NODE_FETCH_R);
                outgoing.setAttachment(sectors);
                sendMessage(outgoing, server_address);                

                break;
              case SUDOKU_ASK:
                // body is a sector id
                s = getSector(Integer.parseInt(body));
                outgoing = new SystemMessage<Object>("", SystemMessageType.SUDOKU_ASK_R);
                outgoing.setAttachment(s);
                sendMessage(outgoing, src_addr);
                break;
              case SUDOKU_ASK_R:
                synchronized (external) {
                  solver.external.add((Sector) attachment);
                }
                break;
              case SUDOKU_PUT:
                /*
                 args:
                 0 - source sector
                 1 - destination sector
                 2 - filled in value in source sector
                 3 - row the filled value is in
                 4 - col the filled value is in
                */
                args = body.split(",");
                Log.i(TAG, "Received from " + src_addr + ": " + body);
                // printToFrontend("Received from " + src_addr + ": " + body);
                if (Utilities.sameSectorCol(Integer.parseInt(args[0]), Integer.parseInt(args[1]))) {
                  cleanSectorCol(getSector(Integer.parseInt(args[1])), Integer.parseInt(args[4]),
                      Integer.parseInt(args[2]));
                }
                else if (Utilities.sameSectorRow(Integer.parseInt(args[0]),
                    Integer.parseInt(args[1]))) {
                  cleanSectorRow(getSector(Integer.parseInt(args[1])), Integer.parseInt(args[3]),
                      Integer.parseInt(args[2]));
                }

                break;
              case SYS_SECMAP:
                printToFrontend("Received Sector Map.");
                solver.sector_map = (LookUpTable<Integer, String>) attachment;
                solver.external = new LinkedList<Sector>();
                solver.last_filled = System.currentTimeMillis();
                
                solver.printToFrontend(((LookUpTable<Integer, String>) attachment).toString());
                
                // establish a outgoing connection with every other node
                for (Entry<Integer, String> entry : sector_map.entrySet()) {
                  String address = entry.getValue();
                  if (!address.equals(node_address))
                    sender.addConnection(address);
                }
                
                break;
              case SYS_DIST:
                printToFrontend("Received Sectors.");
                // printToFrontend(((Sector)attachment).toString());
                synchronized (solver.sectors) {
                  solver.sectors = (LinkedList<Sector>) attachment;
                }
                break;
              case SYS_START:
                // set /reset some things
                solver.sector_map = (LookUpTable<Integer, String>) attachment;
                solver.external = new LinkedList<Sector>();
                solver.last_filled = System.currentTimeMillis();

                solver.printToFrontend(((LookUpTable<Integer, String>) attachment).toString());

                // establish a outgoing connection with every other node
                for (Entry<Integer, String> entry : sector_map.entrySet()) {
                  String address = entry.getValue();
                  if (!address.equals(node_address))
                    sender.addConnection(address);
                }

                solver.initializeSolver();
                break;
              case SYS_END:
                solver.printToFrontend(body);
                solver.endSolver();
                break;
              case SYS_JOIN_R:
                solver.printToFrontend(body);
                break;
              default:
                solver.printToFrontend(incoming.toString());
                break;

            }
          }
        }
      }
    }// end run

    public void stop() {
      // end processing
      processing = false;
      /*
      SystemMessage<Object> outgoing = new SystemMessage<Object>("", SystemMessageType.SYS_LEAVE);
      outgoing.setSourceUid(uid);
      sendMessage(outgoing, server_addr);
      */
      // end connection
      try {
        receiver.stop();

        // tell server this node is done
        SystemMessage<Object> outgoing = new SystemMessage<Object>("", SystemMessageType.SYS_LEAVE);
        outgoing.setSourceUid(uid);
        sendMessage(outgoing, server_addr);

        sender.stop();

        printToFrontend("MessageProcessor Stopped");
      }
      catch (IOException e) {
        printToFrontend("SectorSolver: Unable to end server connection.");
      }
    }
  }
}
