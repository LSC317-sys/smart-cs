package com.smartcs.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RAGService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private VectorStoreService vectorStoreService;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private RAGConfigService ragConfigService;

    @Value("${siliconflow.chat-api-key}")
    private String chatApiKey;

    @Value("${siliconflow.chat-base-url}")
    private String chatBaseUrl;

    @Value("${siliconflow.chat-model}")
    private String chatModel;
    
    // @Value 作为默认值，实际使用 RAGConfigService 动态获取
    @Value("${rag.top-k:5}")
    private int defaultTopK = 5;

    @Value("${rag.min-similarity:0.3}")
    private float defaultMinSimilarity = 0.3f;

    // LLM 调用常量
    private static final int LLM_MAX_TOKENS = 500;
    private static final double LLM_TEMPERATURE = 0.3;
    private static final int LLM_CONNECT_TIMEOUT_MS = 15000;
    private static final int LLM_READ_TIMEOUT_MS = 120000;
    
    // 系统提示词常量
    private static final String SYSTEM_PROMPT_NO_CONTEXT = 
        "你是一个智能客服。请根据你的知识回答用户问题。如果不知道，请说明。" +
        "重要：回答时不要在文字两边加引号，直接输出纯文本。";
    private static final String SYSTEM_PROMPT_WITH_CONTEXT_PREFIX = 
        "你是一个智能客服。请根据以下参考信息回答用户问题。\n\n【参考信息】\n";
    private static final String SYSTEM_PROMPT_WITH_CONTEXT_SUFFIX = 
        "\n\n回答要求：\n" +
        "1. 优先基于【参考信息】回答\n" +
        "2. 如果参考信息不足，补充你的知识但要说明\n" +
        "3. 回答简洁有条理\n" +
        "4. 直接输出回答内容，不要在文字两边加引号，不要用引号包裹句子";

    // 兼容旧接口：如果没有 sessionId，使用默认配置
    public AnswerResult answerWithSession(String question, String sessionId) {
        return answerWithSession(question, sessionId, null, null);
    }

    // 主要方法：支持传入自定义参数
    public AnswerResult answerWithSession(String question, String sessionId, Integer topKOverride, Float minSimilarityOverride) {
        int topK = (topKOverride != null) ? topKOverride : ragConfigService.getTopK();
        float minSimilarity = (minSimilarityOverride != null) ? minSimilarityOverride : ragConfigService.getMinSimilarity();
        float[] queryVector = embeddingService.embed(question);

        List<DocumentChunker.DocumentChunk> allChunks = vectorStoreService.loadAllChunks();
        
        List<VectorStoreService.SearchResult> results = vectorStoreService.search(
                allChunks, queryVector, topK, minSimilarity);
        
        // 构建带相似度的上下文
        String context = results.isEmpty() ? "" : results.stream()
                .map(r -> String.format("【相关度: %.1f%%】\n%s", r.similarity * 100, r.chunk.getContent()))
                .collect(Collectors.joining("\n\n---\n\n"));

        // 构建调试信息（检索详情，含文档标题）
        List<Map<String, Object>> debugChunks = new ArrayList<>();
        for (VectorStoreService.SearchResult r : results) {
            Map<String, Object> item = new LinkedHashMap<>();
            String docId = r.chunk.getDocumentId();
            item.put("docId", docId);
            item.put("index", r.chunk.getIndex());
            item.put("similarity", Math.round(r.similarity * 1000) / 10.0);
            item.put("contentPreview", r.chunk.getContent().length() > 120 
                    ? r.chunk.getContent().substring(0, 120) + "..." 
                    : r.chunk.getContent());
            item.put("content", r.chunk.getContent());
            // 从 chunk 元数据获取文档标题
            String title = r.chunk.getDocTitle();
            item.put("docTitle", title != null && !title.isEmpty() ? title : docId);
            debugChunks.add(item);
        }

        List<Map<String, String>> messages = new ArrayList<>();

        String systemPrompt = buildSystemPrompt(context);
        messages.add(Map.of("role", "system", "content", systemPrompt));

        if (sessionId != null && !sessionId.trim().isEmpty()) {
            List<Map<String, String>> history = conversationService.getHistory(sessionId);
            messages.addAll(history);
            conversationService.addUserMessage(sessionId, question);
        }

        messages.add(Map.of("role", "user", "content", question));

        String answer = callLLM(messages);

        if (sessionId != null && !sessionId.trim().isEmpty()) {
            conversationService.addAssistantMessage(sessionId, answer);
        }

        return new AnswerResult(answer, results.size(), context.length(), debugChunks);
    }

    public String answer(String question) {
        RAGService.AnswerResult result = answerWithSession(question, null, null, null);
        return result.getAnswer();
    }

    private String buildSystemPrompt(String context) {
        if (context == null || context.trim().isEmpty()) {
            return SYSTEM_PROMPT_NO_CONTEXT;
        }
        return SYSTEM_PROMPT_WITH_CONTEXT_PREFIX + context + SYSTEM_PROMPT_WITH_CONTEXT_SUFFIX;
    }

    private String callLLM(List<Map<String, String>> messages) {
        String url = chatBaseUrl + "/chat/completions";

        Map<String, Object> body = new HashMap<>();
        body.put("model", chatModel);
        body.put("stream", false);
        body.put("max_tokens", LLM_MAX_TOKENS);
        body.put("temperature", LLM_TEMPERATURE);
        body.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(chatApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);
            String rawResponse = response.getBody();

            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    String content = message.get("content").asText();
                    content = content.replaceAll("^```(?:json)?\\s*", "")
                                     .replaceAll("\\s*```$", "")
                                     .trim();
                    return content;
                }
            }
        } catch (Exception e) {
            System.err.println("LLM call failed: " + e.getMessage());
            return "LLM 调用失败: " + e.getMessage();
        }

        return "LLM 返回格式异常";
    }

    public static class AnswerResult {
        private String answer;
        private int chunksRetrieved;
        private int contextLength;
        private List<Map<String, Object>> debug;

        public AnswerResult(String answer, int chunksRetrieved, int contextLength, List<Map<String, Object>> debug) {
            this.answer = answer;
            this.chunksRetrieved = chunksRetrieved;
            this.contextLength = contextLength;
            this.debug = debug;
        }

        public String getAnswer() { return answer; }
        public int getChunksRetrieved() { return chunksRetrieved; }
        public int getContextLength() { return contextLength; }
        public List<Map<String, Object>> getDebug() { return debug; }
    }

    // ==================== SSE 流式输出 ====================

    public String callLLMStream(java.util.function.Consumer<String> onChunk,
                                java.util.function.Consumer<String> onError,
                                java.util.List<java.util.Map<String, String>> messages) {
        String urlStr = chatBaseUrl + "/chat/completions";
        java.io.BufferedReader reader = null;
        java.net.HttpURLConnection conn = null;

        try {
            java.net.URL url = new java.net.URL(urlStr);
            conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + chatApiKey);
            conn.setRequestProperty("Accept", "text/event-stream");
            conn.setDoOutput(true);
            conn.setConnectTimeout(LLM_CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(LLM_READ_TIMEOUT_MS);

            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("model", chatModel);
            body.put("stream", true);
            body.put("max_tokens", LLM_MAX_TOKENS);
            body.put("temperature", LLM_TEMPERATURE);
            body.put("messages", messages);

            String jsonBody = objectMapper.writeValueAsString(body);

            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                String err = "LLM API 返回错误码: " + responseCode;
                onError.accept(err);
                return err;
            }

            StringBuilder fullAnswer = new StringBuilder();

            reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data:")) {
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    String chunk = parseStreamChunk(data);
                    if (chunk != null && !chunk.isEmpty()) {
                        fullAnswer.append(chunk);
                        onChunk.accept(chunk);
                    }
                }
            }

            return fullAnswer.toString();

        } catch (Exception e) {
            e.printStackTrace();
            onError.accept("流式调用异常: " + e.getMessage());
            return "流式调用异常: " + e.getMessage();
        } finally {
            if (reader != null) { try { reader.close(); } catch (Exception ignored) {} }
            if (conn != null) { conn.disconnect(); }
        }
    }

    private String parseStreamChunk(String data) {
        try {
            JsonNode node = objectMapper.readTree(data);
            JsonNode choices = node.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode delta = choices.get(0).get("delta");
                if (delta != null && delta.has("content")) {
                    return delta.get("content").asText();
                }
            }
        } catch (Exception e) {}
        return null;
    }

    public String answerWithSessionStream(String question,
                                          String sessionId,
                                          java.util.function.Consumer<String> onChunk,
                                          java.util.function.Consumer<String> onError,
                                          java.util.function.Consumer<java.util.List<java.util.Map<String, Object>>> onSources) {
        return answerWithSessionStream(question, sessionId, onChunk, onError, onSources, null, null);
    }

    public String answerWithSessionStream(String question,
                                          String sessionId,
                                          java.util.function.Consumer<String> onChunk,
                                          java.util.function.Consumer<String> onError,
                                          java.util.function.Consumer<java.util.List<java.util.Map<String, Object>>> onSources,
                                          Integer topKOverride,
                                          Float minSimilarityOverride) {
        int topK = (topKOverride != null) ? topKOverride : ragConfigService.getTopK();
        float minSimilarity = (minSimilarityOverride != null) ? minSimilarityOverride : ragConfigService.getMinSimilarity();

        float[] queryVector = embeddingService.embed(question);
        List<DocumentChunker.DocumentChunk> allChunks = vectorStoreService.loadAllChunks();
        List<VectorStoreService.SearchResult> results = vectorStoreService.search(allChunks, queryVector, topK, minSimilarity);

        // 构建检索结果（用于 SSE 事件）
        List<Map<String, Object>> debugChunks = new ArrayList<>();
        for (VectorStoreService.SearchResult r : results) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("docId", r.chunk.getDocumentId());
            item.put("index", r.chunk.getIndex());
            item.put("similarity", Math.round(r.similarity * 1000) / 10.0);
            item.put("contentPreview", r.chunk.getContent().length() > 120
                    ? r.chunk.getContent().substring(0, 120) + "..."
                    : r.chunk.getContent());
            item.put("content", r.chunk.getContent());
            String title = r.chunk.getDocTitle();
            item.put("docTitle", title != null && !title.isEmpty() ? title : r.chunk.getDocumentId());
            debugChunks.add(item);
        }

        // 通过回调发送检索结果
        if (onSources != null) {
            onSources.accept(debugChunks);
        }

        String context = results.isEmpty() ? "" : results.stream()
                .map(r -> String.format("【相关度: %.1f%%】\n%s", r.similarity * 100, r.chunk.getContent()))
                .collect(Collectors.joining("\n\n---\n\n"));

        java.util.List<java.util.Map<String, String>> messages = new java.util.ArrayList<>();
        String systemPrompt = buildSystemPrompt(context);
        messages.add(java.util.Map.of("role", "system", "content", systemPrompt));

        if (sessionId != null && !sessionId.trim().isEmpty()) {
            java.util.List<java.util.Map<String, String>> history = conversationService.getHistory(sessionId);
            messages.addAll(history);
            conversationService.addUserMessage(sessionId, question);
        }

        messages.add(java.util.Map.of("role", "user", "content", question));

        String fullAnswer = callLLMStream(onChunk, onError, messages);

        if (sessionId != null && !sessionId.trim().isEmpty()) {
            conversationService.addAssistantMessage(sessionId, fullAnswer);
        }

        return fullAnswer;
    }

}