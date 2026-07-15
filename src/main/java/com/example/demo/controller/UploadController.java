package com.example.demo.controller;

import java.io.IOException;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.example.demo.service.SummaryService;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

  private final SummaryService summaryService;

  public UploadController(SummaryService summaryService) {
    this.summaryService = summaryService;
  }

  @PostMapping("/word")
  public org.springframework.http.ResponseEntity<?> uploadWord(
      @org.springframework.web.bind.annotation.RequestParam("file") MultipartFile file) {
    try {
      SummaryService.SummaryResponse resp = summaryService.summarizeWordFile(file);
      return org.springframework.http.ResponseEntity.ok(resp);
    } catch (org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException ex) {
      return org.springframework.http.ResponseEntity.badRequest().body("Uploaded file is not a valid .docx file");
    } catch (Exception ex) {
      return org.springframework.http.ResponseEntity.status(500).body("Failed to process uploaded file");
    }
  }
}
