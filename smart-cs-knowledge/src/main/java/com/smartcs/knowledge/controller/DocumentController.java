package com.smartcs.knowledge.controller;

import com.smartcs.knowledge.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档管理 API
 */
@Slf4j
@RestController
@RequestMapping("/knowledge")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * 读取文件字节（避免 Tomcat temp 文件被并发清理问题）
     */
    private MultipartFile wrapFile(MultipartFile file) {
        byte[] fileBytes;
        String originalFilename = file.getOriginalFilename();
        String originalContentType = file.getContentType();
        try {
            fileBytes = file.getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file bytes: " + e.getMessage(), e);
        }
        final byte[] bytes = fileBytes;
        return new MultipartFile() {
            @Override public String getName() { return file.getName(); }
            @Override public String getOriginalFilename() { return originalFilename; }
            @Override public String getContentType() { return originalContentType; }
            @Override public boolean isEmpty() { return bytes.length == 0; }
            @Override public long getSize() { return bytes.length; }
            @Override public byte[] getBytes() { return bytes; }
            @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
            @Override public void transferTo(java.io.File dest) throws java.io.IOException {
                java.nio.file.Files.write(dest.toPath(), bytes);
            }
        };
    }

    /**
     * 上传单个文档（自动解析+向量化）
     */
    @PostMapping("/documents/upload")
    public Map<String, Object> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "kbId", required = false, defaultValue = "1") Long kbId) {
        log.info("Received file: {}, size: {}, kbId: {}", file.getOriginalFilename(), file.getSize(), kbId);
        return documentService.uploadDocument(wrapFile(file), title, kbId);
    }

    /**
     * 批量上传文档
     */
    @PostMapping("/documents/batch-upload")
    public Map<String, Object> batchUpload(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "titles", required = false) String titlesJson,
            @RequestParam(value = "kbId", required = false, defaultValue = "1") Long kbId) {
        log.info("Batch upload: {} files, kbId: {}", files.length, kbId);

        // 解析可选的标题列表（JSON 数组，与 files 一一对应）
        List<String> titles = new ArrayList<>();
        if (titlesJson != null && !titlesJson.isBlank()) {
            try {
                titles = Arrays.asList(new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(titlesJson, String[].class));
            } catch (Exception e) {
                log.warn("titles JSON 解析失败: {}", e.getMessage());
            }
        }

        List<Map<String, Object>> results = new ArrayList<>();
        List<Map<String, Object>> failures = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            String title = (titles.size() > i && titles.get(i) != null) ? titles.get(i) : null;
            try {
                Map<String, Object> result = documentService.uploadDocument(wrapFile(file), title, kbId);
                result.put("index", i);
                result.put("fileName", file.getOriginalFilename());
                results.add(result);
                log.info("Batch[{}] OK: {}", i, file.getOriginalFilename());
            } catch (Exception e) {
                log.error("Batch[{}] FAIL: {} - {}", i, file.getOriginalFilename(), e.getMessage());
                failures.add(Map.of(
                    "index", i,
                    "fileName", file.getOriginalFilename(),
                    "fileSize", file.getSize(),
                    "error", e.getMessage() != null ? e.getMessage() : "处理失败"
                ));
            }
        }

        int total = files.length;
        int success = results.size();
        int failed = failures.size();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", total);
        response.put("success", success);
        response.put("failed", failed);
        response.put("results", results);
        if (!failures.isEmpty()) response.put("failures", failures);
        response.put("message", String.format("批量上传完成：%d/%d 成功", success, total));
        return response;
    }

    /**
     * 文档列表
     */
    @GetMapping("/documents")
    public List<Map<String, Object>> listDocuments(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "kbId", required = false) Long kbId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return documentService.listDocuments(status, kbId, page, size);
    }

    /**
     * 文档详情
     */
    @GetMapping("/documents/{id}")
    public Map<String, Object> getDocument(@PathVariable(name = "id") Long id) {
        return documentService.getDocument(id);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/documents/{id}")
    public Map<String, Object> deleteDocument(@PathVariable(name = "id") Long id) {
        documentService.deleteDocument(id);
        return Map.of("success", true, "id", id, "message", "文档已删除");
    }

    /**
     * 批量删除文档
     */
    @PostMapping("/documents/batch-delete")
    public Map<String, Object> batchDelete(@RequestBody Map<String, Object> body) {
        List<?> idsRaw = (List<?>) body.get("ids");
        if (idsRaw == null || idsRaw.isEmpty()) {
            return Map.of("success", false, "message", "未提供要删除的文档 ID 列表");
        }
        List<Long> ids = idsRaw.stream()
                .map(o -> {
                    if (o instanceof Number) return ((Number) o).longValue();
                    if (o instanceof String) return Long.parseLong((String) o);
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int deleted = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        for (Long id : ids) {
            try {
                documentService.deleteDocument(id);
                deleted++;
            } catch (Exception e) {
                failed++;
                errors.add("doc_" + id + ": " + e.getMessage());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deleted", deleted);
        result.put("failed", failed);
        result.put("total", ids.size());
        result.put("message", String.format("删除完成：%d/%d 成功", deleted, ids.size()));
        if (!errors.isEmpty()) result.put("errors", errors);
        return result;
    }

    /**
     * 获取文档内容（用于预览）
     */
    @GetMapping("/documents/{id}/content")
    public Map<String, Object> getDocumentContent(@PathVariable(name = "id") Long id) {
        return documentService.getDocumentContent(id);
    }

    /**
     * 全量统计
     */
    @GetMapping("/stats")
    public Map<String, Object> stats(@RequestParam(value = "kbId", required = false) Long kbId) {
        return documentService.stats(kbId);
    }

    /**
     * 支持的文件类型
     */
    @GetMapping("/types")
    public Map<String, Object> allowedTypes() {
        return Map.of("allowedTypes",
            List.of("PDF", "DOC", "DOCX", "XLS", "XLSX", "TXT", "HTML", "MD", "CSV"));
    }
    
    /**
     * 临时接口：回填 text_len 字段（精确方式）
     * 从 MinIO 重新下载文档并解析，计算真实文本长度
     */
    @PostMapping("/admin/backfill-text-len")
    public Map<String, Object> backfillTextLen() {
        return documentService.backfillTextLen();
    }

    /**
     * 更新文档标题
     */
    @PatchMapping("/documents/{id}/title")
    public Map<String, Object> updateTitle(
            @PathVariable(name = "id") Long id,
            @RequestBody Map<String, String> body) {
        String title = body.get("title");
        if (title == null || title.isBlank()) {
            return Map.of("success", false, "message", "标题不能为空");
        }
        return documentService.updateDocumentTitle(id, title);
    }

    /**
     * 更新文档内容（重新上传并重新索引）
     */
    @PutMapping("/documents/{id}/content")
    public Map<String, Object> updateContent(
            @PathVariable(name = "id") Long id,
            @RequestParam("file") MultipartFile file) {
        log.info("Update document content: id={}, file={}", id, file.getOriginalFilename());
        return documentService.updateDocumentContent(id, wrapFile(file));
    }
}