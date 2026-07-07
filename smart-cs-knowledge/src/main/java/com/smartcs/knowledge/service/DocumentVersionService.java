package com.smartcs.knowledge.service;

import com.smartcs.knowledge.entity.Document;
import com.smartcs.knowledge.entity.DocumentVersion;
import com.smartcs.knowledge.mapper.DocumentMapper;
import com.smartcs.knowledge.mapper.DocumentVersionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文档版本服务
 */
@Slf4j
@Service
public class DocumentVersionService {
    
    @Autowired
    private DocumentVersionMapper versionMapper;
    
    @Autowired
    private DocumentMapper documentMapper;
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private MinioService minioService;
    
    @Autowired
    private DocumentParserService parserService;
    
    @Autowired
    private TextChunkerService chunkerService;
    
    @Autowired
    private EmbeddingService embeddingService;
    
    @Autowired
    private VectorStoreService vectorStoreService;
    
    /**
     * 获取文档的所有版本
     */
    public List<DocumentVersion> getVersions(Long docId) {
        return versionMapper.findByDocId(docId);
    }
    
    /**
     * 获取特定版本
     */
    public DocumentVersion getVersion(Long docId, Integer version) {
        return versionMapper.findByDocIdAndVersion(docId, version);
    }
    
    /**
     * 上传新版本（归档当前版本 + 更新文档）
     */
    @Transactional
    public Map<String, Object> uploadNewVersion(Long docId, MultipartFile file, String changeSummary, Long userId) throws Exception {
        // 1. 获取当前文档
        Document doc = documentMapper.selectById(docId);
        if (doc == null) {
            throw new RuntimeException("文档不存在: " + docId);
        }
        
        // 2. 归档当前版本（保存当前文件信息到版本历史）
        archiveCurrentVersion(doc, changeSummary, userId);
        
        // 3. 使用 DocumentService 更新文档内容
        Map<String, Object> result = documentService.updateDocumentContent(docId, file);
        
        // 4. 创建新版本记录（指向新文件）
        createNewVersionRecord(docId, changeSummary != null ? changeSummary : "上传新版本", userId);
        
        return result;
    }
    
    /**
     * 回滚到指定版本
     */
    @Transactional
    public Map<String, Object> rollback(Long docId, Integer targetVersion, String changeSummary, Long userId) {
        // 1. 获取当前文档
        Document doc = documentMapper.selectById(docId);
        if (doc == null) {
            throw new RuntimeException("文档不存在: " + docId);
        }
        
        // 2. 获取目标版本
        DocumentVersion target = versionMapper.findByDocIdAndVersion(docId, targetVersion);
        if (target == null) {
            throw new RuntimeException("版本不存在: " + targetVersion);
        }
        
        // 3. 归档当前版本
        archiveCurrentVersion(doc, "回滚前归档", userId);
        
        // 4. 删除当前向量
        vectorStoreService.deleteByDocId("doc_" + docId);
        
        // 5. 从 MinIO 读取目标版本的文件并重新处理
        try {
            InputStream fileStream = minioService.getFile(target.getFileUrl());
            String content = parserService.parseStream(fileStream, doc.getFileName());
            
            if (content == null || content.isBlank()) {
                throw new RuntimeException("文档内容为空");
            }
            
            // 分块
            String docIdStr = "doc_" + docId;
            List<Map<String, Object>> chunks = chunkerService.chunkText(content, docIdStr, doc.getTitle());
            
            // 嵌入
            List<String> chunkTexts = chunks.stream()
                    .map(c -> (String) c.get("content"))
                    .collect(Collectors.toList());
            List<float[]> vectors = embeddingService.embedBatch(chunkTexts);
            
            // 存储向量
            vectorStoreService.saveVectors(chunks, vectors);
            
            // 更新文档记录
            doc.setFilePath(target.getFileUrl());
            doc.setFileSize(target.getFileSize());
            doc.setStatus("completed");
            doc.setChunkCount(chunks.size());
            doc.setTextLen((long) content.length());
            documentMapper.updateById(doc);
            
            log.info("文档回滚成功: docId={}, targetVersion={}, chunks={}", docId, targetVersion, chunks.size());
            
        } catch (Exception e) {
            log.error("文档回滚失败: docId={}, error={}", docId, e.getMessage(), e);
            doc.setStatus("failed");
            doc.setErrorMsg("回滚处理失败: " + e.getMessage());
            documentMapper.updateById(doc);
            throw new RuntimeException("回滚处理失败: " + e.getMessage(), e);
        }
        
        // 6. 创建新版本记录
        String summary = changeSummary != null ? changeSummary : "回滚到版本 " + targetVersion;
        createNewVersionRecord(docId, summary, userId);
        
        // 返回结果
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", docId);
        result.put("status", doc.getStatus());
        result.put("rolledBackFrom", targetVersion);
        result.put("success", true);
        result.put("message", "回滚成功");
        return result;
    }
    
    /**
     * 归档当前版本
     */
    private void archiveCurrentVersion(Document doc, String changeSummary, Long userId) {
        Integer maxVersion = versionMapper.findMaxVersion(doc.getId());
        int nextVersion = (maxVersion == null ? 0 : maxVersion) + 1;
        
        DocumentVersion version = new DocumentVersion();
        version.setDocId(doc.getId());
        version.setVersion(nextVersion);
        version.setDocIdVersion(doc.getId() + "_" + nextVersion);
        version.setFileUrl(doc.getFilePath());
        version.setFileSize(doc.getFileSize());
        version.setChangeSummary(changeSummary != null ? changeSummary : "自动归档");
        version.setCreatedAt(LocalDateTime.now());
        version.setCreatedBy(userId != null ? userId : 0L);
        
        versionMapper.insert(version);
        log.info("版本归档成功: docId={}, version={}", doc.getId(), nextVersion);
    }
    
    /**
     * 创建新版本记录
     */
    private void createNewVersionRecord(Long docId, String changeSummary, Long userId) {
        // 重新获取文档以获取最新文件信息
        Document doc = documentMapper.selectById(docId);
        
        Integer maxVersion = versionMapper.findMaxVersion(docId);
        int nextVersion = (maxVersion == null ? 0 : maxVersion) + 1;
        
        DocumentVersion version = new DocumentVersion();
        version.setDocId(docId);
        version.setVersion(nextVersion);
        version.setDocIdVersion(docId + "_" + nextVersion);
        version.setFileUrl(doc.getFilePath());
        version.setFileSize(doc.getFileSize());
        version.setChangeSummary(changeSummary);
        version.setCreatedAt(LocalDateTime.now());
        version.setCreatedBy(userId != null ? userId : 0L);
        
        versionMapper.insert(version);
        log.info("新版本记录创建成功: docId={}, version={}", docId, nextVersion);
    }
}
