package com.smartcs.knowledge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 文本分块工具服务
 */
@Slf4j
@Service
public class TextChunkerService {

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_CHUNK_OVERLAP = 50;

    /**
     * 按字数分块，保留段落边界；超长段落强制切分
     */
    public List<Map<String, Object>> chunkText(String text, String docId, String docTitle) {
        List<Map<String, Object>> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        // 清理空白字符
        text = text.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");

        // 先按段落分割
        String[] paragraphs = text.split("\\n\\s*\\n");

        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        String titlePrefix = "[" + docTitle + "] ";

        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) continue;

            // 如果单个段落超过分块大小，先强制切分该段落
            if (para.length() > DEFAULT_CHUNK_SIZE) {
                // 先保存当前累积的内容
                if (currentChunk.length() > 0) {
                    String chunkText = currentChunk.toString().trim();
                    if (!chunkText.isEmpty()) {
                        chunks.add(buildChunk(chunkText, docId, docTitle, titlePrefix, chunkIndex++));
                    }
                    currentChunk = new StringBuilder();
                }
                // 强制切分超长段落
                List<String> subChunks = splitLongText(para, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
                for (String sub : subChunks) {
                    chunks.add(buildChunk(sub, docId, docTitle, titlePrefix, chunkIndex++));
                }
                continue;
            }

            // 如果当前段落加上现有内容超过分块大小，先保存当前的
            if (currentChunk.length() + para.length() + 1 > DEFAULT_CHUNK_SIZE && currentChunk.length() > 0) {
                String chunkText = currentChunk.toString().trim();
                if (!chunkText.isEmpty()) {
                    chunks.add(buildChunk(chunkText, docId, docTitle, titlePrefix, chunkIndex++));
                }
                // 滑动窗口：保留最后overlap字符作为下一个chunk的开头
                String overlap = currentChunk.length() > DEFAULT_CHUNK_OVERLAP
                    ? currentChunk.substring(currentChunk.length() - DEFAULT_CHUNK_OVERLAP)
                    : currentChunk.toString();
                currentChunk = new StringBuilder(overlap.trim());
            }

            currentChunk.append(para).append("\n");
        }

        // 最后一块
        if (currentChunk.length() > 0) {
            String chunkText = currentChunk.toString().trim();
            if (!chunkText.isEmpty()) {
                chunks.add(buildChunk(chunkText, docId, docTitle, titlePrefix, chunkIndex));
            }
        }

        log.info("文本分块完成: 文档ID={}, 总段数={}, 分块数={}", docId, paragraphs.length, chunks.size());
        return chunks;
    }

    /**
     * 强制切分超长文本（按固定大小+重叠窗口）
     * 尝试在句子边界处切分，避免切断句子
     */
    private List<String> splitLongText(String text, int chunkSize, int overlap) {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            // 如果不是最后一块，尝试在句子边界处切分
            if (end < text.length()) {
                int lastSentenceEnd = findLastSentenceEnd(text, start, end);
                if (lastSentenceEnd > start) {
                    end = lastSentenceEnd;
                }
            }
            result.add(text.substring(start, end).trim());
            if (end >= text.length()) break;
            // 下一块从 end-overlap 开始，确保前进（至少比 start 多走1个字符）
            int nextStart = end - overlap;
            if (nextStart <= start) {
                nextStart = start + 1; // 安全保底：确保前进
            }
            start = nextStart;
        }
        return result;
    }

    /**
     * 在 [start, end] 范围内找最后一个句子结束位置（。！？.!?换行等）
     */
    private int findLastSentenceEnd(String text, int start, int end) {
        // 在end附近（后20%范围内）找句子边界
        int searchStart = start + (end - start) * 80 / 100;
        for (int i = end - 1; i >= searchStart; i--) {
            char c = text.charAt(i);
            if (c == '\u3002' || c == '\uff01' || c == '\uff1f'  // 。！？
                || c == '.' || c == '!' || c == '?'
                || c == '\n' || c == '\uff1b' || c == ';') {      // ；
                return i + 1; // 包含标点
            }
        }
        return end; // 没找到句子边界，在原位置切分
    }

    private Map<String, Object> buildChunk(String chunkText, String docId, String docTitle,
                                            String titlePrefix, int chunkIndex) {
        Map<String, Object> chunk = new HashMap<>();
        chunk.put("chunkId", docId + "_" + chunkIndex);
        chunk.put("docId", docId);
        chunk.put("docTitle", docTitle);
        chunk.put("content", titlePrefix + chunkText);
        chunk.put("chunkIndex", chunkIndex);
        return chunk;
    }
}
