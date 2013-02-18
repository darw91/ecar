package es.unirioja.ecarcontroller;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

import android.content.Intent;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.MenuItem;

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
		map.getController().animateTo(searchLocationByName("Universidad de La Rioja, Logroño, España"));
		map.getController().setZoom(18);
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	private GeoPoint searchLocationByName(String locationName) {
		Geocoder geoCoder = new Geocoder(this, Locale.getDefault());
		GeoPoint gp = null;
		try {
			List<android.location.Address> addresses = geoCoder.getFromLocationName(locationName, 1);
			for (android.location.Address address : addresses) {
				gp = new GeoPoint((int)(address.getLatitude() * 1E6), (int)(address.getLongitude() * 1E6));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return gp;
	}

}
