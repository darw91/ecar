package es.unirioja.ecarcontroller;

import java.io.StringReader;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import es.unirioja.ecarcontroller.map.MapOverlay;
import es.unirioja.ecarcontroller.map.Position;
import es.unirioja.ecarcontroller.utils.WebConnection;

public class MapActivity extends com.google.android.maps.MapActivity {

	private MapView map;

	private TextView tvSpeed;
	private TextView tvPosition;
	private TextView tvBattery;

	private ImageView ivLights;

	private boolean live = false;
	private GeoPoint gp;
	private String battery;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.map);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		tvSpeed = (TextView)findViewById(R.id.tvSpeed);
		tvPosition = (TextView)findViewById(R.id.tvPosition);
		tvBattery = (TextView)findViewById(R.id.tvBattery);
		ivLights = (ImageView)findViewById(R.id.ivLights);

		initializeMap();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.map, menu);
		return true;
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
			break;
		case R.id.menu_live:
			liveMode();
			break;
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
				List<GeoPoint> listado = Position.getPositionList();

				for (int i=0; i<listado.size() && !live; i++) {
					final GeoPoint gpAnterior = i == 0 ? null : listado.get(i-1);
					final GeoPoint gp = listado.get(i);

					runOnUiThread(new Runnable() {
						public void run() {
							map.getController().animateTo(gp);

							addOverlay(gp, getResources().getDrawable(R.drawable.marker));

							if (gpAnterior != null) {
								tvSpeed.setText((int)(3.6*calculaDistancia(gpAnterior, gp)) + " km/h");
							}

							tvPosition.setText(String.format("%.5f, %.5f", gp.getLatitudeE6()/1000000.0, gp.getLongitudeE6()/1000000.0));
						}
					});

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		t.start();
	}

	private void liveMode() {
		live = true;
		map.clearAnimation();

		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				WebConnection connection = new WebConnection();
				try {
					connection.execute("GET", "http://electrosa.260mb.org/ecar/datos.xml");
					String result = connection.get();
					
					SAXReader saxReader = new SAXReader();
					Document document = saxReader.read(new StringReader(result));
					Element root = document.getRootElement();
					if (root.getName().equals("info_coche")) {
						List<Element> elements = root.elements();
						for (Element child : elements) {
							if (child.getNodeType() == Node.ELEMENT_NODE) {
								String name = child.getName();
								if (name.equals("coordenadas")) {
									int lat = (int) (Double.parseDouble(child.attributeValue("latitud")) * 1E6);
									int lon = (int) (Double.parseDouble(child.attributeValue("longitud")) * 1E6);
									gp = new GeoPoint(lat, lon);
								}
								else if (name.equals("energia")) {
									List<Element> subElements = child.elements();
									for (Element subChild : subElements) {
										if (subChild.getNodeType() == Node.ELEMENT_NODE) {
											String name2 = subChild.getName();
											if (name2.equals("carga")) {
												battery = subChild.getTextTrim() + "%";
											}
										}
									}
								}
							}
						}
					}
					
					runOnUiThread(new Runnable() {
						public void run() {
							map.getController().animateTo(gp);
							addOverlay(gp, getResources().getDrawable(R.drawable.marker));
							tvPosition.setText(String.format("%.5f, %.5f", gp.getLatitudeE6()/1000000.0, gp.getLongitudeE6()/1000000.0));
							tvBattery.setText(battery);
							tvSpeed.setText("0 km/h");
						}
					});

				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				} catch (DocumentException e) {
					e.printStackTrace();
				}
			}
		});
		t.start();
	}

	@Override
	protected boolean isRouteDisplayed() {
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

	private double calculaDistancia(GeoPoint p1, GeoPoint p2) {
		//int difLat = p2.getLatitudeE6() - p1.getLatitudeE6();
		double dlong = (p2.getLongitudeE6() - p1.getLongitudeE6()) / 1000000.0;
		double dvalue = (Math.sin(Math.toRadians(p1.getLatitudeE6() / 1000000.0)) * Math.sin(Math.toRadians(p2.getLatitudeE6() / 1000000.0)))
				+ (Math.cos(Math.toRadians(p1.getLatitudeE6() / 1000000.0)) * Math.cos(Math.toRadians(p2.getLatitudeE6() / 1000000.0)) * Math.cos(Math.toRadians(dlong)));
		double dd = Math.toDegrees(Math.acos(dvalue));

		return dd * 111302;
	}
}
