package src.services;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class AIService {

    // 🔑 Replace this with your actual Gemini API key
    private static final String API_KEY = "AIzaSyDuXWPW3hb5PEdV-0UW1UtaGyBhrZpN9MI";
    private static final String API_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;

    public static String getAISuggestions(String promptText) {
        try {
            // Prepare HTTP request
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            // Gemini expects this JSON format
            String jsonInput = "{\n" +
                               "  \"contents\": [\n" +
                               "    {\n" +
                               "      \"parts\": [\n" +
                               "        { \"text\": \"%s\" }\n" +
                               "      ]\n" +
                               "    }\n" +
                               "  ]\n" +
                               "}";
            jsonInput = String.format(jsonInput, promptText.replace("\"", "\\\""));

            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonInput.getBytes());
            }

            // Read response
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            String rawResponse = response.toString();
            System.out.println("Raw AI response: " + rawResponse); // Debug print
            return extractTextFromGeminiResponse(rawResponse);

        } catch (Exception e) {
            return "Failed to get AI suggestion: " + e.getMessage();
        }
    }

    // Basic JSON parsing to extract the text content
    private static String extractTextFromGeminiResponse(String jsonResponse) {
        // Look for the "text": "..." pattern
        String searchText = "\"text\": \"";
        int textStartIndex = jsonResponse.indexOf(searchText);

        if (textStartIndex == -1) {
            return "Could not parse AI response (no text found).";
        }

        textStartIndex += searchText.length();

        // Find the end of the text content (the next unescaped double quote)
        int textEndIndex = textStartIndex;
        while (textEndIndex < jsonResponse.length()) {
            if (jsonResponse.charAt(textEndIndex) == '\"' && (textEndIndex == 0 || jsonResponse.charAt(textEndIndex - 1) != '\\')) {
                break;
            }
            textEndIndex++;
        }

        if (textEndIndex >= jsonResponse.length()) {
             return "Could not parse AI response (text end not found).";
        }

        String extractedText = jsonResponse.substring(textStartIndex, textEndIndex);

        // Basic unescaping of common characters (like newline and escaped quotes)
        extractedText = extractedText.replace("\\n", "\n");
        extractedText = extractedText.replace("\\\\", "\\"); // Unescape escaped backslashes
        extractedText = extractedText.replace("\\\"", "\""); // Unescape escaped quotes

        return extractedText;
    }
}
