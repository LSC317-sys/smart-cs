package com.smartcs.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class EmbeddingService {

    @Value("${siliconflow.embedding-api-key}")
    private String apiKey;

    @Value("${siliconflow.embedding-base-url}")
    private String baseUrl;

    @Value("${siliconflow.embedding-model}")
    private String model;

    private final RestTemplate restTemplate;

    public EmbeddingService() {
        this.restTemplate = new RestTemplate();
        // 强制 UTF-8 编码
        this.restTemplate.getMessageConverters().removeIf(c -> c instanceof StringHttpMessageConverter);
        this.restTemplate.getMessageConverters().add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
    }

    /**
     * 调用硅基流动 BGE-m3 Embedding 接口，将文本转为向量
     */
    public float[] embed(String text) {
        String url = baseUrl + "/embeddings";

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("input", text);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("data")) {
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) responseBody.get("data");
                if (!dataList.isEmpty()) {
                    Map<String, Object> embeddingData = dataList.get(0);
                    List<Number> embedding = (List<Number>) embeddingData.get("embedding");
                    float[] result = new float[embedding.size()];
                    for (int i = 0; i < embedding.size(); i++) {
                        result[i] = embedding.get(i).floatValue();
                    }
                    return result;
                }
            }
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Embedding API 调用失败[Http" + e.getStatusCode() + "]: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Embedding API 调用失败[网络]: " + e.getMessage());
        }

        throw new RuntimeException("Embedding 接口返回格式异常");
    }
}