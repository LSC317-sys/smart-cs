package com.smartcs.knowledge.controller;

import com.smartcs.knowledge.entity.DocumentVersion;
import com.smartcs.knowledge.service.DocumentVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档版本管理 Controller
 */
@RestController
@RequestMapping("/knowledge/documents")
public class DocumentVersionController {
    
    @Autowired
    private DocumentVersionService versionService;
    
    /**
     * 获取文档的所有版本
     */
    @GetMapping("/{id}/versions")
    public ResponseEntity<List<DocumentVersion>> getVersions(@PathVariable Long id) {
        List<DocumentVersion> versions = versionService.getVersions(id);
        return ResponseEntity.ok(versions);
    }
    
    /**
     * 获取特定版本
     */
    @GetMapping("/{id}/versions/{version}")
    public ResponseEntity<?> getVersion(
            @PathVariable Long id,
            @PathVariable Integer version) {
        DocumentVersion v = versionService.getVersion(id, version);
        if (v == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "版本不存在");
            error.put("docId", id);
            error.put("version", version);
            return ResponseEntity.status(404).body(error);
        }
        return ResponseEntity.ok(v);
    }
    
    /**
     * 上传新版本
     */
    @PostMapping("/{id}/update")
    public ResponseEntity<?> uploadNewVersion(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "changeSummary", required = false) String changeSummary,
            @RequestParam(value = "userId", required = false, defaultValue = "0") Long userId) {
        try {
            Map<String, Object> result = versionService.uploadNewVersion(id, file, changeSummary, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "上传失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * 回滚到指定版本
     */
    @PostMapping("/{id}/rollback")
    public ResponseEntity<?> rollback(
            @PathVariable Long id,
            @RequestParam("version") Integer targetVersion,
            @RequestParam(value = "changeSummary", required = false) String changeSummary,
            @RequestParam(value = "userId", required = false, defaultValue = "0") Long userId) {
        try {
            Map<String, Object> result = versionService.rollback(id, targetVersion, changeSummary, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "回滚失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
