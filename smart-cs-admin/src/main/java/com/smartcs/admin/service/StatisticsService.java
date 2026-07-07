package com.smartcs.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StatisticsService {

    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    // ========== Redis Key 前缀常量 ==========
    private static final String CHAT_CONVERSATION_PREFIX = "chat:conversation:";
    private static final String RAG_CONTENT_PREFIX = "rag:content:";
    private static final String RAG_VECTORS_PREFIX = "rag:vectors:";
    private static final String RAG_META_PREFIX = "rag:meta:";

    @Value("${siliconflow.embedding-model:BAAI/bge-m3}")
    private String embeddingModel;

    @Value("${siliconflow.chat-model:Qwen/Qwen2.5-7B-Instruct}")
    private String chatModel;

    public StatisticsService(StringRedisTemplate redis, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.redis = redis;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /** 全局概览：向量数 / 会话数 / 文档数（从 MySQL 读取） */
    public Map<String, Object> getOverview() {
        // 从 MySQL smart_cs 数据库读取文档统计
        Integer totalDocs = 0;
        Long totalChunks = 0L;
        try {
            totalDocs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM documents WHERE status='completed'", Integer.class);
            
            totalChunks = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(chunk_count), 0) FROM documents WHERE status='completed'", Long.class);
        } catch (Exception e) {
            log.error("从MySQL读取文档统计失败: {}", e.getMessage());
        }

        // 会话数从 Redis 读取
        Set<String> chatKeys = redis.keys(CHAT_CONVERSATION_PREFIX + "*");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activeSessions", chatKeys != null ? chatKeys.size() : 0);
        result.put("totalChunks", totalChunks != null ? totalChunks : 0);
        result.put("totalDocs", totalDocs != null ? totalDocs : 0);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /** 会话列表：读取 Redis 中的会话 key 摘要 */
    public Map<String, Object> getConversationList() {
        // ⚠️ 注意：keys() 在生产环境大数据量时应改用 SCAN
        Set<String> keys = redis.keys(CHAT_CONVERSATION_PREFIX + "*");
        List<Map<String, Object>> sessions = new ArrayList<>();

        if (keys != null) {
            for (String key : keys) {
                String historyJson = redis.opsForValue().get(key);
                if (historyJson != null) {
                    try {
                        List<Map<String, Object>> list = objectMapper.readValue(historyJson,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                        int msgCount = list != null ? list.size() : 0;
                        String lastMsg = list != null && !list.isEmpty()
                            ? String.valueOf(list.get(list.size() - 1).get("content"))
                            : "";
                        if (lastMsg.length() > 50) lastMsg = lastMsg.substring(0, 50) + "...";

                        sessions.add(Map.of(
                            "sessionId", key.replace(CHAT_CONVERSATION_PREFIX, ""),
                            "messageCount", msgCount,
                            "lastMessage", lastMsg
                        ));
                    } catch (Exception e) {
                        log.warn("解析会话历史失败: {}", key);
                    }
                }
            }
        }

        sessions.sort((a, b) -> Integer.compare((int) b.get("messageCount"), (int) a.get("messageCount")));
        return Map.of("sessions", sessions, "total", sessions.size());
    }

    /** 删除指定会话 */
    public void clearConversation(String sessionId) {
        redis.delete(CHAT_CONVERSATION_PREFIX + sessionId);
        log.info("会话已清除: {}", sessionId);
    }

    /** 从 Redis 统计知识库向量信息 */
    public Map<String, Object> getKnowledgeStats() {
        // ⚠️ 注意：keys() 在生产环境大数据量时应改用 SCAN
        Set<String> contentKeys = redis.keys(RAG_CONTENT_PREFIX + "*");
        Set<String> vectorKeys = redis.keys(RAG_VECTORS_PREFIX + "*");
        Set<String> metaKeys = redis.keys(RAG_META_PREFIX + "*");

        long totalLen = 0;
        if (contentKeys != null) {
            for (String k : contentKeys) {
                String v = redis.opsForValue().get(k);
                if (v != null) totalLen += v.length();
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalChunks", vectorKeys != null ? vectorKeys.size() : 0);
        result.put("totalDocs", metaKeys != null ? metaKeys.size() : 0);
        result.put("totalContentLen", totalLen);
        return result;
    }

    /** 当前模型配置 */
    public Map<String, Object> getModelConfig() {
        return Map.of(
            "embeddingModel", embeddingModel,
            "chatModel", chatModel,
            "provider", "硅基流动 (SiliconFlow)"
        );
    }
}