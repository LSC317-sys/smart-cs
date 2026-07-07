package com.smartcs.chat.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档切分服务
 * 将长文档按语义切分为小块，适合向量检索
 */
@Service
public class DocumentChunker {

    /**
     * 按段落切分文档
     * @param text 原始文本
     * @param maxChars 每段最大字符数
     * @param overlap 重叠字符数（保留上下文）
     * @return 切分后的文本块列表
     */
    public List<DocumentChunk> chunkByParagraph(String text, int maxChars, int overlap) {
        List<DocumentChunk> chunks = new ArrayList<>();

        // 按换行符分割段落
        String[] paragraphs = text.split("\n");

        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;

        for (String paragraph : paragraphs) {
            // 跳过空白段落
            if (paragraph.trim().isEmpty()) continue;

            // 如果当前块加上这个段落会超出限制
            if (currentChunk.length() + paragraph.length() + 1 > maxChars) {
                // 保存当前块
                if (currentChunk.length() > 0) {
                    chunks.add(new DocumentChunk(
                        chunkIndex++,
                        currentChunk.toString().trim()
                    ));

                    // 保留 overlap 长度的内容作为下一块的起始（后退一点保持连贯）
                    int startPos = Math.max(0, currentChunk.length() - overlap);
                    String tail = currentChunk.substring(startPos);
                    currentChunk = new StringBuilder(tail);
                }

                // 如果单个段落就超过限制，强制切分
                if (paragraph.length() > maxChars) {
                    for (int i = 0; i < paragraph.length(); i += maxChars - overlap) {
                        int end = Math.min(i + maxChars, paragraph.length());
                        chunks.add(new DocumentChunk(chunkIndex++, paragraph.substring(i, end)));
                    }
                } else {
                    currentChunk.append(paragraph).append("\n");
                }
            } else {
                currentChunk.append(paragraph).append("\n");
            }
        }

        // 保存最后一个块
        if (currentChunk.length() > 0) {
            chunks.add(new DocumentChunk(chunkIndex, currentChunk.toString().trim()));
        }

        return chunks;
    }

    /**
     * 按固定字符数切分（简单粗暴版）
     */
    public List<DocumentChunk> chunkByFixedSize(String text, int chunkSize) {
        List<DocumentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, text.length());
            chunks.add(new DocumentChunk(i / chunkSize, text.substring(i, end)));
        }
        return chunks;
    }

    /**
     * 文档块数据结构
     */
    public static class DocumentChunk {
        private int index;
        private String content;
        private float[] embedding;
        private String documentId;
        private String docTitle;

        public DocumentChunk(int index, String content) {
            this.index = index;
            this.content = content;
        }

        // Getters and Setters
        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public float[] getEmbedding() { return embedding; }
        public void setEmbedding(float[] embedding) { this.embedding = embedding; }
        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }
        public String getDocTitle() { return docTitle; }
        public void setDocTitle(String docTitle) { this.docTitle = docTitle; }
    }
}