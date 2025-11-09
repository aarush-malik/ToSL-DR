package com.tosldr.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Handles HTTP calls to the Hugging Face Inference API
 * using the DistilBART ToS summarizer model.
 */
public class HuggingFaceClient {

    // DistilBART ToS model endpoint
    private static final String API_URL =
        "https://router.huggingface.co/hf-inference/models/ml6team/distilbart-tos-summarizer-tosdr";

    private final HttpClient httpClient;
    private final String apiToken;

    public HuggingFaceClient(String apiToken) {
        this.httpClient = HttpClient.newHttpClient();
        this.apiToken = apiToken;
    }

    public String summarize(String text) throws IOException, InterruptedException {
        if (apiToken == null || apiToken.isBlank()) {
            throw new IllegalStateException("Hugging Face API token is not set.");
        }

        JSONObject body = new JSONObject();
        body.put("inputs", text);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + apiToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("HF API error: " + response.statusCode() + " | " + response.body());
        }

        // Response is usually: [ { "summary_text": "..." } ]
        JSONArray arr = new JSONArray(response.body());
        if (arr.isEmpty()) {
            throw new IOException("Empty response from HF API");
        }

        JSONObject first = arr.getJSONObject(0);
        return first.optString("summary_text", "No summary returned.");
    }
}