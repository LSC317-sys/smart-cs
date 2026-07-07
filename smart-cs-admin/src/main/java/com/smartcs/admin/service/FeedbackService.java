package com.smartcs.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class FeedbackService {

    private final JdbcTemplate jdbcTemplate;

    public FeedbackService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 从 chat_history 表读取反馈统计 */
    public Map<String, Object> getFeedbackStats() {
        try {
            // rating 字段: null=未评价, 1=good(好评), 0=bad(差评)
            // 也兼容 "good"/"bad" 字符串值（直接从前端传来的）
            Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_history WHERE rating IS NOT NULL", Integer.class);
            Integer goodCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_history WHERE rating = 'good' OR rating = 1", Integer.class);
            Integer badCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_history WHERE rating = 'bad' OR rating = 0", Integer.class);
            Integer rated = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_history WHERE rating IS NOT NULL", Integer.class);

            int t = total != null ? total : 0;
            int g = goodCount != null ? goodCount : 0;
            int b = badCount != null ? badCount : 0;

            double satisfactionRate = t > 0 ? (g * 100.0 / t) : 0;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("total", t);
            result.put("rated", rated != null ? rated : 0);
            result.put("good", g);
            result.put("bad", b);
            result.put("satisfactionRate", String.format("%.1f", satisfactionRate));
            result.put("timestamp", System.currentTimeMillis());
            return result;
        } catch (Exception e) {
            log.error("读取反馈统计失败: {}", e.getMessage());
            return Map.of("total", 0, "rated", 0, "good", 0, "bad", 0,
                         "satisfactionRate", "0.0", "error", e.getMessage());
        }
    }

    /** 获取最近反馈列表 */
    public Map<String, Object> getFeedbackList(int limit) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, session_id, question, answer, rating, comment, created_at, user_id " +
                "FROM chat_history WHERE rating IS NOT NULL ORDER BY created_at DESC LIMIT ?",
                limit);

            List<Map<String, Object>> records = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", row.get("id"));
                item.put("sessionId", row.get("session_id"));
                item.put("question", row.get("question"));
                item.put("answer", row.get("answer") != null ? String.valueOf(row.get("answer")).substring(0, Math.min(100, String.valueOf(row.get("answer")).length())) : "");
                item.put("rating", row.get("rating"));
                item.put("comment", row.get("comment"));
                item.put("createdAt", row.get("created_at"));
                item.put("userId", row.get("user_id"));
                records.add(item);
            }

            return Map.of("records", records, "total", records.size());
        } catch (Exception e) {
            log.error("读取反馈列表失败: {}", e.getMessage());
            return Map.of("records", Collections.emptyList(), "total", 0, "error", e.getMessage());
        }
    }
}