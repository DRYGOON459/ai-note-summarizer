package com.example.demo.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
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
      extension = getExtensionFromContentType(file.getContentType());
    }
    if (extension == null) {
      throw new IllegalArgumentException("Unable to determine file type");
    }

    return switch (extension.toLowerCase()) {
      case "txt", "md", "csv", "log", "json", "xml", "rtf" -> extractTextFromText(file);
      case "pdf" -> extractTextFromPdf(file);
      case "docx" -> extractTextFromDocx(file);
      case "doc" -> extractTextFromDoc(file);
      case "xlsx", "xls" -> extractTextFromSpreadsheet(file);
      case "pptx", "ppt" -> extractTextFromPresentation(file);
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

  private String extractTextFromDoc(MultipartFile file) throws IOException {
    try (InputStream input = file.getInputStream(); HWPFDocument document = new HWPFDocument(input);
        WordExtractor extractor = new WordExtractor(document)) {
      return extractor.getText().trim();
    }
  }

  private String extractTextFromSpreadsheet(MultipartFile file) throws IOException {
    try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
      List<String> lines = new ArrayList<>();
      for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
        Sheet sheet = workbook.getSheetAt(sheetIndex);
        for (Row row : sheet) {
          List<String> values = new ArrayList<>();
          for (Cell cell : row) {
            values.add(safeCellValue(cell));
          }
          lines.add(String.join(" | ", values));
        }
      }
      return String.join(System.lineSeparator(), lines).trim();
    }
  }

  private String extractTextFromPresentation(MultipartFile file) throws IOException {
    try (InputStream input = file.getInputStream(); XMLSlideShow slideshow = new XMLSlideShow(input)) {
      List<String> slides = new ArrayList<>();
      for (XSLFSlide slide : slideshow.getSlides()) {
        List<String> slideLines = new ArrayList<>();
        slide.getShapes().forEach(shape -> {
          if (shape instanceof org.apache.poi.xslf.usermodel.XSLFTextShape textShape) {
            String text = textShape.getText();
            if (text != null && !text.isBlank()) {
              slideLines.add(text.trim());
            }
          }
        });
        if (!slideLines.isEmpty()) {
          slides.add(String.join(System.lineSeparator(), slideLines));
        }
      }
      return String.join(System.lineSeparator(), slides).trim();
    }
  }

  private String extractTextFromImage(MultipartFile file) throws IOException {
    BufferedImage image = ImageIO.read(file.getInputStream());
    if (image == null) {
      throw new IllegalArgumentException("Uploaded image file could not be read");
    }

    try {
      ITesseract ocr = createTesseractEngine();
      String text = ocr.doOCR(image);
      return text == null ? "" : text.trim();
    } catch (TesseractException ex) {
      log.warn("OCR extraction failed for image upload", ex);
      throw new IOException("Unable to extract text from image. Make sure Tesseract is installed and available.", ex);
    }
  }

  private ITesseract createTesseractEngine() {
    Tesseract tesseract = new Tesseract();
    String dataPath = findTesseractDataPath();
    if (dataPath != null) {
      tesseract.setDatapath(dataPath);
    }
    String executablePath = findTesseractExecutablePath();
    if (executablePath != null) {
      tesseract.setDatapath(executablePath.substring(0, executablePath.lastIndexOf(java.io.File.separator)));
    }
    tesseract.setLanguage("eng");
    return tesseract;
  }

  private String findTesseractDataPath() {
    String[] candidates = {
        System.getenv("TESSDATA_PREFIX"),
        System.getenv("TESSERACT_DATA_PATH"),
        "C:\\Program Files\\Tesseract-OCR\\tessdata",
        "C:\\Program Files (x86)\\Tesseract-OCR\\tessdata",
        System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Tesseract-OCR\\tessdata",
        System.getProperty("user.home") + "\\AppData\\Local\\Tesseract-OCR\\tessdata"
    };
    for (String candidate : candidates) {
      if (candidate != null && !candidate.isBlank()) {
        java.io.File file = new java.io.File(candidate);
        if (file.exists()) {
          return candidate;
        }
      }
    }
    return null;
  }

  private String findTesseractExecutablePath() {
    String[] candidates = {
        System.getenv("TESSERACT_PATH"),
        "C:\\Program Files\\Tesseract-OCR\\tesseract.exe",
        "C:\\Program Files (x86)\\Tesseract-OCR\\tesseract.exe",
        System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Tesseract-OCR\\tesseract.exe",
        System.getProperty("user.home") + "\\AppData\\Local\\Tesseract-OCR\\tesseract.exe"
    };
    for (String candidate : candidates) {
      if (candidate != null && !candidate.isBlank()) {
        java.io.File file = new java.io.File(candidate);
        if (file.exists()) {
          return candidate;
        }
      }
    }
    return null;
  }

  private String safeCellValue(Cell cell) {
    if (cell == null) {
      return "";
    }
    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue();
      case NUMERIC -> Double.toString(cell.getNumericCellValue());
      case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
      case FORMULA -> cell.getCellFormula();
      default -> "";
    };
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

  private String getExtensionFromContentType(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      return null;
    }
    return switch (contentType.toLowerCase()) {
      case "text/plain" -> "txt";
      case "text/markdown" -> "md";
      case "application/pdf" -> "pdf";
      case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx";
      case "application/msword" -> "doc";
      case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "pptx";
      case "application/vnd.ms-powerpoint" -> "ppt";
      case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx";
      case "application/vnd.ms-excel" -> "xls";
      case "image/png" -> "png";
      case "image/jpeg" -> "jpg";
      case "image/jpg" -> "jpg";
      case "image/bmp" -> "bmp";
      case "image/gif" -> "gif";
      default -> null;
    };
  }
}
