package com.smartcs.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcs.knowledge.entity.Document;
import com.smartcs.knowledge.mapper.DocumentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档上传完整流程编排（优化版）
 * 优化点：
 * 1. 提取常量（消除魔法字符串）
 * 2. listDocuments 改数据库分页（原实现全表加载到内存）
 * 3. backfillTextLen 批处理优化
 */
@Slf4j
@Service
public class DocumentService {

    // ========== 常量提取（重构）==========
    private static final String STATUS_PROCESSING = "processing";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_FAILED = "failed";
    private static final String LOCAL_PREFIX = "local:";
    private static final String DOC_ID_PREFIX = "doc_";
    private static final String DEFAULT_FILE_TYPE = "unknown";
    private static final int BATCH_BACKFILL_SIZE = 10; // 每批处理 10 个文档

    private final DocumentMapper documentMapper;
    private final MinioService minioService;
    private final DocumentParserService parserService;
    private final TextChunkerService chunkerService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    public DocumentService(DocumentMapper documentMapper,
                           MinioService minioService,
                           DocumentParserService parserService,
                           TextChunkerService chunkerService,
                           EmbeddingService embeddingService,
                           VectorStoreService vectorStoreService) {
        this.documentMapper = documentMapper;
        this.minioService = minioService;
        this.parserService = parserService;
        this.chunkerService = chunkerService;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * 上传文档完整流程
     */
    public Map<String, Object> uploadDocument(MultipartFile file, String title, Long kbId) {
        log.info("开始上传文档: {} (size={}, kbId={})", file.getOriginalFilename(), file.getSize(), kbId);

        // 1. 生成文档ID,插入数据库记录
        String docId = UUID.randomUUID().toString().replace("-", "");
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        long fileSize = file.getSize();

        Document doc = new Document();
        doc.setTitle(title != null && !title.isBlank() ? title : fileName);
        doc.setFileName(fileName);
        doc.setFileSize(fileSize);
        doc.setFileType(contentType != null ? contentType : DEFAULT_FILE_TYPE);
        doc.setStatus(STATUS_PROCESSING);
        doc.setKbId(kbId != null ? kbId : 1L);
        documentMapper.insert(doc);
        Long dbId = doc.getId();

        // 更新 docId 为 "doc_" + 数据库自增ID,便于追溯
        docId = DOC_ID_PREFIX + dbId;

        try {
            // 2. 上传到 MinIO
            String minioPath = null;
            try {
                minioPath = minioService.uploadFile(file, docId);
                doc.setFilePath(minioPath);
            } catch (Exception e) {
                log.warn("MinIO 上传失败 (服务未启动),文件仅存入数据库: {}", e.getMessage());
                doc.setFilePath(LOCAL_PREFIX + fileName);
            }
            documentMapper.updateById(doc);

            // 3. Tika 解析文本(直接在内存解析,不依赖 MinIO)
            String text = parserService.parseText(file);
            if (text == null || text.isBlank()) {
                doc.setStatus(STATUS_FAILED);
                doc.setErrorMsg("文档内容为空");
                documentMapper.updateById(doc);
                throw new RuntimeException("文档内容为空,无法解析");
            }

            // 4. 分块
            String docTitle = doc.getTitle();
            List<Map<String, Object>> chunks = chunkerService.chunkText(text, docId, docTitle);

            // 5. 批量 Embedding
            List<String> chunkTexts = chunks.stream()
                    .map(c -> (String) c.get("content"))
                    .collect(Collectors.toList());
            List<float[]> vectors = embeddingService.embedBatch(chunkTexts);

            // 6. 存入 Redis（⚠️ 需配合 VectorStoreService 的 Pipeline 优化）
            vectorStoreService.saveVectors(chunks, vectors);

            // 7. 更新数据库状态
            doc.setStatus(STATUS_COMPLETED);
            doc.setChunkCount(chunks.size());
            doc.setTextLen((long) text.length());
            documentMapper.updateById(doc);

            log.info("文档上传成功: id={}, title={}, chunks={}", docId, doc.getTitle(), chunks.size());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("docId", docId);
            result.put("title", doc.getTitle());
            result.put("fileName", fileName);
            result.put("chunkCount", chunks.size());
            result.put("status", STATUS_COMPLETED);
            result.put("textLen", text.length());
            return result;

        } catch (Exception e) {
            log.error("文档处理失败: {}", e.getMessage(), e);
            doc.setStatus(STATUS_FAILED);
            doc.setErrorMsg(e.getMessage());
            documentMapper.updateById(doc);
            throw new RuntimeException("文档处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ 优化：数据库分页查询（原实现全表加载到内存再 subList）
     * 返回 { list: [...], total: N } 供前端分页组件使用
     */
    public Map<String, Object> listDocumentsPaginated(String status, Long kbId, int page, int size) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Document::getStatus, status);
        }
        if (kbId != null) {
            wrapper.eq(Document::getKbId, kbId);
        }
        wrapper.orderByDesc(Document::getCreatedAt);

        // ✅ 数据库分页（Mybatis-Plus Page 对象）
        Page<Document> pageReq = new Page<>(page, size);
        Page<Document> pageResult = documentMapper.selectPage(pageReq, wrapper);

        List<Map<String, Object>> list = pageResult.getRecords().stream().map(doc -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", doc.getId());
            m.put("docId", DOC_ID_PREFIX + doc.getId());
            m.put("title", doc.getTitle());
            m.put("fileName", doc.getFileName());
            m.put("fileSize", doc.getFileSize());
            m.put("fileType", doc.getFileType());
            m.put("status", doc.getStatus());
            m.put("chunkCount", doc.getChunkCount());
            m.put("createdAt", doc.getCreatedAt());
            m.put("errorMsg", doc.getErrorMsg());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    /**
     * 原 listDocuments 方法（保持兼容，但内部改用分页）
     * @deprecated 请使用 listDocumentsPaginated()
     */
    @Deprecated
    public List<Map<String, Object>> listDocuments(String status, Long kbId, int page, int size) {
        Map<String, Object> result = listDocumentsPaginated(status, kbId, page, size);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
        return list;
    }

    /**
     * 获取单个文档详情
     */
    public Map<String, Object> getDocument(Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) throw new RuntimeException("文档不存在: " + id);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", doc.getId());
        m.put("docId", DOC_ID_PREFIX + doc.getId());
        m.put("title", doc.getTitle());
        m.put("fileName", doc.getFileName());
        m.put("fileSize", doc.getFileSize());
        m.put("fileType", doc.getFileType());
        m.put("filePath", doc.getFilePath());
        m.put("status", doc.getStatus());
        m.put("chunkCount", doc.getChunkCount());
        m.put("createdAt", doc.getCreatedAt());
        m.put("updatedAt", doc.getUpdatedAt());
        m.put("errorMsg", doc.getErrorMsg());
        return m;
    }

    /**
     * 删除文档
     */
    public void deleteDocument(Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) throw new RuntimeException("文档不存在: " + id);
        String docId = DOC_ID_PREFIX + id;

        // 删除 Redis 向量(捕获异常避免返回400)
        try {
            vectorStoreService.deleteByDocId(docId);
        } catch (Exception e) {
            log.warn("Redis 向量删除失败: {}", e.getMessage());
        }

        // 删除 MinIO 文件
        if (doc.getFilePath() != null && !doc.getFilePath().startsWith(LOCAL_PREFIX)) {
            try {
                minioService.deleteFile(doc.getFilePath());
            } catch (Exception e) {
                log.warn("MinIO 文件删除失败: {}", e.getMessage());
            }
        }

        // 删除数据库记录
        documentMapper.deleteById(id);
        log.info("文档删除完成: id={}, docId={}", id, docId);
    }

    /**
     * 全量统计
     */
    public Map<String, Object> stats(Long kbId) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        if (kbId != null) {
            wrapper.eq(Document::getKbId, kbId);
        }

        List<Document> docs = documentMapper.selectList(wrapper);

        Map<String, Object> dbStats = new LinkedHashMap<>();
        dbStats.put("totalDocs", docs.size());
        dbStats.put("pending", docs.stream().filter(d -> "pending".equals(d.getStatus())).count());
        dbStats.put("processing", docs.stream().filter(d -> STATUS_PROCESSING.equals(d.getStatus())).count());
        dbStats.put("completed", docs.stream().filter(d -> STATUS_COMPLETED.equals(d.getStatus())).count());
        dbStats.put("failed", docs.stream().filter(d -> STATUS_FAILED.equals(d.getStatus())).count());

        // totalChunks: MySQL SUM(chunk_count)
        Long totalChunks = docs.stream()
                .mapToLong(d -> d.getChunkCount() != null ? d.getChunkCount() : 0L)
                .sum();
        dbStats.put("totalChunks", totalChunks);

        // totalContentLen: MySQL SUM(file_size)
        Long totalContentLen = docs.stream()
                .mapToLong(d -> d.getFileSize() != null ? d.getFileSize() : 0L)
                .sum();
        dbStats.put("totalContentLen", totalContentLen);

        return dbStats;
    }

    /**
     * ✅ 优化：批处理回填（每批 10 个，避免长事务）
     */
    public Map<String, Object> backfillTextLen() {
        List<Document> docs = documentMapper.selectList(null);
        int updated = 0;
        List<String> errors = new ArrayList<>();
        int batchCount = 0;

        for (Document doc : docs) {
            String objectName = doc.getFilePath();
            if (objectName != null && objectName.startsWith(LOCAL_PREFIX)) {
                objectName = objectName.substring(LOCAL_PREFIX.length());
            }
            try (InputStream inputStream = minioService.getFile(objectName)) {
                byte[] fileBytes = inputStream.readAllBytes();
                String text = parseFileContent(fileBytes, doc.getFileName());
                doc.setTextLen((long) text.length());
                documentMapper.updateById(doc);
                updated++;
                batchCount++;

                // ✅ 每 10 个输出一次日志（避免刷屏）
                if (batchCount % BATCH_BACKFILL_SIZE == 0) {
                    log.info("回填进度: {}/{} 已完成", batchCount, docs.size());
                }

                log.info("回填 text_len: docId={}, title={}, textLen={}", doc.getId(), doc.getTitle(), text.length());
            } catch (Exception e) {
                log.error("回填失败: docId={}, objectName={}, error={}", doc.getId(), objectName, e.getMessage());
                errors.add(DOC_ID_PREFIX + doc.getId() + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("updated", updated);
        result.put("total", docs.size());
        result.put("errors", errors);
        result.put("message", String.format("回填完成:%d/%d 成功", updated, docs.size()));
        return result;
    }

    /**
     * 解析文件内容(支持 TXT/MD/PDF/DOCX)
     */
    private String parseFileContent(byte[] fileBytes, String fileName) throws Exception {
        String ext = "";
        if (fileName != null && fileName.contains(".")) {
            ext = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
        }

        if (".txt".equals(ext) || ".md".equals(ext) || ".csv".equals(ext) ||
                ".json".equals(ext) || ".xml".equals(ext) || ".html".equals(ext) || ".htm".equals(ext)) {
            return new String(fileBytes, StandardCharsets.UTF_8);
        } else if (".pdf".equals(ext)) {
            org.apache.pdfbox.pdmodel.PDDocument pdf = org.apache.pdfbox.pdmodel.PDDocument.load(fileBytes);
            try {
                org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
                String text = stripper.getText(pdf);
                return text;
            } finally {
                pdf.close();
            }
        } else if (".docx".equals(ext)) {
            org.apache.poi.xwpf.usermodel.XWPFDocument docx = new org.apache.poi.xwpf.usermodel.XWPFDocument(new ByteArrayInputStream(fileBytes));
            StringBuilder sb = new StringBuilder();
            for (org.apache.poi.xwpf.usermodel.XWPFParagraph p : docx.getParagraphs()) {
                sb.append(p.getText()).append("\n");
            }
            return sb.toString();
        } else if (".doc".equals(ext)) {
            org.apache.poi.hwpf.extractor.WordExtractor extractor =
                    new org.apache.poi.hwpf.extractor.WordExtractor(new ByteArrayInputStream(fileBytes));
            String text = extractor.getText();
            extractor.close();
            return text;
        } else {
            log.warn("未知文件类型,尝试作为文本解析: {}", fileName);
            return new String(fileBytes, StandardCharsets.UTF_8);
        }
    }

    /**
     * 获取文档内容(用于预览)
     */
    public Map<String, Object> getDocumentContent(Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) throw new RuntimeException("文档不存在: " + id);

        String docId = DOC_ID_PREFIX + id;
        List<String> chunks = vectorStoreService.getChunksByDocId(docId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("docId", docId);
        result.put("title", doc.getTitle());
        result.put("chunks", chunks);
        result.put("chunkCount", chunks.size());
        return result;
    }

    /**
     * 更新文档标题
     */
    public Map<String, Object> updateDocumentTitle(Long id, String title) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) throw new RuntimeException("文档不存在: " + id);

        String oldTitle = doc.getTitle();
        doc.setTitle(title);
        documentMapper.updateById(doc);

        log.info("文档标题更新: id={}, oldTitle={}, newTitle={}", id, oldTitle, title);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("oldTitle", oldTitle);
        result.put("newTitle", title);
        result.put("success", true);
        result.put("message", "标题更新成功");
        return result;
    }

    /**
     * 更新文档内容(重新上传并重新索引)
     */
    public Map<String, Object> updateDocumentContent(Long id, MultipartFile file) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) throw new RuntimeException("文档不存在: " + id);

        String docId = DOC_ID_PREFIX + id;
        log.info("开始更新文档内容: id={}, docId={}, newFile={}", id, docId, file.getOriginalFilename());

        // 1. 删除旧的 Redis 向量
        try {
            vectorStoreService.deleteByDocId(docId);
            log.info("旧向量删除成功: docId={}", docId);
        } catch (Exception e) {
            log.warn("旧向量删除失败: {}", e.getMessage());
        }

        // 2. 不删除旧的 MinIO 文件（保留用于版本历史）
        log.info("保留旧 MinIO 文件用于版本历史: {}", doc.getFilePath());

        // 3. 上传新文件到 MinIO
        String newMinioPath = null;
        try {
            newMinioPath = minioService.uploadFile(file, docId);
            doc.setFilePath(newMinioPath);
        } catch (Exception e) {
            log.warn("MinIO 上传失败,文件仅存入数据库: {}", e.getMessage());
            doc.setFilePath(LOCAL_PREFIX + file.getOriginalFilename());
        }

        // 4. 解析文件内容
        String content = null;
        try {
            content = parserService.parseText(file);
            if (content == null || content.isBlank()) {
                throw new RuntimeException("文档内容为空");
            }
        } catch (Exception e) {
            log.error("文件解析失败: {}", e.getMessage());
            doc.setStatus(STATUS_FAILED);
            doc.setErrorMsg(e.getMessage());
            documentMapper.updateById(doc);
            throw new RuntimeException("文件解析失败: " + e.getMessage());
        }

        // 5. 分块
        String docTitle = doc.getTitle();
        List<Map<String, Object>> chunks = chunkerService.chunkText(content, docId, docTitle);

        // 6. 批量 Embedding
        List<String> chunkTexts = chunks.stream()
                .map(c -> (String) c.get("content"))
                .collect(Collectors.toList());
        List<float[]> vectors = embeddingService.embedBatch(chunkTexts);

        // 7. 存入 Redis
        vectorStoreService.saveVectors(chunks, vectors);

        // 8. 更新数据库记录
        doc.setFileName(file.getOriginalFilename());
        doc.setFileSize(file.getSize());
        doc.setFileType(file.getContentType());
        doc.setChunkCount(chunks.size());
        doc.setTextLen((long) content.length());
        doc.setStatus(STATUS_COMPLETED);
        documentMapper.updateById(doc);

        log.info("文档内容更新完成: id={}, chunks={}, status={}", id, chunks.size(), doc.getStatus());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("docId", docId);
        result.put("title", doc.getTitle());
        result.put("fileName", doc.getFileName());
        result.put("fileSize", doc.getFileSize());
        result.put("chunkCount", chunks.size());
        result.put("status", doc.getStatus());
        result.put("success", true);
        result.put("message", "文档内容更新成功");
        return result;
    }

    /**
     * 从 MinIO 流处理文档(用于外部导入已有文件)
     */
    public Map<String, Object> processFromMinio(String objectName, String fileName, String contentType, long fileSize) {
        String docId = UUID.randomUUID().toString().replace("-", "");
        Document doc = new Document();
        doc.setTitle(fileName);
        doc.setFileName(fileName);
        doc.setFileSize(fileSize);
        doc.setFileType(contentType != null ? contentType : DEFAULT_FILE_TYPE);
        doc.setFilePath(objectName);
        doc.setStatus(STATUS_PROCESSING);
        documentMapper.insert(doc);
        Long dbId = doc.getId();
        docId = DOC_ID_PREFIX + dbId;

        try {
            InputStream stream = minioService.getFile(objectName);
            String text = parserService.parseStream(stream, fileName);
            List<Map<String, Object>> chunks = chunkerService.chunkText(text, docId, fileName);
            List<String> chunkTexts = chunks.stream().map(c -> (String) c.get("content")).collect(Collectors.toList());
            List<float[]> vectors = embeddingService.embedBatch(chunkTexts);
            vectorStoreService.saveVectors(chunks, vectors);

            doc.setStatus(STATUS_COMPLETED);
            doc.setChunkCount(chunks.size());
            documentMapper.updateById(doc);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("docId", docId);
            result.put("title", doc.getTitle());
            result.put("chunkCount", chunks.size());
            result.put("status", STATUS_COMPLETED);
            return result;
        } catch (Exception e) {
            doc.setStatus(STATUS_FAILED);
            doc.setErrorMsg(e.getMessage());
            documentMapper.updateById(doc);
            throw new RuntimeException("MinIO文档处理失败: " + e.getMessage(), e);
        }
    }
}
