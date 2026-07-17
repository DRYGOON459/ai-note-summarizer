package com.example.demo.service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.SavedSummary;
import com.example.demo.repository.SummaryRepository;

@Service
public class SummaryService {

    private final SummaryRepository summaryRepository;

    private final PromptService promptService;
    private final AiService aiService;
    private final FileExtractionService fileExtractionService;

    public SummaryService(SummaryRepository summaryRepository, PromptService promptService, AiService aiService,
            FileExtractionService fileExtractionService) {
        this.summaryRepository = summaryRepository;
        this.promptService = promptService;
        this.aiService = aiService;
        this.fileExtractionService = fileExtractionService;
    }

    public SummaryResponse summarizeText(String text) {
        if (text == null || text.isBlank()) {
            return new SummaryResponse("No extractable text was found in the document.");
        }

        String normalizedText = prepareTextForSummarization(text);
        String prompt = promptService.buildPrompt(normalizedText, "bullets");
        try {
            String aiSummary = aiService.generate(prompt);
            return new SummaryResponse(aiSummary);
        } catch (Exception ex) {
            String fallback = summarizeLocally(normalizedText);
            return new SummaryResponse("AI summarization unavailable.\n" + fallback);
        }
    }

    public SummaryResponse summarizeFile(MultipartFile file) throws IOException {
        String extracted = fileExtractionService.extractText(file);
        if (extracted == null || extracted.isBlank()) {
            return new SummaryResponse("No readable text could be extracted from this file.");
        }
        return summarizeText(extracted);
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

    private String prepareTextForSummarization(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        int maxLength = 14000;
        if (normalized.length() > maxLength) {
            return normalized.substring(0, maxLength) + "\n\n[Text was trimmed for summarization]";
        }
        return normalized;
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

    // AI interaction moved to AiService. SummaryService coordinates prompt creation
    // and AI calls.

    public record SummaryResponse(String summary) {
    }
}
