package com.amazon.aws;

import com.amazon.aws.util.BedrockRequestBody;
import com.amazon.aws.util.ResponseParser;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ClaudeContentGeneration {

    private static final Logger logger = LoggerFactory.getLogger(ClaudeContentGeneration.class);

    private final BedrockRuntimeClient bedrockClient;
    private final String modelName;

    public ClaudeContentGeneration() {
        Dotenv dotenv = Dotenv.load();

        String accessKeyId = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretAccessKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String region = dotenv.get("AWS_REGION");
        this.modelName = dotenv.get("MODEL_NAME");

        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        
        // Configure HTTP client with infinite timeout
        SdkHttpClient httpClient = ApacheHttpClient.builder().maxConnections(10000).build();

        this.bedrockClient = BedrockRuntimeClient.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
            .httpClient(httpClient)
            .build();
    }

    public String generateContent(String prompt) {
        try {
            // 讀取檔案內容
            String fileContent = new String(Files.readAllBytes(Paths.get("src/main/resources/prompt.txt")), StandardCharsets.UTF_8);
            String prePrompt = new String(Files.readAllBytes(Paths.get("src/main/resources/preprompt.txt")), StandardCharsets.UTF_8);

            // 分段處理
            List<String> segments = splitIntoSegments(prompt, 20);
            List<String> summaries = new ArrayList<>();

            for (String segment : segments) {
                // 刪除包含 "衛教指導" 的段落直到下一個時間段
                segment = removeEducationSections(segment);
                
                // 移除所有空格和換行符號
                segment = removeAllWhitespace(segment);
                
                // 組合 preprompt 和 segment
                String completePrompt = prePrompt + segment;
                
                // 計算提示字元數並記錄長度
                int promptLength = completePrompt.length();
                logger.info("Prompt length: " + promptLength);
                
                String summary = generateSummary(completePrompt);
                summaries.add(summary);
            }

            // 組合所有摘要與 prompt.txt
            String finalPrompt = String.join("", summaries) + fileContent;
            saveToTmpFile(finalPrompt);
            
            // 生成最終內容
            String finalContent = "摘要檢視：\n"+generateSummary(finalPrompt)+"\n詳細日期摘要：\n"+String.join("", summaries);

            return finalContent;
        } catch (Exception e) {
            logger.error("Error invoking Bedrock model", e);
            throw new RuntimeException("Error invoking Bedrock model: " + e.getMessage(), e);
        }
    }

    private List<String> splitIntoSegments(String input, int segmentSize) {
        List<String> segments = new ArrayList<>();
        String[] parts = input.split("(?=\\d{4}-\\d{2}-\\d{2}\t\\d{2}：\\d{2}\t)");
        StringBuilder segment = new StringBuilder();
        
        for (int i = 0; i < parts.length; i++) {
            segment.append(parts[i]);
            if ((i + 1) % segmentSize == 0 || i == parts.length - 1) {
                segments.add(segment.toString());
                segment = new StringBuilder();
            }
        }
        return segments;
    }

    private String removeEducationSections(String input) {
        // Regular expression pattern to identify date and time sections
        String dateTimePattern = "\\d{4}-\\d{2}-\\d{2}\t\\d{2}：\\d{2}\t";

        // Pattern to match "衛教指導" and everything until the next date and time section
        Pattern pattern = Pattern.compile("衛教指導.*?(?=" + dateTimePattern + ")", Pattern.DOTALL);

        // Remove the matched sections
        Matcher matcher = pattern.matcher(input);
        return matcher.replaceAll("");
    }

    private String removeAllWhitespace(String input) {
        // Replace all spaces and newlines with empty string
        return input.replaceAll("\\s+", "");
    }

    private void saveToTmpFile(String content) throws IOException {
        String tmpFilePath = "D:\\Project\\amazon-bedrock-with-builder-and-command-patterns\\tmp\\processed_prompt.txt";
        try (FileWriter writer = new FileWriter(tmpFilePath)) {
            writer.write(content);
        }
    }

    private String generateSummary(String completePrompt) throws Exception {
        String bedrockBody = BedrockRequestBody.builder()
            .withModelId(this.modelName)
            .withPrompt(completePrompt)
            .withInferenceParameter("max_tokens_to_sample", 2048)
            .withInferenceParameter("temperature", 0.5)
            .withInferenceParameter("top_k", 250)
            .withInferenceParameter("top_p", 1)
            .withRole("user")
            .withContentType("application/json")
            .withAccept("application/json")
            .build();

        InvokeModelRequest invokeModelRequest = InvokeModelRequest.builder()
            .modelId(this.modelName)
            .body(SdkBytes.fromString(bedrockBody, Charset.defaultCharset()))
            .contentType("application/json")  // Ensure contentType is set
            .accept("application/json")  // Ensure accept is set
            .build();
        InvokeModelResponse invokeModelResponse = bedrockClient.invokeModel(invokeModelRequest);

        // Convert the InvokeModelResponse body to a string
        String responseBody = invokeModelResponse.body().asString(Charset.defaultCharset());

        // Extract text from the JSON response
        return ResponseParser.extractTextFromResponse(responseBody);
    }
}
