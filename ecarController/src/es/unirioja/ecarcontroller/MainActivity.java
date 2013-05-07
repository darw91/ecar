package es.unirioja.ecarcontroller;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Button btMonitorizar = (Button) findViewById(R.id.btMonitorizar);
        btMonitorizar.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getApplicationContext(), MapActivity.class);
				startActivity(i);
			}
		});
        
        Button btControlar = (Button) findViewById(R.id.btControlar);
        btControlar.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getApplicationContext(), ControlActivity.class);
				startActivity(i);
			}
		});
    }    
}
