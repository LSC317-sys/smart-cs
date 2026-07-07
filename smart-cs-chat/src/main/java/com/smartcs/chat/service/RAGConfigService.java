package com.smartcs.chat.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * RAG 配置服务 - 管理检索参数（存储在 Redis 中，支持动态调整）
 */
@Service
public class RAGConfigService {

    private static final String KEY_PREFIX = "rag:config:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 默认值（来自 application.yml）
    @Value("${rag.top-k:5}")
    private int defaultTopK;

    @Value("${rag.min-similarity:0.3}")
    private float defaultMinSimilarity;

    // Redis key
    private static final String KEY_TOP_K = KEY_PREFIX + "top_k";
    private static final String KEY_MIN_SIMILARITY = KEY_PREFIX + "min_similarity";

    /**
     * 获取当前 RAG 配置
     */
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("topK", getTopK());
        config.put("minSimilarity", getMinSimilarity());
        config.put("defaultTopK", defaultTopK);
        config.put("defaultMinSimilarity", defaultMinSimilarity);
        return config;
    }

    /**
     * 获取 top-k 值（优先 Redis，否则用默认值）
     */
    public int getTopK() {
        String value = redisTemplate.opsForValue().get(KEY_TOP_K);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {}
        }
        return defaultTopK;
    }

    /**
     * 设置 top-k 值
     */
    public void setTopK(int topK) {
        if (topK < 1) topK = 1;
        if (topK > 20) topK = 20;
        redisTemplate.opsForValue().set(KEY_TOP_K, String.valueOf(topK));
    }

    /**
     * 获取最小相似度阈值（优先 Redis，否则用默认值）
     */
    public float getMinSimilarity() {
        String value = redisTemplate.opsForValue().get(KEY_MIN_SIMILARITY);
        if (value != null && !value.isEmpty()) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException ignored) {}
        }
        return defaultMinSimilarity;
    }

    /**
     * 设置最小相似度阈值
     */
    public void setMinSimilarity(float similarity) {
        if (similarity < 0f) similarity = 0f;
        if (similarity > 1f) similarity = 1f;
        redisTemplate.opsForValue().set(KEY_MIN_SIMILARITY, String.valueOf(similarity));
    }

    /**
     * 重置为默认配置
     */
    public void resetToDefaults() {
        redisTemplate.delete(KEY_TOP_K);
        redisTemplate.delete(KEY_MIN_SIMILARITY);
    }

    /**
     * 更新配置
     */
    public void updateConfig(Integer topK, Float minSimilarity) {
        if (topK != null) {
            setTopK(topK);
        }
        if (minSimilarity != null) {
            setMinSimilarity(minSimilarity);
        }
    }
}
