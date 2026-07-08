package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping
    public SavedSummary save(@RequestBody @Valid SaveRequest request) {
        return summaryService.saveSummary(request.title(), request.originalText(), request.summary());
    }

    @GetMapping
    public List<SavedSummary> list() {
        return summaryService.getAllSummaries();
    }

    public record SummaryRequest(@NotBlank String text) {
    }

    public record SaveRequest(@NotBlank String title, @NotBlank String originalText, @NotBlank String summary) {
    }
}
