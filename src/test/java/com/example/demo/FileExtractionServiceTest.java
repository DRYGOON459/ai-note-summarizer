package com.example.demo;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import com.example.demo.service.FileExtractionService;
import com.example.demo.service.SummaryService;

@SpringBootTest
class FileExtractionServiceTest {

    @Autowired
    private FileExtractionService fileExtractionService;

    @Autowired
    private SummaryService summaryService;

    @Test
    void extractTextFromTextFileReturnsOriginalContent() throws Exception {
        var file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "Alpha line. Beta line. Gamma line.".getBytes(StandardCharsets.UTF_8));

        String extracted = fileExtractionService.extractText(file);

        assertThat(extracted).contains("Alpha line").contains("Beta line").contains("Gamma line");
    }

    @Test
    void summarizeFileReturnsFallbackWhenTextIsProvided() throws Exception {
        var file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "First topic sentence. Second topic sentence. Third topic sentence.".getBytes(StandardCharsets.UTF_8));

        SummaryService.SummaryResponse response = summaryService.summarizeFile(file);

        assertThat(response.summary()).isNotBlank();
    }
}
