package com.smartcs.chat.controller;

import com.smartcs.chat.entity.ChatHistory;
import com.smartcs.chat.service.ChatHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/chat/history")
public class ChatHistoryController {

    @Autowired
    private ChatHistoryService chatHistoryService;

    /**
     * 保存聊天记录
     */
    @PostMapping
    public ChatHistory save(@RequestBody ChatHistory chatHistory) {
        return chatHistoryService.save(chatHistory);
    }

    /**
     * 根据 ID 查询
     */
    @GetMapping("/{id}")
    public ChatHistory getById(@PathVariable Long id) {
        return chatHistoryService.getById(id);
    }

    /**
     * 根据用户 ID 查询所有历史
     */
    @GetMapping("/user/{userId}")
    public List<ChatHistory> getByUserId(@PathVariable Long userId) {
        return chatHistoryService.getByUserId(userId);
    }

    /**
     * 根据会话 ID 查询历史
     */
    @GetMapping("/session/{sessionId}")
    public List<ChatHistory> getBySessionId(@PathVariable String sessionId) {
        return chatHistoryService.getBySessionId(sessionId);
    }

    /**
     * 更新评分
     */
    @PatchMapping("/{id}/rating")
    public int updateRating(@PathVariable Long id, @RequestParam Integer rating) {
        return chatHistoryService.updateRating(id, rating);
    }

    /**
     * 删除单条历史
     */
    @DeleteMapping("/{id}")
    public int delete(@PathVariable Long id) {
        return chatHistoryService.delete(id);
    }

    /**
     * 删除用户所有历史
     */
    @DeleteMapping("/user/{userId}")
    public int deleteByUserId(@PathVariable Long userId) {
        return chatHistoryService.deleteByUserId(userId);
    }

    /**
     * 搜索历史
     */
    @GetMapping("/search")
    public List<ChatHistory> search(
            @RequestParam Long userId,
            @RequestParam(required = false) String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return chatHistoryService.getByUserId(userId);
        }
        return chatHistoryService.search(userId, keyword);
    }

    /**
     * 获取评分统计
     */
    @GetMapping("/stats/rating")
    public java.util.Map<String, Object> getRatingStats(@RequestParam Long userId) {
        return chatHistoryService.getRatingStats(userId);
    }
}