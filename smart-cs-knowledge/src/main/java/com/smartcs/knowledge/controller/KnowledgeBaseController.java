package com.smartcs.knowledge.controller;

import com.smartcs.knowledge.entity.KnowledgeBase;
import com.smartcs.knowledge.service.KnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识库管理 API
 */
@Slf4j
@RestController
@RequestMapping("/knowledge/kb")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * 获取用户的所有知识库列表
     */
    @GetMapping("/list")
    public List<KnowledgeBase> list(@RequestParam(value = "userId", defaultValue = "1") Long userId) {
        return knowledgeBaseService.listByUser(userId);
    }

    /**
     * 获取知识库详情
     */
    @GetMapping("/{id}")
    public KnowledgeBase get(@PathVariable Long id) {
        return knowledgeBaseService.getById(id);
    }

    /**
     * 创建知识库
     */
    @PostMapping
    public KnowledgeBase create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String description = body.get("description");
        Long userId = body.get("userId") != null ? Long.parseLong(body.get("userId")) : 1L;
        return knowledgeBaseService.create(name, description, userId);
    }

    /**
     * 更新知识库
     */
    @PutMapping("/{id}")
    public KnowledgeBase update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String name = body.get("name");
        String description = body.get("description");
        return knowledgeBaseService.update(id, name, description);
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        try {
            boolean success = knowledgeBaseService.delete(id);
            return Map.of("success", success, "id", id, "message", success ? "知识库已删除" : "删除失败");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    /**
     * 获取知识库统计信息
     */
    @GetMapping("/{id}/stats")
    public Map<String, Object> stats(@PathVariable Long id) {
        long docCount = knowledgeBaseService.countDocuments(id);
        return Map.of("kbId", id, "totalDocs", docCount);
    }
}