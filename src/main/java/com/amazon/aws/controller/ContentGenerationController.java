package com.amazon.aws.controller;

import com.amazon.aws.ClaudeContentGeneration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping("/api")
public class ContentGenerationController {

    private static final Logger logger = LoggerFactory.getLogger(ContentGenerationController.class);

    @Autowired
    private ClaudeContentGeneration contentGeneration;

    @PostMapping("/generate")
    public String generateContent(@RequestParam String prompt) {
        logger.info("Received prompt: {}", prompt);
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
            String prompt = "hello";
            String response = contentGeneration.generateContent(prompt);
            logger.info("Test response: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("Error in test endpoint", e);
            return "Error in test endpoint: " + e.getMessage();
        }
    }
}
