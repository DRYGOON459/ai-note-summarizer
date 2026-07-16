package com.example.demo.service;

import org.springframework.stereotype.Service;

/**
 * Builds prompts for the AI model. Single responsibility: only prompt
 * construction.
 */
@Service
public class PromptService {

  /**
   * Build a prompt for the given text and style.
   * 
   * @param text  the input text to summarize
   * @param style the summary style (e.g., "bullets", "paragraph")
   * @return the full prompt to send to the AI
   */
  public String buildPrompt(String text, String style) {
    if (text == null)
      text = "";
    String normalized = (style == null) ? "" : style.toLowerCase().trim();

    String base;
    switch (normalized) {
      case "bullets", "bullet", "bulletpoints", "bullet points" ->
        base = "Summarize the following text into clear, concise bullet points. Use 4 to 8 bullets and keep the main ideas. Do not add extra commentary.\n\n";
      case "paragraph", "paragraphs" ->
        base = "Write a concise paragraph summary of the following text. Keep it clear and focused.\n\n";
      default ->
        base = "Summarize the following text:\n\n";
    }

    return base + text;
  }
}
