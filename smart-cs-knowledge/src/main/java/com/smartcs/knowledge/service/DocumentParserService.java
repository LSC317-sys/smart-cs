package com.smartcs.knowledge.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

@Slf4j
@Service
public class DocumentParserService {

    private final Tika tika = new Tika();
    
    private static final Set<String> ALLOWED_TYPES = Set.of(
        "application/pdf",
        "application/x-pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain",
        "text/html",
        "text/markdown",
        "text/x-markdown",
        "text/csv",
        "application/octet-stream"
    );

    public String parseText(MultipartFile file) {
        String contentType = file.getContentType();
        String originalName = file.getOriginalFilename();
        
        log.info("Parsing file: {}, contentType: [{}], size: {}", originalName, contentType, file.getSize());
        
        // Infer content type from extension if not provided or generic
        if (contentType == null || contentType.equals("application/octet-stream")) {
            if (originalName != null) {
                String lower = originalName.toLowerCase();
                if (lower.endsWith(".txt")) contentType = "text/plain";
                else if (lower.endsWith(".html") || lower.endsWith(".htm")) contentType = "text/html";
                else if (lower.endsWith(".md")) contentType = "text/markdown";
                else if (lower.endsWith(".csv")) contentType = "text/csv";
                else if (lower.endsWith(".pdf")) contentType = "application/pdf";
                else if (lower.endsWith(".docx")) contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                else if (lower.endsWith(".doc")) contentType = "application/msword";
            }
            log.info("Inferred contentType from extension: {}", contentType);
        }
        
        // Handle text/* types directly, bypass Tika
        boolean isTextType = contentType != null && (contentType.equals("text/plain") || contentType.startsWith("text/"));
        log.info("Is text type: {}, contentType={}", isTextType, contentType);
        
        if (isTextType) {
            return parseAsPlainText(file, originalName);
        }
        
        if (contentType == null) {
            throw new RuntimeException("Cannot identify file type: " + originalName);
        }
        
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new RuntimeException("Unsupported file type: " + contentType);
        }

        try {
            String text = tika.parseToString(file.getInputStream());
            text = cleanText(text);
            log.info("Tika parsed: {}, length={}", originalName, text.length());
            return text;
        } catch (Exception e) {
            log.error("Tika error: type={}, msg={}", e.getClass().getName(), e.getMessage(), e);
            // Fallback to plain text
            log.info("Tika failed, trying plain text fallback");
            return parseAsPlainText(file, originalName);
        }
    }
    
    private String parseAsPlainText(MultipartFile file, String originalName) {
        log.info("Using plain text parsing via InputStream");
        try (InputStream is = file.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[8192];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
            String text = sb.toString();
            text = cleanText(text);
            log.info("Plain text parsed: {}, length={}", originalName, text.length());
            return text;
        } catch (Exception e) {
            log.error("Plain text read error: {}", e.getMessage(), e);
            throw new RuntimeException("Text read failed: " + e.getMessage(), e);
        }
    }

    public String parseStream(InputStream inputStream, String fileName) {
        try {
            String text = tika.parseToString(inputStream);
            text = cleanText(text);
            log.info("Stream parsed: {}, length={}", fileName, text.length());
            return text;
        } catch (Exception e) {
            log.error("Stream parse error: {}", e.getMessage(), e);
            throw new RuntimeException("Stream parse failed: " + e.getMessage(), e);
        }
    }

    private String cleanText(String text) {
        if (text == null) return "";
        return text
            .replaceAll("\u0000", "")
            .replaceAll("\\s+", " ")
            .replaceAll("\n{3,}", "\n\n")
            .trim();
    }

    public Set<String> getAllowedTypes() {
        return ALLOWED_TYPES;
    }
}