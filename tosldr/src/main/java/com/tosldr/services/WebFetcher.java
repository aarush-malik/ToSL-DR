package com.tosldr.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebFetcher {

    public static String fetch(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn;

        int maxRedirects = 5;

        while (true) {
            conn = (HttpURLConnection) url.openConnection();

            // Pretend to be a browser (VERY important!)
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/119.0 Safari/537.36");

            conn.setInstanceFollowRedirects(false);
            conn.connect();

            int status = conn.getResponseCode();

            // Handle redirects
            if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == HttpURLConnection.HTTP_MOVED_PERM ||
                status == HttpURLConnection.HTTP_SEE_OTHER) {

                if (maxRedirects-- == 0)
                    throw new Exception("Too many redirects");

                String newUrl = conn.getHeaderField("Location");

                // Handle relative redirects
                if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                    newUrl = url.getProtocol() + "://" + url.getHost() + newUrl;
                }

                url = new URL(newUrl);
                continue;
            }

            // Stop redirect loop → break
            break;
        }

        if (conn.getResponseCode() != 200) {
            throw new Exception("HTTP " + conn.getResponseCode());
        }

        BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
        );

        StringBuilder content = new StringBuilder();
        String line;

        while ((line = in.readLine()) != null) {
            content.append(line).append("\n");
        }

        in.close();
        return content.toString();
    }
}