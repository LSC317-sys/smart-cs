package com.smartcs.chat.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 向量存储与检索服务
 * 使用 Redis Hash 存储向量，支持余弦相似度检索
 */
@Service
public class VectorStoreService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Redis key 前缀
    private static final String VECTOR_KEY_PREFIX = "rag:vectors:";
    private static final String DOC_CONTENT_PREFIX = "rag:content:";
    private static final String DOC_META_PREFIX = "rag:meta:";
    private static final String DOC_INDEX_KEY = "rag:doc_index";

    /**
     * 保存文档块及其向量
     */
    public void saveChunks(String documentId, List<DocumentChunker.DocumentChunk> chunks) {
        for (DocumentChunker.DocumentChunk chunk : chunks) {
            // 保存向量（序列化 float[] 为 JSON 字符串）
            String vectorJson;
            try {
                vectorJson = objectMapper.writeValueAsString(chunk.getEmbedding());
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new RuntimeException("序列化向量失败", e);
            }
            String vectorKey = VECTOR_KEY_PREFIX + documentId + ":" + chunk.getIndex();
            redisTemplate.opsForValue().set(vectorKey, vectorJson);

            // 保存原始文本内容
            String contentKey = DOC_CONTENT_PREFIX + documentId + ":" + chunk.getIndex();
            redisTemplate.opsForValue().set(contentKey, chunk.getContent());
        }
    }

    /**
     * 保存文档元数据
     */
    public void saveDocMeta(String docId, String title, int contentLength, int chunkCount) {
        try {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("docId", docId);
            meta.put("title", title);
            meta.put("contentLength", contentLength);
            meta.put("chunkCount", chunkCount);
            meta.put("createdAt", System.currentTimeMillis());
            String metaJson = objectMapper.writeValueAsString(meta);
            redisTemplate.opsForValue().set(DOC_META_PREFIX + docId, metaJson);
            // 维护文档索引（Set）
            redisTemplate.opsForSet().add(DOC_INDEX_KEY, docId);
        } catch (Exception e) {
            System.err.println("保存文档元数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档元数据
     */
    public Map<String, Object> getDocMeta(String docId) {
        try {
            String metaJson = redisTemplate.opsForValue().get(DOC_META_PREFIX + docId);
            if (metaJson != null) {
                return objectMapper.readValue(metaJson, Map.class);
            }
        } catch (Exception e) {
            System.err.println("读取文档元数据失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 列出所有文档
     */
    public List<Map<String, Object>> listDocuments() {
        List<Map<String, Object>> docs = new ArrayList<>();
        Set<String> docIds = redisTemplate.opsForSet().members(DOC_INDEX_KEY);
        if (docIds == null) return docs;
        for (String docId : docIds) {
            Map<String, Object> meta = getDocMeta(docId);
            if (meta != null) {
                docs.add(meta);
            }
        }
        return docs;
    }

    /**
     * 删除文档（向量 + 内容 + 元数据）
     */
    public void deleteDocument(String docId) {
        // 删除元数据
        redisTemplate.delete(DOC_META_PREFIX + docId);
        // 从索引中移除
        redisTemplate.opsForSet().remove(DOC_INDEX_KEY, docId);
        // 删除向量和内容
        Set<String> keys = redisTemplate.keys(VECTOR_KEY_PREFIX + docId + ":*");
        if (keys != null) {
            for (String key : keys) {
                String suffix = key.substring(VECTOR_KEY_PREFIX.length());
                redisTemplate.delete(key);
                redisTemplate.delete(DOC_CONTENT_PREFIX + suffix);
            }
        }
    }

    /**
     * 计算余弦相似度
     */
    private float cosineSimilarity(float[] a, float[] b) {
        float dotProduct = 0;
        float normA = 0;
        float normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dotProduct / (float)(Math.sqrt(normA) * Math.sqrt(normB) + 1e-8);
    }

    /**
     * 在向量库中检索最相似的 Top-K 个块
     * @param chunks 文档块列表
     * @param queryVector 查询向量
     * @param topK 返回数量
     */
    public List<SearchResult> search(List<DocumentChunker.DocumentChunk> chunks, float[] queryVector, int topK) {
        return search(chunks, queryVector, topK, 0.3f); // 默认阈值 0.3
    }
    
    /**
     * 在向量库中检索最相似的 Top-K 个块（带相似度阈值）
     * @param chunks 文档块列表
     * @param queryVector 查询向量
     * @param topK 返回数量
     * @param minSimilarity 最小相似度阈值（低于此值的结果将被过滤）
     */
    public List<SearchResult> search(List<DocumentChunker.DocumentChunk> chunks, float[] queryVector, int topK, float minSimilarity) {
        List<SearchResult> results = new ArrayList<>();
        for (DocumentChunker.DocumentChunk chunk : chunks) {
            if (chunk.getEmbedding() != null) {
                float similarity = cosineSimilarity(queryVector, chunk.getEmbedding());
                // 只保留超过阈值的结果
                if (similarity >= minSimilarity) {
                    results.add(new SearchResult(chunk, similarity));
                }
            }
        }
        
        // 按相似度降序排序
        List<SearchResult> sorted = results.stream()
                .sorted((a, b) -> Float.compare(b.similarity, a.similarity))
                .limit(topK)
                .collect(Collectors.toList());
        
        // 日志输出检索质量
        if (!sorted.isEmpty()) {
            System.out.printf("[RAG] 检索结果: %d 个 chunk, 相似度范围 [%.3f, %.3f], 阈值 %.3f%n",
                    sorted.size(),
                    sorted.get(sorted.size() - 1).similarity,
                    sorted.get(0).similarity,
                    minSimilarity);
        } else {
            System.out.printf("[RAG] 无检索结果: 所有 chunk 相似度低于阈值 %.3f%n", minSimilarity);
        }
        
        return sorted;
    }

    /**
     * 从 Redis 加载所有已存储的文档块
     */
    public List<DocumentChunker.DocumentChunk> loadAllChunks() {
        List<DocumentChunker.DocumentChunk> allChunks = new ArrayList<>();
        Set<String> vectorKeys = redisTemplate.keys(VECTOR_KEY_PREFIX + "*");
        if (vectorKeys == null) return allChunks;

        for (String vectorKey : vectorKeys) {
            try {
                String vectorJson = redisTemplate.opsForValue().get(vectorKey);
                if (vectorJson == null) {
                    continue;
                }
                float[] embedding = objectMapper.readValue(vectorJson, float[].class);
                if (embedding == null || embedding.length == 0) {
                    continue;
                }

                String suffix = vectorKey.substring(VECTOR_KEY_PREFIX.length());
                String docId;
                int idx;
                if (suffix.contains(":")) {
                    String[] parts = suffix.split(":", 2);
                    docId = parts[0];
                    idx = Integer.parseInt(parts[1]);
                } else if (suffix.contains("_")) {
                    int lastUnderscore = suffix.lastIndexOf('_');
                    docId = suffix.substring(0, lastUnderscore);
                    idx = Integer.parseInt(suffix.substring(lastUnderscore + 1));
                } else {
                    continue;
                }

                // 使用下划线格式（与 Redis 实际存储格式一致）
                String contentKey = DOC_CONTENT_PREFIX + docId + "_" + idx;
                String content = redisTemplate.opsForValue().get(contentKey);

                DocumentChunker.DocumentChunk chunk = new DocumentChunker.DocumentChunk(idx, content);
                chunk.setDocumentId(docId);
                chunk.setEmbedding(embedding);
                
                // 从文档级元数据获取标题（而非 chunk 级）
                String docTitle = null;
                try {
                    String docMetaKey = DOC_META_PREFIX + docId;
                    String docMetaJson = redisTemplate.opsForValue().get(docMetaKey);
                    if (docMetaJson != null) {
                        Map<String, Object> docMeta = objectMapper.readValue(docMetaJson, Map.class);
                        if (docMeta != null && docMeta.get("title") != null) {
                            docTitle = docMeta.get("title").toString();
                        }
                    }
                } catch (Exception me) {
                    // ignore meta parse errors
                }
                if (docTitle != null) {
                    chunk.setDocTitle(docTitle);
                }
                
                allChunks.add(chunk);
            } catch (Exception e) {
                // ignore parse errors
            }
        }
        return allChunks;
    }

    /**
     * 搜索结果封装
     */
    public static class SearchResult {
        public DocumentChunker.DocumentChunk chunk;
        public float similarity;

        public SearchResult(DocumentChunker.DocumentChunk chunk, float similarity) {
            this.chunk = chunk;
            this.similarity = similarity;
        }
    }
}