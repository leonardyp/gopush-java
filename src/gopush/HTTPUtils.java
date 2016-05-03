package gopush;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class HTTPUtils {

	public static String get(String url) throws IOException {
		assert url != null && url.trim().length() != 0;

		HttpURLConnection huc = null;
		try {
			huc = getHttpURLConnection(url, "GET");
			BufferedReader reader = new BufferedReader(new InputStreamReader(huc.getInputStream()));
			StringBuilder result = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				result.append(line);
				result.append("\r\n");
			}

			return result.toString();
		} finally {
			if (huc != null) {
				huc.disconnect();
			}
		}
	}

	public static String post(String url, String data) throws IOException {
		assert url != null && url.trim().length() != 0;

		HttpURLConnection huc = null;
		try {
			huc = getHttpURLConnection(url, "POST");

			huc.setDoOutput(true);
			huc.setDoInput(true);
			huc.setRequestMethod("POST");
			huc.setUseCaches(false);
			huc.setInstanceFollowRedirects(true);
			huc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			huc.connect();

			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(huc.getOutputStream(), "UTF-8"));
			writer.write(data);
			writer.flush();
			writer.close();

			BufferedReader reader = new BufferedReader(new InputStreamReader(huc.getInputStream()));
			StringBuilder result = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				result.append(line);
				result.append("\r\n");
			}

			return result.toString();
		} finally {
			if (huc != null) {
				huc.disconnect();
			}
		}
	}

	private static HttpURLConnection getHttpURLConnection(String url, String method) throws IOException {
		HttpURLConnection huc = (HttpURLConnection) new URL(url).openConnection();

		huc.setRequestMethod(method);
		huc.setUseCaches(false);
		huc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		huc.setRequestProperty("Charset", "UTF-8");

		return huc;
	}

	public static void main(String[] args) throws IOException {
		logutil.debug(get("http://localhost:8090/server/get?key=Terry-Mao&proto=2"));
	}
}
