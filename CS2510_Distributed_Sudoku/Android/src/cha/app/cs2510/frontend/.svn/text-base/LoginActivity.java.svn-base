package cha.app.cs2510.frontend;

import cha.app.cs2510.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends Activity implements OnClickListener {
  
  //gui elements
  private EditText txf_address;
  private Button btn_login;
  
  public void onCreate(Bundle state){
    super.onCreate(state);
    setContentView(R.layout.login);
    
    //initialize gui components
    btn_login = (Button) findViewById(R.id.login_button);
    txf_address = (EditText) findViewById(R.id.login_address);

    //configure gui elements
    btn_login.setOnClickListener(this);
  }
  
  public void onStart(){
    super.onStart();
  }

  public void onDestroy(){
    super.onDestroy();
  }
  
  public void onClick(View v) {
    if(v.equals(btn_login)){
      String address = txf_address.getText().toString();
      Intent i = new Intent(this, SudokuActivity.class);
      i.putExtra("address", address);
      startActivity(i);
    }
  }
}
