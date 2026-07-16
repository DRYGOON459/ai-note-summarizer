package com.example.demo.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.demo.model.SavedSummary;
import com.example.demo.repository.SummaryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SummaryService {

    private final SummaryRepository summaryRepository;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.5-pro}")
    private String model;

    @Value("${gemini.endpoint:https://gemini.googleapis.com/v1/models/%s:generateText?key=%s}")
    private String endpointTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public SummaryService(SummaryRepository summaryRepository) {
        this.summaryRepository = summaryRepository;
    }

    public SummaryResponse summarizeText(String text) {
        if (text == null || text.isBlank()) {
            return new SummaryResponse("Please provide some notes to summarize.");
        }

        if (apiKey == null || apiKey.isBlank()) {
            String fallback = summarizeLocally(text);
            return new SummaryResponse(fallback);
        }

        try {
            String prompt = buildBulletPointPrompt(text);
            String aiSummary = callAiModel(prompt);
            return new SummaryResponse(aiSummary);
        } catch (Exception ex) {
            String fallback = summarizeLocally(text);
            return new SummaryResponse("AI summarization unavailable.\n" + fallback);
        }
    }

    public SummaryResponse summarizeWordFile(org.springframework.web.multipart.MultipartFile file) throws IOException {
        String extracted = extractTextFromDocx(file);
        return summarizeText(extracted);
    }

    private String extractTextFromDocx(org.springframework.web.multipart.MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream());
                XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    public SavedSummary saveSummary(String title, String originalText, String summary) {
        SavedSummary savedSummary = new SavedSummary();
        savedSummary.setTitle(title);
        savedSummary.setOriginalText(originalText);
        savedSummary.setSummary(summary);
        savedSummary.setCreatedAt(Instant.now());
        return summaryRepository.save(savedSummary);
    }

    public List<SavedSummary> getAllSummaries() {
        return summaryRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public Optional<SavedSummary> updateSummary(Long id, String title, String originalText, String summary) {
        Optional<SavedSummary> opt = summaryRepository.findById(id);
        if (opt.isEmpty())
            return Optional.empty();
        SavedSummary s = opt.get();
        s.setTitle(title);
        s.setOriginalText(originalText);
        s.setSummary(summary);
        s.setCreatedAt(Instant.now());
        return Optional.of(summaryRepository.save(s));
    }

    public void deleteSummary(Long id) {
        summaryRepository.deleteById(id);
    }

    private String summarizeLocally(String text) {
        String[] sentences = text.split("(?<=[.!?])\\s+");
        if (sentences.length == 0) {
            return "Summary unavailable.";
        }

        int bulletCount = Math.min(5, sentences.length);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bulletCount; i++) {
            String line = sentences[i].trim().replaceAll("\\s+", " ");
            if (!line.isEmpty()) {
                builder.append("- ").append(line);
                if (i < bulletCount - 1) {
                    builder.append(System.lineSeparator());
                }
            }
        }
        return builder.toString();
    }

    private String buildBulletPointPrompt(String text) {
        return "Summarize the following text into clear, concise bullet points. "
                + "Use 4 to 8 bullets and keep the main ideas. Do not add extra commentary.\n\n" + text;
    }

    private String callAiModel(String prompt) throws Exception {
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
            throw new IllegalStateException(
                    "AI service returned status " + response.statusCode() + ": " + response.body());
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

    public record SummaryResponse(String summary) {
    }
}
