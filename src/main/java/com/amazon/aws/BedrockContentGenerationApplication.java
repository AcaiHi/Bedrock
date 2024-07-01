package com.amazon.aws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.amazon.aws")
public class BedrockContentGenerationApplication {

    public static void main(String[] args) {
        SpringApplication.run(BedrockContentGenerationApplication.class, args);
    }
}
