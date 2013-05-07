package es.unirioja.ecarcontroller;

import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.VideoView;
import android.app.Activity;
import android.content.Intent;

public class ControlActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.control);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		VideoView videoView = (VideoView) findViewById(R.id.video);

		Uri path = Uri.parse("android.resource://es.unirioja.ecarcontroller/" + R.raw.coche);

		videoView.setVideoURI(path);
		videoView.start();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This is called when the Home (Up) button is pressed
			// in the Action Bar.
			Intent parentActivityIntent = new Intent(this, MainActivity.class);
			parentActivityIntent.addFlags(
					Intent.FLAG_ACTIVITY_CLEAR_TOP |
					Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(parentActivityIntent);
			finish();
			
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
}
