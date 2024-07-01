package com.amazon.aws.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ResponseParser {

    public static String extractTextFromResponse(String jsonResponse) {
        try {
            JSONObject responseObj = new JSONObject(new JSONTokener(jsonResponse));
            JSONArray contentArray = responseObj.getJSONArray("content");

            StringBuilder textBuilder = new StringBuilder();

            for (int i = 0; i < contentArray.length(); i++) {
                JSONObject contentObj = contentArray.getJSONObject(i);
                if ("text".equals(contentObj.getString("type"))) {
                    textBuilder.append(contentObj.getString("text"));
                }
            }

            return textBuilder.toString();
        } catch (Exception e) {
            // Log the error and return a meaningful message
            e.printStackTrace();
            return "Error parsing response: " + e.getMessage();
        }
    }
}
