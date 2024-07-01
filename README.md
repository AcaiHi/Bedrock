主要程式碼在ClaudeContentGeneration.java

.env 要記得配置!
AWS_ACCESS_KEY_ID=XXX
AWS_SECRET_ACCESS_KEY=XXX
AWS_REGION=us-west-2
MODEL_NAME=anthropic.claude-v2

檔案配置：
```cpp
src
├── main // 主要程式碼
│   ├── java
│   │   └── com
│   │       └── amazon
│   │           ├── BedrockContentGenerationApplication.java
│   │           ├── ClaudeContentGeneration.java
│   │           └── controller // 路由導向配置
│   │               └── ContentGenerationController.java
│   └── resources
│       ├── application.properties // 運行配置
│       └── templates
│           └── index.html // 前端顯示頁面

```



## 主要精神

1. API 配置
```java
AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        this.bedrockClient = BedrockRuntimeClient.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
            .build();
```

2. request 設定
```java
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
```
組合成的json應該會長這樣：
```json
{
  "modelId": "anthropic.claude-3-haiku-20240307-v1:0",
  "contentType": "application/json",
  "accept": "application/json",
  "body": {
    "anthropic_version": "bedrock-2023-05-31",
    "max_tokens": 1000,
    "messages": [
      {
        "role": "user",
        "content": [
          {
            "type": "image",
            "source": {
              "type": "base64",
              "media_type": "image/jpeg",
              "data": "iVBORw..."
            }
          },
          {
            "type": "text",
            "text": "What's in this image?"
          }
        ]
      }
    ]
  }
}
```
3. 針對 claude response 設定
```json
{"id":"msg_bdrk_01FQW9WGLaEfWvrWgqhx8oY5","type":"message","role":"assistant","model":"claude-instant-1.2","stop_sequence":null,"usage":{"input_tokens":10,"output_tokens":6},"content":[{"type":"text","text":"Hello!"}],"stop_reason":"end_turn"}

解析方式可以參考 src/main/java/com/amazon/aws/utils/ResponseParser.java
```