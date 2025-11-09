package com.tosldr.services;

public class HuggingFaceTest {

    public static void main(String[] args) throws Exception {
        // TEMP: put your new HF token here just to test.
        // After confirming it works, DELETE this from code.
        String token = "hf_NEW_TOKEN";

        if (token == null || token.isBlank()) {
            System.out.println("❌ HF token is empty.");
            return;
        }

        String sample = "By using our service you agree that data may be shared with third parties and subject to binding arbitration.";

        HuggingFaceClient client = new HuggingFaceClient(token);
        String summary = client.summarize(sample);

        System.out.println("✅ API call succeeded!");
        System.out.println("Summary:\n" + summary);
    }
}