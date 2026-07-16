package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  @PostMapping
  public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
    try {
      SummaryService.SummaryResponse resp = summaryService.summarizeFile(file);
      return ResponseEntity.ok(resp);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().body(ex.getMessage());
    } catch (Exception ex) {
      return ResponseEntity.status(500).body("Failed to process uploaded file: " + ex.getMessage());
    }
  }
}
