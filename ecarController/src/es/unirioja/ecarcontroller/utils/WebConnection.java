package es.unirioja.ecarcontroller.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.AsyncTask;

public class WebConnection extends AsyncTask<String, Void, String> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	protected String doInBackground(String... params) {
		String data = "";

		String method = params[0];
		String request = params[1];
		String parameters = "";

		if (params.length > 2) {
			parameters = params[2];
		}

		URL url = null;
		HttpURLConnection connection = null;
		DataOutputStream dos = null;
		DataInputStream dis = null;

		try {
			url = new URL(request);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestMethod(method);

			if (method.equals("POST")) {
				connection.setRequestProperty("Content-Type",
						"application/x-www-form-urlencoded");
				connection.setRequestProperty("charset", "UTF-8");
				connection.setRequestProperty("Content-Length", ""
						+ Integer.toString(parameters.getBytes().length));

				dos = new DataOutputStream(connection.getOutputStream());
				dos.writeBytes(parameters);
				dos.flush();
				dos.close();
			}

			dis = new DataInputStream(connection.getInputStream());

			String line;

			while ((line = dis.readLine()) != null) {
				data += line;
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (dos != null)
					dos.close();
				if (dis != null)
					dis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return data;
	}
}
