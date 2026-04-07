package com.example.educationalqualityproject.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * VCR-like utility for recording and playing back HTTP interactions.
 * This mimics the Ruby VCR gem behavior for Java tests.
 */
public class VcrHelper {

    private MockWebServer mockWebServer;
    private final String cassettePath;
    private final String cassetteName;
    private final boolean recordMode;
    private final ObjectMapper objectMapper;

    public VcrHelper(String cassettePath, String cassetteName, boolean recordMode) {
        this.cassettePath = cassettePath;
        this.cassetteName = cassetteName;
        this.recordMode = recordMode;
        this.mockWebServer = new MockWebServer();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Start the VCR helper and either load recorded responses or prepare for recording
     */
    public void start() throws IOException {
        mockWebServer.start();
        
        if (!recordMode) {
            // Playback mode: load recorded responses from cassette
            loadCassette();
        }
    }

    /**
     * Stop the VCR helper and save recordings if in record mode
     */
    public void stop() throws IOException {
        if (recordMode) {
            // In a real implementation, you would save the recorded requests/responses
            // For this educational example, we document the pattern
            saveCassette();
        }
        mockWebServer.shutdown();
    }

    /**
     * Get the URL to use for API calls (points to mock server)
     */
    public String getUrl(String path) {
        return mockWebServer.url(path).toString();
    }

    /**
     * Get an OkHttpClient configured to use the mock server
     */
    public OkHttpClient getOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Load recorded responses from cassette file
     */
    @SuppressWarnings("unused")
    private void loadCassette() throws IOException {
        Path cassetteFile = Paths.get(cassettePath, cassetteName + ".json");
        if (Files.exists(cassetteFile)) {
            String cassetteContent = Files.readString(cassetteFile);
            // Parse cassette and enqueue responses
            // This is a simplified implementation
            enqueueResponseFromCassette(cassetteContent);
        } else {
            throw new IOException("Cassette file not found: " + cassetteFile);
        }
    }

    /**
     * Save recorded interactions to cassette file
     */
    @SuppressWarnings("unused")
    private void saveCassette() throws IOException {
        Path cassetteDir = Paths.get(cassettePath);
        if (!Files.exists(cassetteDir)) {
            Files.createDirectories(cassetteDir);
        }
        
        Path cassetteFile = cassetteDir.resolve(cassetteName + ".json");
        // In a full implementation, you would serialize recorded interactions
        // For now, we document the pattern
        Files.writeString(cassetteFile, generateCassetteContent());
    }

    /**
     * Enqueue a mock response for testing
     */
    public void enqueueResponse(int statusCode, String body, String contentType) {
        MockResponse response = new MockResponse()
                .setResponseCode(statusCode)
                .setHeader("Content-Type", contentType)
                .setBody(body);
        mockWebServer.enqueue(response);
    }

    /**
     * Enqueue a JSON response
     */
    public void enqueueJsonResponse(int statusCode, String jsonBody) {
        enqueueResponse(statusCode, jsonBody, "application/json");
    }

    /**
     * Take a recorded request from the mock server
     */
    public RecordedRequest takeRequest() throws InterruptedException {
        return mockWebServer.takeRequest(5, TimeUnit.SECONDS);
    }

    private void enqueueResponseFromCassette(String cassetteContent) {
        try {
            // Parse the cassette JSON
            JsonNode cassette = objectMapper.readTree(cassetteContent);
            
            // Extract the response body from the first interaction
            JsonNode interactions = cassette.get("http_interactions");
            if (interactions != null && interactions.isArray() && interactions.size() > 0) {
                JsonNode firstInteraction = interactions.get(0);
                JsonNode response = firstInteraction.get("response");
                
                if (response != null) {
                    int statusCode = response.get("status").get("code").asInt();
                    String body = response.get("body").asText();
                    String contentType = "application/json";
                    
                    if (response.get("headers") != null && response.get("headers").get("Content-Type") != null) {
                        contentType = response.get("headers").get("Content-Type").asText();
                    }
                    
                    // Enqueue the extracted response
                    mockWebServer.enqueue(new MockResponse()
                            .setResponseCode(statusCode)
                            .setHeader("Content-Type", contentType)
                            .setBody(body));
                } else {
                    // Fallback: use entire cassette as body
                    mockWebServer.enqueue(new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody(cassetteContent));
                }
            }
        } catch (Exception e) {
            // If parsing fails, enqueue the cassette content as-is
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(cassetteContent));
        }
    }

    private String generateCassetteContent() {
        // Generate cassette content from recorded requests
        return "{}";
    }

    /**
     * Get the mock web server for advanced configurations
     */
    public MockWebServer getMockWebServer() {
        return mockWebServer;
    }
}
