package es.unirioja.sendposition;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

public class HomeActivity extends Activity {
	
	private Button btConnect;
	private Button btDisconnect;
	
	private ToggleButton tbPosicion;
	private ToggleButton tbCortas;
	private ToggleButton tbLargas;
	private ToggleButton tbIzquierdo;
	private ToggleButton tbDerecho;
	
	private EditText etIP;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		
		btConnect = (Button)findViewById(R.id.btConnect);
		btDisconnect = (Button)findViewById(R.id.btDisconnect);
		etIP = (EditText)findViewById(R.id.etIP);
		
		tbPosicion = (ToggleButton)findViewById(R.id.tbPosicion);
		tbPosicion.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				UpdateService.posicion = tbPosicion.isChecked();
			}
		});
		
		tbCortas = (ToggleButton)findViewById(R.id.tbCortas);
		tbCortas.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				UpdateService.cortas = tbCortas.isChecked();
			}
		});
		
		tbLargas = (ToggleButton)findViewById(R.id.tbLargas);
		tbLargas.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				UpdateService.largas = tbLargas.isChecked();
			}
		});
		
		tbIzquierdo = (ToggleButton)findViewById(R.id.tbIzquierdo);
		tbIzquierdo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				UpdateService.izquierda = tbIzquierdo.isChecked();
			}
		});
		
		tbDerecho = (ToggleButton)findViewById(R.id.tbDerecho);
		tbDerecho.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				UpdateService.derecha = tbDerecho.isChecked();
			}
		});
		
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
		Intent serviceIntent = new Intent(this, UpdateService.class);
		serviceIntent.putExtra("ip", etIP.getText().toString());
		startService(serviceIntent);
		btConnect.setEnabled(false);
		btDisconnect.setEnabled(true);
		etIP.setEnabled(false);
	}
	
	private void handleDisconnectPressed() {
		Intent serviceIntent = new Intent(this, UpdateService.class);
		stopService(serviceIntent);
		btDisconnect.setEnabled(false);
		btConnect.setEnabled(true);
		etIP.setEnabled(true);
	}
}
