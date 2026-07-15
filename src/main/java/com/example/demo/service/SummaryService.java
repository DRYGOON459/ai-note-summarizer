package com.example.demo.service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.demo.model.SavedSummary;
import com.example.demo.repository.SummaryRepository;

@Service
public class SummaryService {

    private final SummaryRepository summaryRepository;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

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

        return new SummaryResponse("AI summary would be generated here using model " + model + " for input: "
                + text.substring(0, Math.min(text.length(), 80)) + "...");
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
        return "Key points: " + String.join(" ", sentences[0].trim(), sentences.length > 1 ? sentences[1].trim() : "")
                .replaceAll("\\s+", " ").trim();
    }

    public record SummaryResponse(String summary) {
    }
}
