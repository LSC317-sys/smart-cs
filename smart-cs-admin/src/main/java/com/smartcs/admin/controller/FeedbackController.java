package com.smartcs.admin.controller;

import com.smartcs.admin.service.FeedbackService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/admin/stats")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /**
     * 用户反馈统计概览
     */
    @GetMapping("/feedback")
    public Map<String, Object> feedbackStats() {
        return feedbackService.getFeedbackStats();
    }

    /**
     * 用户反馈列表（最近N条）
     */
    @GetMapping("/feedback/list")
    public Map<String, Object> feedbackList(
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return feedbackService.getFeedbackList(limit);
    }
}