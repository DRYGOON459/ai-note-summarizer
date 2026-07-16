package com.example.demo.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Responsible for communicating with the AI provider (Gemini).
 * Single responsibility: make HTTP request/response mapping.
 */
@Service
public class AiService {

  @Value("${gemini.api.key:}")
  private String apiKey;

  @Value("${gemini.model:gemini-3.5-pro}")
  private String model;

  @Value("${gemini.endpoint:https://gemini.googleapis.com/v1/models/%s:generateText?key=%s}")
  private String endpointTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newHttpClient();

  /**
   * Send the prompt to the AI and return the generated text.
   */
  public String generate(String prompt) throws Exception {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("No Gemini API key configured");
    }

    String endpoint = String.format(endpointTemplate, model, apiKey);
    Map<String, Object> payload = Map.of(
        "prompt", Map.of("text", prompt),
        "temperature", 0.2,
        "maxOutputTokens", 512);

    String requestBody = objectMapper.writeValueAsString(payload);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(endpoint))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IllegalStateException("AI service returned status " + response.statusCode() + ": " + response.body());
    }

    JsonNode json = objectMapper.readTree(response.body());
    JsonNode candidate = json.path("candidates").path(0);
    if (!candidate.isMissingNode()) {
      JsonNode output = candidate.path("output");
      if (output.isTextual()) {
        return output.asText().trim();
      }
      JsonNode content = candidate.path("content");
      if (content.isArray() && content.size() > 0) {
        return content.get(0).path("text").asText().trim();
      }
    }

    JsonNode output = json.path("output");
    if (output.isTextual()) {
      return output.asText().trim();
    }

    return response.body();
  }
}
