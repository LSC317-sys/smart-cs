package com.smartcs.chat.controller;

import com.smartcs.chat.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

import java.util.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private RAGService ragService;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private VectorStoreService vectorStoreService;

    @Autowired
    private DocumentChunker documentChunker;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private RAGConfigService ragConfigService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== 测试接口 ====================

    @GetMapping("/test/chinese")
    public String testChinese() throws Exception {
        Map<String, Object> data = Map.of(
            "message", "文档已入库",
            "name", "智能客服系统",
            "count", 42
        );
        return objectMapper.writeValueAsString(data);
    }

    @GetMapping("/test/chinese2")
    public String testChinese2() throws Exception {
        Map<String, Object> data = Map.of(
            "message", "文档已入库",
            "name", "智能客服系统",
            "count", 42
        );
        return objectMapper.writeValueAsString(data);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "service", "smart-cs-chat",
            "port", 8081,
            "time", System.currentTimeMillis()
        );
    }

    // ==================== 核心业务接口 ====================

    @PostMapping("/ask")
    public String ask(@RequestBody Map<String, String> request, HttpServletResponse response) throws Exception {
        String question = request.get("question");
        if (question == null || question.trim().isEmpty()) {
            return objectMapper.writeValueAsString(Map.of("error", "问题不能为空"));
        }
        RAGService.AnswerResult result = ragService.answerWithSession(question, null);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("answer", result.getAnswer());
        res.put("question", question);
        res.put("chunksRetrieved", result.getChunksRetrieved());
        res.put("contextLength", result.getContextLength());
        res.put("sources", result.getDebug());
        res.put("debug", result.getDebug());
        return objectMapper.writeValueAsString(res);
    }

    @PostMapping("/chat")
    public String chat(@RequestBody Map<String, String> request, HttpServletResponse response) throws Exception {
        String question = request.get("question");
        String sessionId = request.get("sessionId");

        if (question == null || question.trim().isEmpty()) {
            return objectMapper.writeValueAsString(Map.of("error", "问题不能为空"));
        }

        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        RAGService.AnswerResult result = ragService.answerWithSession(question, sessionId);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("answer", result.getAnswer());
        res.put("question", question);
        res.put("sessionId", sessionId);
        res.put("chunksRetrieved", result.getChunksRetrieved());
        res.put("contextLength", result.getContextLength());
        res.put("sources", result.getDebug());
        res.put("debug", result.getDebug());

        return objectMapper.writeValueAsString(res);
    }

    @GetMapping("/session/{sessionId}")
    public String getSession(@PathVariable("sessionId") String sessionId, HttpServletResponse response) throws Exception {
        try {
            List<Map<String, String>> history = conversationService.getHistory(sessionId);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("sessionId", sessionId);
            result.put("messages", history);
            result.put("count", history.size());
            result.put("status", "ok");
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getClass().getName() + ": " + e.getMessage());
            err.put("sessionId", sessionId);
            return objectMapper.writeValueAsString(err);
        }
    }

    @GetMapping("/session/test/{sessionId}")
    public String testSession(@PathVariable("sessionId") String sessionId) throws Exception {
        System.out.println("=== testSession called with: " + sessionId + " ===");
        String json = "{\"test\":true,\"sessionId\":\"" + sessionId + "\"}";
        System.out.println("=== testSession returning: " + json + " ===");
        return json;
    }

    @DeleteMapping("/session/{sessionId}")
    public String clearSession(@PathVariable("sessionId") String sessionId, HttpServletResponse response) throws Exception {
        conversationService.clearSession(sessionId);
        return objectMapper.writeValueAsString(Map.of(
            "message", "会话已清除",
            "sessionId", sessionId
        ));
    }

    @PostMapping("/knowledge/add")
    public String addKnowledge(@RequestBody Map<String, String> request, HttpServletResponse response) throws Exception {
        String docId = request.getOrDefault("docId", UUID.randomUUID().toString());
        String title = request.getOrDefault("title", "未命名文档");
        String content = request.get("content");

        if (content == null || content.trim().isEmpty()) {
            return objectMapper.writeValueAsString(Map.of("error", "文档内容不能为空"));
        }

        List<DocumentChunker.DocumentChunk> chunks = documentChunker.chunkByParagraph(content, 500, 50);

        for (DocumentChunker.DocumentChunk chunk : chunks) {
            float[] embedding = embeddingService.embed(chunk.getContent());
            chunk.setEmbedding(embedding);
        }
        vectorStoreService.saveChunks(docId, chunks);
        vectorStoreService.saveDocMeta(docId, title, content.length(), chunks.size());

        return objectMapper.writeValueAsString(Map.of(
            "message", "文档已入库",
            "docId", docId,
            "title", title,
            "chunks", chunks.size()
        ));
    }

    @GetMapping("/knowledge/list")
    public String listKnowledge(HttpServletResponse response) throws Exception {
        List<Map<String, Object>> docs = vectorStoreService.listDocuments();
        return objectMapper.writeValueAsString(Map.of(
            "documents", docs,
            "count", docs.size()
        ));
    }

    @DeleteMapping("/knowledge/{docId}")
    public String deleteKnowledge(@PathVariable("docId") String docId, HttpServletResponse response) throws Exception {
        vectorStoreService.deleteDocument(docId);
        return objectMapper.writeValueAsString(Map.of(
            "message", "文档已删除",
            "docId", docId
        ));
    }

    @GetMapping("/knowledge/stats")
    public String knowledgeStats(HttpServletResponse response) throws Exception {
        List<DocumentChunker.DocumentChunk> chunks = vectorStoreService.loadAllChunks();
        List<Map<String, Object>> docs = vectorStoreService.listDocuments();
        return objectMapper.writeValueAsString(Map.of(
            "totalChunks", chunks.size(),
            "totalDocuments", docs.size(),
            "sampleChunk", chunks.isEmpty() ? null : chunks.get(0).getContent().substring(0, Math.min(50, chunks.get(0).getContent().length()))
        ));
    }

    // ==================== SSE 流式输出接口 ====================

    @PostMapping("/chat/stream")
    public void chatStream(@RequestBody Map<String, String> request,
                           jakarta.servlet.http.HttpServletResponse response) throws Exception {

        String question = request.get("question");
        String sessionId = request.get("sessionId");

        if (question == null || question.trim().isEmpty()) {
            response.setStatus(400);
            response.getWriter().write("event: error\ndata: {\"error\": \"问题不能为空\"}\n\n");
            response.getWriter().flush();
            return;
        }

        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
        response.flushBuffer();

        response.getWriter().write("event: sessionId\ndata: " + sessionId + "\n\n");
        response.getWriter().flush();

        ragService.answerWithSessionStream(question, sessionId,

                (String chunk) -> {
                    try {
                        String safe = chunk.replace("\\", "\\\\")
                                .replace("\n", "\\n")
                                .replace("\r", "\\r")
                                .replace("\"", "\\\"");
                        response.getWriter().write("event: message\ndata: \"" + safe + "\"\n\n");
                        response.getWriter().flush();
                    } catch (Exception ignored) {}
                },
            (String errMsg) -> {
                try {
                    response.getWriter().write("event: error\ndata: \"" + errMsg + "\"\n\n");
                    response.getWriter().flush();
                } catch (Exception ignored) {}
            },

            // onSources: 检索结果回调
            (List<Map<String, Object>> sources) -> {
                try {
                    // 发送 sources 事件（直接发送 JSON，SSE data 行不需要额外转义）
                    String sourcesJson = objectMapper.writeValueAsString(sources);
                    response.getWriter().write("event: sources\ndata: " + sourcesJson + "\n\n");
                    response.getWriter().flush();
                } catch (Exception ignored) {}
            }
        );

        try {
            response.getWriter().write("event: done\ndata: \"\"\n\n");
            response.getWriter().flush();
        } catch (Exception ignored) {}
    }

    // ==================== 会话管理接口 ====================

    /**
     * 列出所有会话（带元数据）
     */
    @GetMapping("/sessions")
    public String listSessions(HttpServletResponse response) throws Exception {
        List<ConversationService.SessionMeta> metas = conversationService.listSessionMetas();
        return objectMapper.writeValueAsString(Map.of(
            "sessions", metas,
            "count", metas.size()
        ));
    }

    /**
     * 重命名会话
     */
    @PutMapping("/sessions/{sessionId}/title")
    public String renameSession(@PathVariable("sessionId") String sessionId,
                                @RequestBody Map<String, String> body,
                                HttpServletResponse response) throws Exception {
        String title = body.get("title");
        if (title == null || title.trim().isEmpty()) {
            return objectMapper.writeValueAsString(Map.of("error", "标题不能为空"));
        }
        if (title.length() > 60) title = title.substring(0, 60);
        conversationService.updateSessionTitle(sessionId, title);
        return objectMapper.writeValueAsString(Map.of("message", "标题已更新", "title", title));
    }

    // ==================== 回答反馈接口 ====================

    @Autowired
    private FeedbackService feedbackService;

    /**
     * 提交回答反馈
     */
    @PostMapping("/feedback")
    public String submitFeedback(@RequestBody Map<String, Object> body, HttpServletResponse response) throws Exception {
        String sessionId = body.get("sessionId") != null ? body.get("sessionId").toString() : null;
        String question = body.get("question") != null ? body.get("question").toString() : null;
        String answer = body.get("answer") != null ? body.get("answer").toString() : null;
        String rating = body.get("rating") != null ? body.get("rating").toString() : null;
        String comment = body.get("comment") != null ? body.get("comment").toString() : null;

        if (rating == null) {
            return objectMapper.writeValueAsString(Map.of("error", "缺少评分"));
        }

        feedbackService.saveFeedback(sessionId, question, answer, rating, comment);
        return objectMapper.writeValueAsString(Map.of("message", "反馈已提交"));
    }

    /**
     * 获取反馈统计（管理员）
     */
    @GetMapping("/feedback/stats")
    public String feedbackStats(HttpServletResponse response) throws Exception {
        return objectMapper.writeValueAsString(feedbackService.getStats());
    }

    // ==================== RAG 配置接口 ====================

    /**
     * 获取当前 RAG 配置
     */
    @GetMapping("/config")
    public String getRAGConfig(HttpServletResponse response) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "code", 0,
            "message", "success",
            "data", ragConfigService.getConfig()
        ));
    }

    /**
     * 更新 RAG 配置
     */
    @PutMapping("/config")
    public String updateRAGConfig(@RequestBody Map<String, Object> body, HttpServletResponse response) throws Exception {
        Integer topK = body.get("topK") != null ? ((Number) body.get("topK")).intValue() : null;
        Float minSimilarity = body.get("minSimilarity") != null ? ((Number) body.get("minSimilarity")).floatValue() : null;

        if (topK != null && (topK < 1 || topK > 20)) {
            return objectMapper.writeValueAsString(Map.of(
                "code", 400,
                "message", "topK 必须在 1-20 之间"
            ));
        }

        if (minSimilarity != null && (minSimilarity < 0f || minSimilarity > 1f)) {
            return objectMapper.writeValueAsString(Map.of(
                "code", 400,
                "message", "minSimilarity 必须在 0-1 之间"
            ));
        }

        ragConfigService.updateConfig(topK, minSimilarity);

        return objectMapper.writeValueAsString(Map.of(
            "code", 0,
            "message", "配置已更新",
            "data", ragConfigService.getConfig()
        ));
    }

    /**
     * 重置 RAG 配置为默认值
     */
    @PostMapping("/config/reset")
    public String resetRAGConfig(HttpServletResponse response) throws Exception {
        ragConfigService.resetToDefaults();
        return objectMapper.writeValueAsString(Map.of(
            "code", 0,
            "message", "配置已重置为默认值",
            "data", ragConfigService.getConfig()
        ));
    }
}
