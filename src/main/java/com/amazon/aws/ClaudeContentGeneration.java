package com.amazon.aws;

import java.nio.charset.Charset;
import org.json.JSONObject;
import com.amazon.aws.util.BedrockRequestBody;
import io.github.cdimascio.dotenv.Dotenv;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        this.bedrockClient = BedrockRuntimeClient.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
            .build();
    }

    public String generateContent(String prompt) {
        try {
            String bedrockBody = BedrockRequestBody.builder()
                .withModelId(this.modelName)
                .withPrompt(prompt)
                .withInferenceParameter("max_tokens_to_sample", 2048)
                .withInferenceParameter("temperature", 0.5)
                .withInferenceParameter("top_k", 250)
                .withInferenceParameter("top_p", 1)
                .build();

            InvokeModelRequest invokeModelRequest = InvokeModelRequest.builder()
                .modelId(this.modelName)
                .body(SdkBytes.fromString(bedrockBody, Charset.defaultCharset()))
                .build();
            logger.info("Invoking Bedrock model with request: {}", invokeModelRequest);
            InvokeModelResponse invokeModelResponse = bedrockClient.invokeModel(invokeModelRequest);
            logger.info("Received response from Bedrock model: {}", invokeModelResponse);
            JSONObject responseAsJson = new JSONObject(invokeModelResponse.body().asUtf8String());
            logger.info("Response from Bedrock model: {}", responseAsJson.getString("completion"));
            
            return responseAsJson.getString("completion");
        } catch (Exception e) {
            logger.error("Error invoking Bedrock model", e);
            throw new RuntimeException("Error invoking Bedrock model: " + e.getMessage(), e);
        }
    }
}
