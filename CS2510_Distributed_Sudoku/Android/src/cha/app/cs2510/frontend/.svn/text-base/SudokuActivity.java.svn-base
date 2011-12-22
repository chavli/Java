package cha.app.cs2510.frontend;

import cs2510.project3.common.Sector;
import cs2510.project3.common.Square;
import cha.app.cs2510.R;
import cha.app.cs2510.backend.SectorSolver;
import cha.app.cs2510.backend.Utilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

public class SudokuActivity extends Activity implements OnClickListener {
	private final static String TAG = "SudokuActivity";
	/*declare gui elements*/
	private ScrollView scv_output;
	private TextView txv_output;
  private EditText txf_sectornum;
  private Button btn_sectorok;
  
  private AlertDialog sector_alert;
	
	//server address
	private String server_addr;
	
	//the sector solver is the backend logic
	private SectorSolver solver;
	private Thread solver_thread;
	
	public static final int PRINT = 0;
	public static final int END = 1;
	
	/* State Notes 
     protected void onCreate(Bundle savedInstanceState);
     protected void onStart();
     protected void onRestart();
     protected void onResume();
     protected void onPause();
     protected void onStop();
     protected void onDestroy();
	*/
	
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.sudoku);
    
    Bundle args = getIntent().getExtras();
    server_addr = args.getString("address");
    
    //initiate gui references
    scv_output = (ScrollView)findViewById(R.id.scv_output);
    txv_output = (TextView)findViewById(R.id.txv_output);
    
    //configure gui elements
    scv_output.setVerticalScrollBarEnabled(false);
  }
  
  @Override
  public void onStart(){
    super.onStart();
   
    //initialize the sector solver
    setTitle("Node: " + Utilities.getSystemIP(this));
    solver = new SectorSolver(this.toString(), Utilities.getSystemIP(this), server_addr, handler);
    solver_thread = new Thread(solver);
    solver_thread.start();
    Log.i(TAG, "start");
  }
  
  protected void onStop(){
    super.onStop();
  }
  
  @Override
  public void onDestroy(){
    super.onDestroy();
    solver.stop();
    Log.i(TAG, "destroy");
  }
  
  private boolean updateScreen(String str){
    if(txv_output != null){
      txv_output.setText(txv_output.getText() + str + "\n");
      scv_output.smoothScrollBy(0, 100);
      return true;
    }
    return false;
  }
  
  //inter-thread message handler
  private final Handler handler = new Handler(){
    private Bundle bundle;
    
    public void handleMessage(Message msg){
      bundle = msg.getData();
      switch(msg.what){
        case PRINT:
          updateScreen(bundle.getString("data"));
          break;
        case END:
          solver.stop();
          finish();
          break;          
      }
    }
  };
  
  //Menu button stuff
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu, menu);
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
      // Handle item selection
      switch (item.getItemId()) {
      case R.id.menu_sector:
          for(Sector s : solver.getSectors())
            updateScreen(s.toString());
          return true;
      case R.id.menu_square:
        final CharSequence[] items = new CharSequence[solver.getSectors().size()];
        int i = 0;
        for(Sector s : solver.getSectors())
          items[i++] = "" + s.getId();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sector");
        builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
              Sector s = solver.getSectors().get(item);
              for(Square sq : s.getSquares())
                updateScreen(sq.toString());
              sector_alert.cancel();
            }
        });
        sector_alert = builder.create();
        sector_alert.show();
        return true;
      default:
        return super.onOptionsItemSelected(item);

      }
  }

  public void onClick(View v) {
    if(v.equals(btn_sectorok)){
      int sector = Integer.parseInt(txf_sectornum.getText().toString());
      for(Sector s : solver.getSectors()){
        if(s.getId() == sector){
          for(Square sq : s.getSquares())
            updateScreen(sq.toString());
          break;
        }
      }
    }
  }
}    

