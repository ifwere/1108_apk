package com.aichoice.assistant.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Fetches the chinese-days holiday snapshot. Network access only happens when
 * the user taps the refresh button in settings.
 */
public final class HolidayRemoteClient {

    public static final String DATA_URL =
            "https://cdn.jsdelivr.net/npm/chinese-days/dist/chinese-days.json";

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final int MAX_BYTES = 2 * 1024 * 1024;

    private HolidayRemoteClient() {
    }

    public static String fetch() throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(DATA_URL).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Accept-Encoding", "identity");

            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + code);
            }

            try (InputStream input = connection.getInputStream()) {
                ByteArrayOutputStream output = new ByteArrayOutputStream(64 * 1024);
                byte[] buffer = new byte[8192];
                int read;
                int total = 0;
                while ((read = input.read(buffer)) != -1) {
                    total += read;
                    if (total > MAX_BYTES) {
                        throw new IOException("response exceeds " + MAX_BYTES + " bytes");
                    }
                    output.write(buffer, 0, read);
                }
                String body = output.toString(StandardCharsets.UTF_8.name()).trim();
                if (body.isEmpty()) {
                    throw new IOException("empty response");
                }
                if (!body.startsWith("{")) {
                    throw new IOException("unexpected response format");
                }
                return body;
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
