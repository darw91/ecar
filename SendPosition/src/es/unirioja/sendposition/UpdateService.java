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

import org.xmlpull.v1.XmlSerializer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
	private Location locationAnt;

	private boolean gpsEnabled = false;
	private boolean networkEnabled = false;

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

	public static boolean posicion = false;
	public static boolean cortas = false;
	public static boolean largas = false;
	public static boolean izquierda = false;
	public static boolean derecha = false;
	
	private boolean sigue = true;

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
		sigue = true;
		
		try {
			url = new URL(intent.getExtras().getString("ip"));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				while (sigue) {
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
					
					try {
						Thread.sleep(SECONDS_UPDATE);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				Looper.loop();
			}
		}).start();

		return super.onStartCommand(intent, flags, startId);
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onDestroy()
	 */
	public void onDestroy() {
		super.onDestroy();	
		locationManager.removeUpdates(this);
		sigue = false;
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
		this.locationAnt = this.location;
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
			if (sigue) {
				HttpURLConnection httpCon = null;
				DataOutputStream dos = null;

				try {
					httpCon = (HttpURLConnection) url.openConnection();
					httpCon.setDoOutput(true);
					httpCon.setDoInput(true);
					httpCon.setRequestMethod("POST");

					String parameters = "xml=" + URLEncoder.encode(generateXML(), "UTF-8");

					httpCon.setConnectTimeout(SECONDS_UPDATE * 950);
					httpCon.setReadTimeout(SECONDS_UPDATE * 950);
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
			}
			return null;
		}
	}

	private String generateXML() {
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();
		try {
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);
			serializer.startTag("", INFO_TAG);
			serializer.attribute("", DATE_ATT, new Date().toString());

			// Coordenadas
			serializer.startTag("", COORD_TAG);
			serializer.attribute("", LAT_ATT, String.valueOf(location.getLatitude()));
			serializer.attribute("", LON_ATT, String.valueOf(location.getLongitude()));
			serializer.attribute("", ALT_ATT, String.valueOf(location.getAltitude()));
			serializer.endTag("", COORD_TAG);

			// Movimiento
			serializer.startTag("", MOV_TAG);
			serializer.startTag("", SPE_TAG);
			serializer.text((int)(3.6*calculaDistancia(location, locationAnt)) + "");
			serializer.endTag("", SPE_TAG);
			serializer.startTag("", ODO_TAG);
			serializer.text("");
			serializer.endTag("", ODO_TAG);
			serializer.startTag("", DIR_TAG);
			serializer.text("");
			serializer.endTag("", DIR_TAG);
			serializer.endTag("", MOV_TAG);

			// Energ�a
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
			serializer.attribute("", POS_ATT, "" + posicion);
			serializer.attribute("", DIP_ATT, "" + cortas);
			serializer.attribute("", BEA_ATT, "" + largas);
			serializer.endTag("", ILU_TAG);
			serializer.startTag("", FOG_TAG);
			serializer.attribute("", FRO_ATT, "");
			serializer.attribute("", REA_ATT, "");
			serializer.endTag("", FOG_TAG);
			serializer.startTag("", IND_TAG);
			serializer.attribute("", RIG_ATT, "" + derecha);
			serializer.attribute("", LEF_ATT, "" + izquierda);
			serializer.endTag("", IND_TAG);
			serializer.endTag("", LIG_TAG);

			serializer.endTag("", INFO_TAG);
			serializer.endDocument();

			this.locationAnt = this.location;
			return writer.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private double calculaDistancia(Location p1, Location p2) {
		if (p1 == null || p2 == null)
			return 0;
		double dlong = (p2.getLongitude() - p1.getLongitude());
		double dvalue = (Math.sin(Math.toRadians(p1.getLatitude())) * Math.sin(Math.toRadians(p2.getLatitude())))
				+ (Math.cos(Math.toRadians(p1.getLatitude())) * Math.cos(Math.toRadians(p2.getLatitude())) * Math.cos(Math.toRadians(dlong)));
		double dd = Math.toDegrees(Math.acos(dvalue));

		return dd * 111302;
	}
}
