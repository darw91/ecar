package es.unirioja.sendposition;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.xmlpull.v1.XmlSerializer;

import com.google.android.maps.GeoPoint;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Xml;

public class UpdateService extends Service implements LocationListener {

	public final static int SECONDS_UPDATE = 1;

	private LocationManager locationManager;

	private Location location;

	private boolean gpsEnabled = false;
	private boolean networkEnabled = false;

	private Timer timer = null;
	
	private URL url;
	
	private static final String INFO_TAG = "info_coche";
	private static final String DATE_ATT = "date";
	private static final String COORD_TAG = "coordenadas";
	private static final String LAT_ATT = "latitud";
	private static final String LON_ATT = "longitud";
	private static final String MOV_TAG= "movimiento";
	private static final String SPE_TAG = "velocidad";
	private static final String ODO_TAG = "cuentakilometros";
	private static final String DIR_TAG = "direccion";
	private static final String ALT_ATT = "altitud";
	private static final String ENER_TAG = "energia";
	private static final String CHA_TAG = "carga";
	private static final String POT_TAG = "potencia";
	private static final String INT_TAG = "intensidad";
	private static final String CON_TAG = "consumo";
	private static final String VOL_TAG = "voltaje";
	private static final String LIG_TAG = "luces";
	private static final String ILU_TAG = "iluminacion";
	private static final String POS_ATT = "posicion";
	private static final String DIP_ATT = "cortas";
	private static final String BEA_ATT = "largas";
	private static final String FOG_TAG = "antinieblas";
	private static final String FRO_ATT = "delanteras";
	private static final String REA_ATT = "traseras";
	private static final String IND_TAG = "intermitentes";
	private static final String RIG_ATT = "derecho";
	private static final String LEF_ATT = "izquierdo";
	
	private int nivelBateria;
	
	private BroadcastReceiver bateria = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			nivelBateria = intent.getIntExtra("level", 0);
		}
	};

	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onCreate()
	 */
	public void onCreate() {
		super.onCreate();
		locationManager = (LocationManager)getSystemService(Service.LOCATION_SERVICE);
		this.registerReceiver(this.bateria, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		initializeLocationListeners();
	}

	/**
	 * Initializes location providers.
	 */
	private void initializeLocationListeners() {
		try {
			gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch (Exception ex) {
			// Ignore.
		}
		try {
			networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		} catch (Exception ex) {
			// Ignore.
		}
		// Don't start listeners if no provider is enabled
		if (!gpsEnabled && !networkEnabled)
			return;
		if (gpsEnabled)
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this, Looper.myLooper());
		if (networkEnabled)
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this, Looper.myLooper());
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	public int onStartCommand(Intent intent, int flags, int startId) {
		try {
			url = new URL(intent.getExtras().getString("ip"));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		if (timer != null)
			timer.cancel();
		timer = new Timer(true);
		timer.schedule(createTimerTask(), 1000, SECONDS_UPDATE * 1000);

		return super.onStartCommand(intent, flags, startId);
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onDestroy()
	 */
	public void onDestroy() {
		super.onDestroy();
		timer.cancel();
		timer.purge();
		locationManager.removeUpdates(this);
	}

	/**
	 * Instances a new timerTask to update location to iDigi.
	 * 
	 * @return The new timer task.
	 */
	private TimerTask createTimerTask() {
		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				// Check for internet connection and location services.
				if (!networkEnabled) {
					stopSelf();
					return;
				}
				if (!gpsEnabled) {
					stopSelf();
					return;
				}

				location = getLastKnownLocation();
				updatePosition(location);
			}
		};
		return task;
	}

	/**
	 * Retrieves the last known location. This method is usually used the first time
	 * when there is not yet any available location.
	 * 
	 * @return The last known location.
	 */
	private Location getLastKnownLocation() {
		Location networkLocation = null;
		Location gpsLocation = null;
		if (gpsEnabled)
			gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (networkEnabled)
			networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		// If there are both values use the latest one
		if (gpsLocation != null && networkLocation != null) {
			if (gpsLocation.getTime() > networkLocation.getTime())
				return gpsLocation;
			else
				return networkLocation;
		}

		if (gpsLocation != null)
			return gpsLocation;
		if (networkLocation != null)
			return networkLocation;
		return null;
	}

	public void updatePosition(Location location) {
		new UploadDataTask().execute(location);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onLocationChanged(Location location) {
		this.location = location;
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
	
	class UploadDataTask extends AsyncTask<Location, Void, Void> {
		@Override
		protected Void doInBackground(Location... params) {
			HttpURLConnection httpCon = null;
			DataOutputStream dos = null;
			
			try {
				httpCon = (HttpURLConnection) url.openConnection();
				httpCon.setDoOutput(true);
				httpCon.setDoInput(true);
				httpCon.setRequestMethod("POST");
				
				String parameters = "xml=" + URLEncoder.encode(generateXML(params[0]), "UTF-8");

				httpCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
				httpCon.setRequestProperty("charset", "UTF-8");
				httpCon.setRequestProperty("Content-Length", "" + Integer.toString(parameters.getBytes().length));
	
				dos = new DataOutputStream(httpCon.getOutputStream());
				dos.writeBytes(parameters);
				dos.flush();
				
				new DataInputStream(httpCon.getInputStream());
				
				httpCon.disconnect();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	private String generateXML(Location loc) {
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();
		try {
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);
			serializer.startTag("", INFO_TAG);
			serializer.attribute("", DATE_ATT, new Date().toString());
			
			// Coordenadas
			serializer.startTag("", COORD_TAG);
			serializer.attribute("", LAT_ATT, String.valueOf(loc.getLatitude()));
			serializer.attribute("", LON_ATT, String.valueOf(loc.getLongitude()));
			serializer.attribute("", ALT_ATT, String.valueOf(loc.getAltitude()));
			serializer.endTag("", COORD_TAG);
			
			// Movimiento
			serializer.startTag("", MOV_TAG);
			serializer.startTag("", SPE_TAG);
			serializer.text("");
			serializer.endTag("", SPE_TAG);
			serializer.startTag("", ODO_TAG);
			serializer.text("");
			serializer.endTag("", ODO_TAG);
			serializer.startTag("", DIR_TAG);
			serializer.text("");
			serializer.endTag("", DIR_TAG);
			serializer.endTag("", MOV_TAG);
			
			// Energía
			serializer.startTag("", ENER_TAG);
			serializer.startTag("", CHA_TAG);
			serializer.text(nivelBateria + "");
			serializer.endTag("", CHA_TAG);
			serializer.startTag("", POT_TAG);
			serializer.text("");
			serializer.endTag("", POT_TAG);
			serializer.startTag("", INT_TAG);
			serializer.text("");
			serializer.endTag("", INT_TAG);
			serializer.startTag("", CON_TAG);
			serializer.text("");
			serializer.endTag("", CON_TAG);
			serializer.startTag("", VOL_TAG);
			serializer.text("");
			serializer.endTag("", VOL_TAG);
			serializer.endTag("", ENER_TAG);
			
			// Luces
			serializer.startTag("", LIG_TAG);
			serializer.startTag("", ILU_TAG);
			serializer.attribute("", POS_ATT, "");
			serializer.attribute("", DIP_ATT, "");
			serializer.attribute("", BEA_ATT, "");
			serializer.endTag("", ILU_TAG);
			serializer.startTag("", FOG_TAG);
			serializer.attribute("", FRO_ATT, "");
			serializer.attribute("", REA_ATT, "");
			serializer.endTag("", FOG_TAG);
			serializer.startTag("", IND_TAG);
			serializer.attribute("", RIG_ATT, "");
			serializer.attribute("", LEF_ATT, "");
			serializer.endTag("", IND_TAG);
			serializer.endTag("", LIG_TAG);
			
			serializer.endTag("", INFO_TAG);
			serializer.endDocument();
			return writer.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
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
