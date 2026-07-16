package com.example.demo.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@Service
public class FileExtractionService {

  private static final Logger log = LoggerFactory.getLogger(FileExtractionService.class);

  public String extractText(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Uploaded file is empty");
    }

    String extension = getExtension(file.getOriginalFilename());
    if (extension == null) {
      throw new IllegalArgumentException("Unable to determine file type");
    }

    return switch (extension.toLowerCase()) {
      case "txt", "md", "csv", "log" -> extractTextFromText(file);
      case "pdf" -> extractTextFromPdf(file);
      case "docx" -> extractTextFromDocx(file);
      case "png", "jpg", "jpeg", "bmp", "gif", "tiff", "tif" -> extractTextFromImage(file);
      default -> throw new IllegalArgumentException("Unsupported file type: " + extension);
    };
  }

  private String extractTextFromText(MultipartFile file) throws IOException {
    byte[] bytes = file.getBytes();
    if (bytes.length == 0) {
      return "";
    }
    return new String(bytes, StandardCharsets.UTF_8).trim();
  }

  private String extractTextFromPdf(MultipartFile file) throws IOException {
    try (InputStream input = file.getInputStream(); PDDocument document = PDDocument.load(input)) {
      PDFTextStripper stripper = new PDFTextStripper();
      return stripper.getText(document).trim();
    }
  }

  private String extractTextFromDocx(MultipartFile file) throws IOException {
    try (InputStream input = file.getInputStream(); XWPFDocument document = new XWPFDocument(input);
        XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
      return extractor.getText().trim();
    }
  }

  private String extractTextFromImage(MultipartFile file) throws IOException {
    BufferedImage image = ImageIO.read(file.getInputStream());
    if (image == null) {
      throw new IllegalArgumentException("Uploaded image file could not be read");
    }

    try {
      ITesseract ocr = new Tesseract();
      String text = ocr.doOCR(image);
      return text == null ? "" : text.trim();
    } catch (TesseractException ex) {
      log.warn("OCR extraction failed for image upload", ex);
      throw new IOException("Unable to extract text from image. Make sure Tesseract is installed and available.", ex);
    }
  }

  private String getExtension(String filename) {
    if (filename == null || filename.isBlank()) {
      return null;
    }
    int dotIndex = filename.lastIndexOf('.');
    if (dotIndex < 0 || dotIndex == filename.length() - 1) {
      return null;
    }
    return filename.substring(dotIndex + 1);
  }
}
