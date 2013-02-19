package es.unirioja.ecarcontroller;

import java.util.List;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import es.unirioja.ecarcontroller.map.MapOverlay;
import es.unirioja.ecarcontroller.map.Position;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

public class MapActivity extends com.google.android.maps.MapActivity {

	private MapView map;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		initializeMap();
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

	private void initializeMap() {
		map = (MapView) findViewById(R.id.map);
		map.setBuiltInZoomControls(true);
		map.getController().setZoom(19);

		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				for (final double[] d : Position.GetPositionList()) {
					runOnUiThread(new Runnable() {
						public void run() {
							GeoPoint gp = new GeoPoint((int)(d[0]*1e6), (int)(d[1]*1e6));
							map.getController().animateTo(gp);
							
							addOverlay(gp, getResources().getDrawable(R.drawable.marker));
						}
					});
					
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		t.start();
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	private void addOverlay(GeoPoint gp, Drawable drw) {
		List<Overlay> mapOverlays = map.getOverlays();
		MapOverlay itemizedOverlay = new MapOverlay(drw);

		OverlayItem overlayItem = new OverlayItem(gp, "", "");
		itemizedOverlay.addOverlay(overlayItem);

		mapOverlays.clear();
		mapOverlays.add(itemizedOverlay);
	}
}
