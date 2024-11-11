package az.smartonecashbox.smartCassa;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class ProxyClient {

    public static String callProxy(String data, String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(data.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } else {
            throw new IOException("Proxy çağrısı ugursuz oldu. HTTP kodu: " + responseCode);
        }
    }
}
