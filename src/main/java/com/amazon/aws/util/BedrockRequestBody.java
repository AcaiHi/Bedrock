package com.amazon.aws.util;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import org.json.JSONObject;

import software.amazon.awssdk.annotations.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BedrockRequestBody {

  private BedrockRequestBody() {
  }

  public static BedrockRequestBodyBuilder builder() {
      return new BedrockRequestBodyBuilder();
  }

  public static class BedrockRequestBodyBuilder {

      @NotNull private String modelId;
      @NotNull private String prompt;
      private Map<String, Object> inferenceParameters = new HashMap<>();
      private String system;
      private String role;
      private String contentType;
      private String accept;

      public BedrockRequestBodyBuilder withModelId(String modelId) {
          this.modelId = modelId;
          return this;
      }

      public BedrockRequestBodyBuilder withPrompt(String prompt) {
          this.prompt = prompt;
          return this;
      }

      public BedrockRequestBodyBuilder withInferenceParameter(String paramName, Object paramValue) {
          if (inferenceParameters == null) {
              inferenceParameters = new HashMap<>();
          }
          inferenceParameters.put(paramName, paramValue);
          return this;
      }

      public BedrockRequestBodyBuilder withSystem(String system) {
          this.system = system;
          return this;
      }

      public BedrockRequestBodyBuilder withRole(String role) {
          // Validate the role to ensure it matches the expected values
          if (!"system".equals(role) && !"user".equals(role) && !"assistant".equals(role)) {
              throw new IllegalArgumentException("'role' must be one of 'system', 'user', or 'assistant'");
          }
          this.role = role;
          return this;
      }

      public BedrockRequestBodyBuilder withContentType(String contentType) {
          this.contentType = contentType;
          return this;
      }

      public BedrockRequestBodyBuilder withAccept(String accept) {
          this.accept = accept;
          return this;
      }

      public String build() {
          if (modelId == null) {
              throw new IllegalArgumentException("'modelId' is a required parameter");
          }
          if (prompt == null) {
              throw new IllegalArgumentException("'prompt' is a required parameter");
          }
          if (role == null) {
              throw new IllegalArgumentException("'role' is a required parameter");
          }
          if (contentType == null) {
              throw new IllegalArgumentException("'contentType' is a required parameter");
          }
          if (accept == null) {
              throw new IllegalArgumentException("'accept' is a required parameter");
          }

          BedrockBodyCommand bedrockBodyCommand = null;
          switch (modelId) {
              case "amazon.titan-tg1-large":
              case "amazon.titan-text-express-v1":
                  bedrockBodyCommand = new AmazonTitanCommand(prompt, inferenceParameters, system, role);
                  break;
              case "ai21.j2-mid-v1":
              case "ai21.j2-ultra-v1":
                  bedrockBodyCommand = new AI21LabsCommand(prompt, inferenceParameters, system, role);
                  break;
              case "anthropic.claude-instant-v1":
              case "anthropic.claude-v1":
              case "anthropic.claude-v2":
              case "anthropic.claude-3-sonnet-20240229-v1:0":
              case "anthropic.claude-3-haiku-20240307-v1:0":
                  bedrockBodyCommand = new AnthropicCommand(prompt, inferenceParameters, system, role, contentType, accept);
                  break;
              case "cohere.command-text-v14":
                  bedrockBodyCommand = new CohereCommand(prompt, inferenceParameters, system, role);
                  break;
              case "stability.stable-diffusion-xl-v0":
                  bedrockBodyCommand = new StabilityAICommand(prompt, inferenceParameters, system, role);
                  break;
              default:
                  throw new IllegalArgumentException("Unsupported modelId: " + modelId);
          }
          return bedrockBodyCommand.execute();
      }
  }
}

abstract class BedrockBodyCommand {

  protected String prompt;
  protected Map<String, Object> inferenceParameters;
  protected String system;
  protected String role;
  protected String contentType;
  protected String accept;

  public BedrockBodyCommand(String prompt, Map<String, Object> inferenceParameters, String system, String role, String contentType, String accept) {
      this.prompt = prompt;
      this.inferenceParameters = inferenceParameters;
      this.system = system;
      this.role = role;
      this.contentType = contentType;
      this.accept = accept;
  }

  public BedrockBodyCommand(String prompt, Map<String, Object> inferenceParameters, String system, String role) {
      this(prompt, inferenceParameters, system, role, "application/json", "application/json");
  }

  protected void updateMap(Map<String, Object> existingMap, Map<String, Object> newEntries) {
      newEntries.forEach((newEntryKey, newEntryValue) -> {
          updateMap(existingMap, newEntryKey, newEntryValue);
      });
  }

  protected void updateMap(Map<String, Object> existingMap, String key, Object newValue) {
      if (existingMap.containsKey(key)) {
          existingMap.put(key, newValue);
      } else {
          existingMap.values().forEach(existingValue -> {
              if (existingValue instanceof Map) {
                  @SuppressWarnings("unchecked")
                  var valueAsMap = (Map<String, Object>) existingValue;
                  updateMap(valueAsMap, key, newValue);
              }
          });
      }
  }

  public abstract String execute();
}

class AmazonTitanCommand extends BedrockBodyCommand {

    public AmazonTitanCommand(String prompt, Map<String, Object> inferenceParameters, String system, String role) {
        super(prompt, inferenceParameters, system, role);
    }

    @Override
    public String execute() {

        final Map<String, Object> textGenerationConfig = new HashMap<>(4);

        textGenerationConfig.put("maxTokenCount", 512);
        textGenerationConfig.put("stopSequences", new String[] {});
        textGenerationConfig.put("temperature", 0);
        textGenerationConfig.put("topP", 0.9f);

        final Map<String, Object> jsonMap = new HashMap<>(2);

        jsonMap.put("inputText", this.prompt);
        jsonMap.put("textGenerationConfig", textGenerationConfig);

        if (this.inferenceParameters != null && !this.inferenceParameters.isEmpty()) {
            updateMap(jsonMap, inferenceParameters);
        }
        return new JSONObject(jsonMap).toString();
    }

}

class AI21LabsCommand extends BedrockBodyCommand {

    public AI21LabsCommand(String prompt, Map<String, Object> inferenceParameters, String system, String role) {
        super(prompt, inferenceParameters, system, role);
    }

    @Override
    public String execute() {

        final Map<String, Object> countPenalty = Map.of("scale", 0);
        final Map<String, Object> presencePenalty = Map.of("scale", 0);
        final Map<String, Object> frequencyPenalty = Map.of("scale", 0);
        final Map<String, Object> jsonMap = new HashMap<>(8);

        jsonMap.put("prompt", this.prompt);
        jsonMap.put("maxTokens", 200);
        jsonMap.put("temperature", 0.7);
        jsonMap.put("topP", 1);
        jsonMap.put("stopSequences", new String[] {});
        jsonMap.put("countPenalty", countPenalty);
        jsonMap.put("presencePenalty", presencePenalty);
        jsonMap.put("frequencyPenalty", frequencyPenalty);

        if (this.inferenceParameters != null && !this.inferenceParameters.isEmpty()) {
            updateMap(jsonMap, inferenceParameters);
        }
        return new JSONObject(jsonMap).toString();
    }

}

class AnthropicCommand extends BedrockBodyCommand {

  private static final Logger logger = LoggerFactory.getLogger(BedrockBodyCommand.class);

  public AnthropicCommand(String prompt, Map<String, Object> inferenceParameters, String system, String role, String contentType, String accept) {
      super(prompt, inferenceParameters, system, role, contentType, accept);
  }

  @Override
  public String execute() {
      if (this.prompt == null || this.role == null) {
          throw new IllegalArgumentException("'prompt' and 'role' are required parameters");
      }

      final Map<String, Object> jsonMap = new HashMap<>(4);

      jsonMap.put("anthropic_version", "bedrock-2023-05-31");
      jsonMap.put("max_tokens", 1000);

      // Creating a content structure as required
      // Map<String, Object> imageContent = Map.of(
      //     "type", "image",
      //     "source", Map.of(
      //         "type", "base64",
      //         "media_type", "image/jpeg",
      //         "data", "iVBORw..."
      //     )
      // );

      Map<String, Object> textContent = Map.of(
          "type", "text",
          "text", this.prompt
      );

      Map<String, Object> messageContent = Map.of(
          "role", this.role,
          "content", List.of(textContent)
      );

      jsonMap.put("messages", List.of(messageContent));

      logger.info("Anthropic command: {}", new JSONObject(jsonMap).toString());

      if (this.inferenceParameters != null && !this.inferenceParameters.isEmpty()) {
          updateMap(jsonMap, inferenceParameters);
      }

      return new JSONObject(jsonMap).toString();
  }
}

class CohereCommand extends BedrockBodyCommand {

    public CohereCommand(String prompt, Map<String, Object> inferenceParameters, String system, String role) {
        super(prompt, inferenceParameters, system, role);
    }

    @Override
    public String execute() {

        Map<String, Object> jsonMap = new HashMap<>(7);

        jsonMap.put("prompt", this.prompt);
        jsonMap.put("max_tokens", 400);
        jsonMap.put("temperature", 0.75);
        jsonMap.put("p", 0.01);
        jsonMap.put("k", 0);
        jsonMap.put("stop_sequences", new String[] {});
        jsonMap.put("return_likelihoods", "NONE");

        if (this.inferenceParameters != null && !this.inferenceParameters.isEmpty()) {
            updateMap(jsonMap, inferenceParameters);
        }
        return new JSONObject(jsonMap).toString();
    }

}

class StabilityAICommand extends BedrockBodyCommand {

    public StabilityAICommand(String prompt, Map<String, Object> inferenceParameters, String system, String role) {
        super(prompt, inferenceParameters, system, role);
    }

    @Override
    public String execute() {

        Map<String, Object> jsonMap = new HashMap<>(4);

        jsonMap.put("text_prompts", new Map[] {
            Map.of("text", this.prompt)
        });
        jsonMap.put("cfg_scale", 10);
        jsonMap.put("seed", 0);
        jsonMap.put("steps", 50);

        if (this.inferenceParameters != null && !this.inferenceParameters.isEmpty()) {
            updateMap(jsonMap, inferenceParameters);
        }
        return new JSONObject(jsonMap).toString();
    }

}
