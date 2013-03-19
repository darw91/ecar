package es.unirioja.sendposition;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.widget.Toast;

public class SendPositionApplication extends Application {
	
	private static SendPositionApplication instance;
	
	private static final int ACTION_SHOW_TOAST = 0;
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ACTION_SHOW_TOAST:
				Toast.makeText(SendPositionApplication.this, msg.getData().getString("message"), Toast.LENGTH_LONG).show();
				break;
			}
		}
	};
	
	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;
	}
	
	public static SendPositionApplication getInstance() {
		return instance;
	}
	
	public boolean checkConstraints() {
		if (!areLocationsEnabled()) {
			showToast(getResources().getString(R.string.error_locations));
			return false;
		}
		if (!isInternetAvailable()) {
			showToast(getResources().getString(R.string.error_internet));
			return false;
		}
		return true;
	}
	
	private boolean areLocationsEnabled() {
		String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
		if (provider != null && provider.length() > 0)
			return true;
		else
			return false;
	}

	private boolean isInternetAvailable() {
		ConnectivityManager manager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();
		if (info == null || info.getState().equals(State.DISCONNECTED))
			return false;
		return true;
	}
	
	public void showToast(String message) {
		Bundle bundle = new Bundle();
		bundle.putString("message", message);
		Message msg = handler.obtainMessage();
		msg.setData(bundle);
		handler.sendMessage(msg);
	}
}
