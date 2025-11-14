package com.tosldr.services;

public class HuggingFaceTest {

    public static void main(String[] args) throws Exception {
        String token = System.getenv("HF_TOKEN");

        if (token == null || token.isBlank()) {
            System.out.println("❌ HF token is empty.");
            return;
        }

        String sample = "By using our service you agree that data may be shared with third parties and subject to binding arbitration.";

        HuggingFaceClient client = new HuggingFaceClient(token);
        String summary = client.summarize(sample);

        System.out.println("Status: 200 (simulated — remove debug if needed)");
        System.out.println("Summary:\n" + summary);
    }
}