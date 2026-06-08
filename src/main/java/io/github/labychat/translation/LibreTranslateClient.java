package io.github.labychat.translation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.labychat.LabyChatClient;
import io.github.labychat.config.ConfigManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/** Optional service. Disabled until the user explicitly supplies an endpoint. */
public final class LibreTranslateClient {
    private static final Gson GSON = new Gson();
    private final ConfigManager configManager;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public LibreTranslateClient(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public CompletableFuture<String> translate(String text) {
        var config = configManager.get();
        if (!config.translationEnabled || config.libreTranslateEndpoint.isBlank()) {
            return CompletableFuture.completedFuture(text);
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("q", text);
        payload.addProperty("source", "auto");
        payload.addProperty("target", config.translateTargetLanguage);
        payload.addProperty("format", "text");
        if (!config.libreTranslateApiKey.isBlank()) payload.addProperty("api_key", config.libreTranslateApiKey);

        String endpoint = config.libreTranslateEndpoint.replaceAll("/+$", "") + "/translate";
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload), StandardCharsets.UTF_8))
                .build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) return text;
                    JsonObject object = GSON.fromJson(response.body(), JsonObject.class);
                    return object.has("translatedText") ? object.get("translatedText").getAsString() : text;
                })
                .exceptionally(exception -> {
                    LabyChatClient.LOGGER.debug("Translation request failed", exception);
                    return text;
                });
    }
}
