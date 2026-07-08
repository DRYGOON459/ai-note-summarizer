package com.example.demo;

import com.example.demo.model.SavedSummary;
import com.example.demo.repository.SummaryRepository;
import com.example.demo.service.SummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SummaryServiceTest {

    @Autowired
    private SummaryService summaryService;

    @Autowired
    private SummaryRepository summaryRepository;

    @BeforeEach
    void setUp() {
        summaryRepository.deleteAll();
    }

    @Test
    void summarizeTextReturnsFallbackSummaryWhenNoApiKeyIsConfigured() {
        var response = summaryService.summarizeText("First topic sentence. Second topic sentence. Third topic sentence.");

        assertThat(response.summary()).isNotBlank();
        assertThat(response.summary()).contains("First topic sentence");
    }

    @Test
    void saveSummaryPersistsEntry() {
        SavedSummary saved = summaryService.saveSummary("Study Notes", "Original content", "A short summary");

        assertThat(saved.getId()).isNotNull();
        assertThat(summaryRepository.findAll()).hasSize(1);
    }
}
