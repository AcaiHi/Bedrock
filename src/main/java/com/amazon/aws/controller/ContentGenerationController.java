package com.amazon.aws.controller;

import com.amazon.aws.ClaudeContentGeneration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.util.Map;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api")
public class ContentGenerationController {

    private static final Logger logger = LoggerFactory.getLogger(ContentGenerationController.class);

    @Autowired
    private ClaudeContentGeneration contentGeneration;

    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String generateContent(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        try {
            String response = contentGeneration.generateContent(prompt);
            logger.info("Generated response: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("Error generating content", e);
            return "Error generating content: " + e.getMessage();
        }
    }

    @GetMapping("/test")
    public String test() {
        try {
            String prompt = new String(Files.readAllBytes(Paths.get("src/main/resources/sample.txt")), StandardCharsets.UTF_8);
            String response = contentGeneration.generateContent(prompt);
            logger.info("Test response: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("Error in test endpoint", e);
            return "Error in test endpoint: " + e.getMessage();
        }
    }
}
