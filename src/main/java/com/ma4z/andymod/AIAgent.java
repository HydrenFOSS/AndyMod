package com.ma4z.andymod.ai;

import com.ma4z.andymod.config.AndyModConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class AIAgent {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    public static CompletableFuture<String> sendPromptAsync(String prompt) {
        if (!AndyModConfig.enabled.get()) {
            return CompletableFuture.completedFuture("AI generation is currently disabled via config.");
        }

        String providerType = AndyModConfig.provider.get().toLowerCase();
        String apiKeyStr = AndyModConfig.apiKey.get();
        String modelStr = AndyModConfig.model.get();

        if (apiKeyStr.isEmpty()) {
            return CompletableFuture.completedFuture("Error: API Key is empty in configuration.");
        }

        String endpoint;
        String authHeaderValue;
        String jsonPayload;

        switch (providerType) {
            case "openai":
                endpoint = "https://api.openai.com/v1/chat/completions";
                authHeaderValue = "Bearer " + apiKeyStr;
                jsonPayload = buildOpenAICompatiblePayload(modelStr, prompt);
                break;
            case "groq":
                endpoint = "https://api.groq.com/openai/v1/chat/completions";
                authHeaderValue = "Bearer " + apiKeyStr;
                jsonPayload = buildOpenAICompatiblePayload(modelStr, prompt);
                break;
            case "gemini":
                endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + modelStr + ":generateContent?key=" + apiKeyStr;
                authHeaderValue = null;
                jsonPayload = buildGeminiPayload(prompt);
                break;
            default:
                return CompletableFuture.completedFuture("Error: Unknown or unsupported provider: " + providerType);
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(AndyModConfig.timeout.get()));

        if (authHeaderValue != null) {
            requestBuilder.header("Authorization", authHeaderValue);
        }

        HttpRequest request = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonPayload)).build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        return "API Error (Status Code " + response.statusCode() + "): " + response.body();
                    }
                    return parseResponse(providerType, response.body());
                })
                .exceptionally(ex -> "Network/Timeout Exception occurred: " + ex.getMessage());
    }

    private static String buildOpenAICompatiblePayload(String model, String prompt) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("temperature", AndyModConfig.temperature.get());
        body.addProperty("max_tokens", AndyModConfig.maxTokens.get());

        JsonArray messages = new JsonArray();
        JsonObject messageItem = new JsonObject();
        messageItem.addProperty("role", "user");
        messageItem.addProperty("content", prompt);
        messages.add(messageItem);
        
        body.add("messages", messages);
        return gson.toJson(body);
    }

    private static String buildGeminiPayload(String prompt) {
        JsonObject body = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject contentItem = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject partItem = new JsonObject();
        
        partItem.addProperty("text", prompt);
        parts.add(partItem);
        contentItem.add("parts", parts);
        contents.add(contentItem);
        body.add("contents", contents);

        JsonObject config = new JsonObject();
        config.addProperty("temperature", AndyModConfig.temperature.get());
        config.addProperty("maxOutputTokens", AndyModConfig.maxTokens.get());
        body.add("generationConfig", config);

        return gson.toJson(body);
    }

    private static String parseResponse(String provider, String jsonBody) {
        try {
            JsonObject root = gson.fromJson(jsonBody, JsonObject.class);
            if (provider.equals("openai") || provider.equals("groq")) {
                return root.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
            } else if (provider.equals("gemini")) {
                return root.getAsJsonArray("candidates")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("content")
                        .getAsJsonArray("parts")
                        .get(0).getAsJsonObject()
                        .get("text").getAsString();
            }
        } catch (Exception e) {
            return "Failed to parse API structure response: " + e.getMessage();
        }
        return "Unknown error structural response.";
    }
}