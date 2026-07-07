package com.smartcs.admin.controller;

import com.smartcs.admin.service.StatisticsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
public class StatsController {

    private final StatisticsService statisticsService;

    public StatsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * 全局统计概览
     */
    @GetMapping("/stats/overview")
    public Map<String, Object> overview() {
        return statisticsService.getOverview();
    }

    /**
     * 会话历史列表（从 Redis 读取）
     */
    @GetMapping("/stats/conversations")
    public Map<String, Object> conversations() {
        return statisticsService.getConversationList();
    }

    /**
     * 清除指定会话历史
     */
    @GetMapping("/stats/conversations/{sessionId}/clear")
    public Map<String, Object> clearConversation(
            @PathVariable("sessionId") String sessionId) {
        statisticsService.clearConversation(sessionId);
        return Map.of("success", true, "sessionId", sessionId);
    }

    /**
     * 知识库状态统计
     */
    @GetMapping("/stats/knowledge")
    public Map<String, Object> knowledgeStats() {
        return statisticsService.getKnowledgeStats();
    }

    /**
     * 模型配置查询（只读）
     */
    @GetMapping("/config/models")
    public Map<String, Object> modelConfig() {
        return statisticsService.getModelConfig();
    }
}