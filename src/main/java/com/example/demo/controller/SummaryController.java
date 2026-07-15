package com.example.demo.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.SavedSummary;
import com.example.demo.service.SummaryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/summaries")
public class SummaryController {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @PostMapping("/summarize")
    public SummaryService.SummaryResponse summarize(@RequestBody @Valid SummaryRequest request) {
        return summaryService.summarizeText(request.text());
    }

    @PostMapping("/upload-word")
    public SummaryService.SummaryResponse uploadWord(
            @org.springframework.web.bind.annotation.RequestParam("file") MultipartFile file) throws IOException {
        return summaryService.summarizeWordFile(file);
    }

    @PostMapping
    public SavedSummary save(@RequestBody @Valid SaveRequest request) {
        return summaryService.saveSummary(request.title(), request.originalText(), request.summary());
    }

    @GetMapping
    public List<SavedSummary> list() {
        return summaryService.getAllSummaries();
    }

    @PutMapping("/{id}")
    public SavedSummary update(
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody @Valid UpdateRequest request) {
        return summaryService.updateSummary(id, request.title(), request.originalText(), request.summary())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public void delete(@org.springframework.web.bind.annotation.PathVariable Long id) {
        summaryService.deleteSummary(id);
    }

    public record SummaryRequest(@NotBlank String text) {
    }

    public record SaveRequest(@NotBlank String title, @NotBlank String originalText, @NotBlank String summary) {
    }

    public record UpdateRequest(@NotBlank String title, @NotBlank String originalText, @NotBlank String summary) {
    }
}
