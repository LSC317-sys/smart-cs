package com.smartcs.chat.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 回答反馈服务
 * 存储在 Redis 中
 */
@Service
public class FeedbackService {

    private static final String FEEDBACK_LIST_KEY = "rag:feedback:list";
    private static final String FEEDBACK_STATS_KEY = "rag:feedback:stats";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 保存反馈
     */
    public void saveFeedback(String sessionId, String question, String answer, String rating, String comment) {
        Map<String, String> feedback = new LinkedHashMap<>();
        feedback.put("id", UUID.randomUUID().toString());
        feedback.put("sessionId", sessionId != null ? sessionId : "");
        feedback.put("question", question != null ? question : "");
        feedback.put("answer", answer != null ? answer : "");
        feedback.put("rating", rating); // "good" | "bad"
        feedback.put("comment", comment != null ? comment : "");
        feedback.put("createdAt", FMT.format(Instant.now()));

        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(feedback);
            redisTemplate.opsForList().leftPush(FEEDBACK_LIST_KEY, json);
            // 最多保留 1000 条反馈
            redisTemplate.opsForList().trim(FEEDBACK_LIST_KEY, 0, 999);

            // 更新统计数据
            updateStats(rating);
        } catch (Exception e) {
            System.err.println("保存反馈失败: " + e.getMessage());
        }
    }

    private void updateStats(String rating) {
        String key = FEEDBACK_STATS_KEY + ":" + rating;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 30, TimeUnit.DAYS);
    }

    /**
     * 获取所有反馈列表
     */
    public List<Map<String, Object>> getAllFeedback() {
        List<String> raw = redisTemplate.opsForList().range(FEEDBACK_LIST_KEY, 0, -1);
        if (raw == null || raw.isEmpty()) return Collections.emptyList();

        List<Map<String, Object>> result = new ArrayList<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            for (String json : raw) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fb = mapper.readValue(json, Map.class);
                    result.add(fb);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.err.println("读取反馈列表失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取统计数据
     */
    public Map<String, Object> getStats() {
        String goodKey = FEEDBACK_STATS_KEY + ":good";
        String badKey = FEEDBACK_STATS_KEY + ":bad";

        int good = 0, bad = 0;
        try { good = Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get(goodKey))); } catch (Exception ignored) {}
        try { bad = Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get(badKey))); } catch (Exception ignored) {}

        List<Map<String, Object>> recent = getAllFeedback().stream().limit(10).toList();

        return Map.of(
            "total", good + bad,
            "good", good,
            "bad", bad,
            "satisfactionRate", good + bad > 0 ? String.format("%.1f", (double) good / (good + bad) * 100) : "0",
            "recent", recent
        );
    }
}
