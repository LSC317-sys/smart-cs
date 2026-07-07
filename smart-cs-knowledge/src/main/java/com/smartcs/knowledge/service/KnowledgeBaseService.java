package com.smartcs.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartcs.knowledge.entity.KnowledgeBase;
import com.smartcs.knowledge.mapper.KnowledgeBaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    public KnowledgeBaseService(KnowledgeBaseMapper knowledgeBaseMapper) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
    }

    public List<KnowledgeBase> listByUser(Long userId) {
        return knowledgeBaseMapper.selectList(
            Wrappers.<KnowledgeBase>query().eq("user_id", userId).orderByDesc("created_at")
        );
    }

    public KnowledgeBase getById(Long id) {
        return knowledgeBaseMapper.selectById(id);
    }

    public KnowledgeBase create(String name, String description, Long userId) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(name);
        kb.setDescription(description);
        kb.setUserId(userId);
        knowledgeBaseMapper.insert(kb);
        log.info("Created knowledge base: {} by user {}", kb.getId(), userId);
        return kb;
    }

    public KnowledgeBase update(Long id, String name, String description) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb != null) {
            if (name != null && !name.isBlank()) {
                kb.setName(name);
            }
            if (description != null) {
                kb.setDescription(description);
            }
            knowledgeBaseMapper.updateById(kb);
        }
        return kb;
    }

    public boolean delete(Long id) {
        // Check if there are documents in this knowledge base
        Long count = 0L; // Need to query documents table
        if (count > 0) {
            throw new RuntimeException("知识库中还有文档，请先删除文档");
        }
        return knowledgeBaseMapper.deleteById(id) > 0;
    }

    public long countDocuments(Long kbId) {
        // Count documents in this knowledge base
        return 0L; // TODO: Implement
    }
}