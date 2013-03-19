package es.unirioja.sendposition;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class HomeActivity extends Activity {
	
	private Button btConnect;
	private Button btDisconnect;
	
	private EditText etIP;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);
		
		btConnect = (Button)findViewById(R.id.btConnect);
		btDisconnect = (Button)findViewById(R.id.btDisconnect);
		etIP = (EditText)findViewById(R.id.etIP);
		
		btDisconnect.setEnabled(false);
		
		btConnect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleConnectPressed();
			}
		});
		
		btDisconnect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleDisconnectPressed();
			}
		});
	}
	
	private void handleConnectPressed() {
		Intent serviceIntent = new Intent(this, CurrentLocationService.class);
		serviceIntent.putExtra("ip", etIP.getText().toString());
		startService(serviceIntent);
		btConnect.setEnabled(false);
		btDisconnect.setEnabled(true);
		etIP.setEnabled(false);
	}
	
	private void handleDisconnectPressed() {
		Intent serviceIntent = new Intent(this, CurrentLocationService.class);
		stopService(serviceIntent);
		btDisconnect.setEnabled(false);
		btConnect.setEnabled(true);
		etIP.setEnabled(true);
	}
}
