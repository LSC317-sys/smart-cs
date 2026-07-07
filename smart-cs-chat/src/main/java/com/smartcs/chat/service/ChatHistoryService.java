package com.smartcs.chat.service;

import com.smartcs.chat.entity.ChatHistory;
import com.smartcs.chat.mapper.ChatHistoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ChatHistoryService {

    @Autowired
    private ChatHistoryMapper chatHistoryMapper;

    /**
     * 保存聊天历史
     */
    public ChatHistory save(ChatHistory chatHistory) {
        chatHistoryMapper.insert(chatHistory);
        return chatHistory;
    }

    /**
     * 根据 ID 查询
     */
    public ChatHistory getById(Long id) {
        return chatHistoryMapper.selectById(id);
    }

    /**
     * 根据用户 ID 查询所有历史
     */
    public List<ChatHistory> getByUserId(Long userId) {
        return chatHistoryMapper.selectByUserId(userId);
    }

    /**
     * 根据会话 ID 查询历史
     */
    public List<ChatHistory> getBySessionId(String sessionId) {
        return chatHistoryMapper.selectBySessionId(sessionId);
    }

    /**
     * 更新评分
     */
    public int updateRating(Long id, Integer rating) {
        return chatHistoryMapper.updateRating(id, rating);
    }

    /**
     * 删除单条历史
     */
    public int delete(Long id) {
        return chatHistoryMapper.deleteById(id);
    }

    /**
     * 删除用户所有历史
     */
    public int deleteByUserId(Long userId) {
        return chatHistoryMapper.deleteByUserId(userId);
    }

    /**
     * 搜索历史
     */
    public List<ChatHistory> search(Long userId, String keyword) {
        return chatHistoryMapper.search(userId, keyword);
    }

    /**
     * 获取评分统计
     */
    public java.util.Map<String, Object> getRatingStats(Long userId) {
        List<java.util.Map<String, Object>> ratingCounts = chatHistoryMapper.countByRating(userId);
        int total = chatHistoryMapper.countByUserId(userId);
        
        int goodCount = 0;
        int badCount = 0;
        int neutralCount = 0;
        
        for (java.util.Map<String, Object> row : ratingCounts) {
            Integer rating = ((Number) row.get("rating")).intValue();
            Integer count = ((Number) row.get("count")).intValue();
            if (rating >= 4) {
                goodCount += count;
            } else if (rating <= 2) {
                badCount += count;
            } else {
                neutralCount += count;
            }
        }
        
        int ratedCount = goodCount + badCount + neutralCount;
        double satisfactionRate = ratedCount > 0 ? (double) goodCount / ratedCount * 100 : 0;
        
        return java.util.Map.of(
            "total", total,
            "rated", ratedCount,
            "good", goodCount,
            "neutral", neutralCount,
            "bad", badCount,
            "satisfactionRate", String.format("%.1f", satisfactionRate)
        );
    }
}