package com.smartcs.knowledge.service;

import com.smartcs.knowledge.config.SiliconFlowConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * 调用 SiliconFlow Embedding API 生成向量
 */
@Slf4j
@Service
public class EmbeddingService {

    private final SiliconFlowConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public EmbeddingService(SiliconFlowConfig config) {
        this.config = config;
    }

    /**
     * 单条文本转向量
     */
    public float[] embed(String text) {
        try {
            String payload = "{\"model\":\"" + config.getEmbeddingModel() + "\",\"input\":\"" + escapeJson(text) + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getEmbeddingBaseUrl() + "/embeddings"))
                    .header("Authorization", "Bearer " + config.getEmbeddingApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                throw new RuntimeException("Embedding API error: " + response.statusCode() + " body=" + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.get("data");
            if (data == null || !data.isArray() || data.isEmpty()) {
                throw new RuntimeException("Invalid embedding response: " + response.body());
            }
            JsonNode embedding = data.get(0).get("embedding");
            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = (float) embedding.get(i).asDouble();
            }
            return vector;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Embedding failed for text length " + text.length() + ": " + e.getMessage(), e);
        }
    }

    /**
     * 批量文本转向量（每次最多10条）
     */
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        int batchSize = 10;
        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);

            // 构造 JSON array: {"model":"...","input":["text1","text2",...]}
            StringBuilder sb = new StringBuilder();
            sb.append("{\"model\":\"").append(config.getEmbeddingModel())
              .append("\",\"input\":[");
            for (int j = 0; j < batch.size(); j++) {
                if (j > 0) sb.append(",");
                sb.append("\"").append(escapeJson(batch.get(j))).append("\"");
            }
            sb.append("]}");
            String payload = sb.toString();

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(config.getEmbeddingBaseUrl() + "/embeddings"))
                        .header("Authorization", "Bearer " + config.getEmbeddingApiKey())
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));

                if (response.statusCode() != 200) {
                    log.error("Batch embedding API error: {} body={}", response.statusCode(), response.body());
                    throw new RuntimeException("Embedding batch failed: " + response.statusCode());
                }

                JsonNode root = objectMapper.readTree(response.body());
                JsonNode data = root.get("data");
                for (JsonNode item : data) {
                    JsonNode emb = item.get("embedding");
                    float[] vec = new float[emb.size()];
                    for (int j = 0; j < emb.size(); j++) {
                        vec[j] = (float) emb.get(j).asDouble();
                    }
                    results.add(vec);
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Batch embedding failed at index " + i + ": " + e.getMessage(), e);
            }
        }
        return results;
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
