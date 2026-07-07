package com.smartcs.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 对话会话管理服务
 * - 会话存储在 Redis 中
 * - 支持多轮对话（历史消息注入 prompt）
 * - 会话默认 30 分钟无操作自动过期
 */
@Service
public class ConversationService {

    private static final String SESSION_PREFIX = "rag:session:";
    private static final long SESSION_TTL_MINUTES = 30;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取或创建会话
     */
    public Session getOrCreateSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        String json = redisTemplate.opsForValue().get(key);
        if (json != null) {
            try {
                return objectMapper.readValue(json, Session.class);
            } catch (Exception e) {
                // 解析失败，创建新会话
            }
        }
        return new Session(sessionId);
    }

    /**
     * 保存会话
     */
    public void saveSession(Session session) {
        String key = SESSION_PREFIX + session.getId();
        try {
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(key, json, SESSION_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            System.err.println("保存会话失败: " + e.getMessage());
        }
    }

    /**
     * 添加用户消息到会话
     */
    public void addUserMessage(String sessionId, String content) {
        Session session = getOrCreateSession(sessionId);
        session.addMessage(new Message("user", content));
        // 刷新 TTL
        redisTemplate.expire(SESSION_PREFIX + sessionId, SESSION_TTL_MINUTES, TimeUnit.MINUTES);
        saveSession(session);
    }

    /**
     * 添加助手回复到会话
     */
    public void addAssistantMessage(String sessionId, String content) {
        Session session = getOrCreateSession(sessionId);
        session.addMessage(new Message("assistant", content));
        saveSession(session);
    }

    /**
     * 获取对话历史（转成 ChatML 格式列表，用于 LLM）
     */
    public List<Map<String, String>> getHistory(String sessionId) {
        Session session = getOrCreateSession(sessionId);
        return session.getMessages().stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .collect(Collectors.toList());
    }

    /**
     * 清除会话
     */
    public void clearSession(String sessionId) {
        redisTemplate.delete(SESSION_PREFIX + sessionId);
    }

    /**
     * 更新会话标题（取第一条用户消息的前20字）
     */
    public void updateSessionTitle(String sessionId, String title) {
        Session session = getOrCreateSession(sessionId);
        session.setTitle(title);
        saveSession(session);
    }

    /**
     * 获取会话元数据列表（用于列表展示）
     * 返回：id、标题、创建时间、最后访问时间、消息数
     */
    public List<SessionMeta> listSessionMetas() {
        Set<String> keys = redisTemplate.keys(SESSION_PREFIX + "*");
        if (keys == null || keys.isEmpty()) return Collections.emptyList();

        List<SessionMeta> metas = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Shanghai"));

        for (String key : keys) {
            String sessionId = key.substring(SESSION_PREFIX.length());
            try {
                String json = redisTemplate.opsForValue().get(key);
                if (json == null) continue;
                Session session = objectMapper.readValue(json, Session.class);
                String title = session.getTitle();
                if (title == null || title.isEmpty()) {
                    // 取第一条用户消息作为标题
                    for (Message m : session.getMessages()) {
                        if ("user".equals(m.getRole())) {
                            title = m.getContent().length() > 24
                                ? m.getContent().substring(0, 24) + "..."
                                : m.getContent();
                            break;
                        }
                    }
                    if (title == null || title.isEmpty()) title = "新对话";
                }
                String createdAt = fmt.format(Instant.ofEpochSecond(session.getCreatedTime()));
                String lastAccessAt = fmt.format(Instant.ofEpochSecond(session.getLastAccessTime()));
                metas.add(new SessionMeta(
                    session.getId(),
                    title,
                    createdAt,
                    lastAccessAt,
                    session.getMessages().size()
                ));
            } catch (Exception e) {
                // 跳过解析失败的
            }
        }

        // 按最后访问时间倒序
        metas.sort((a, b) -> Long.compare(
            parseLastAccess(b.lastAccessAt),
            parseLastAccess(a.lastAccessAt)
        ));

        return metas;
    }

    private long parseLastAccess(String ts) {
        try {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.of("Asia/Shanghai"))
                .parse(ts, Instant::from)
                .toEpochMilli();
        } catch (Exception e) {
            return 0;
        }
    }

    // ========== 内部类 ==========

    public static class SessionMeta {
        private String id;
        private String title;
        private String createdAt;
        private String lastAccessAt;
        private int messageCount;

        public SessionMeta() {}
        public SessionMeta(String id, String title, String createdAt, String lastAccessAt, int messageCount) {
            this.id = id; this.title = title; this.createdAt = createdAt;
            this.lastAccessAt = lastAccessAt; this.messageCount = messageCount;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getLastAccessAt() { return lastAccessAt; }
        public void setLastAccessAt(String lastAccessAt) { this.lastAccessAt = lastAccessAt; }
        public int getMessageCount() { return messageCount; }
        public void setMessageCount(int messageCount) { this.messageCount = messageCount; }
    }

    public static class Session {
        private String id;
        private String title; // 会话标题（可自定义）
        private long createdTime;
        private long lastAccessTime;
        private List<Message> messages;

        public Session() {}

        public Session(String id) {
            this.id = id;
            this.createdTime = Instant.now().getEpochSecond();
            this.lastAccessTime = this.createdTime;
            this.messages = new ArrayList<>();
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public long getCreatedTime() { return createdTime; }
        public void setCreatedTime(long createdTime) { this.createdTime = createdTime; }
        public long getLastAccessTime() { return lastAccessTime; }
        public void setLastAccessTime(long lastAccessTime) { this.lastAccessTime = lastAccessTime; }
        public List<Message> getMessages() { return messages; }
        public void setMessages(List<Message> messages) { this.messages = messages; }

        public void addMessage(Message message) {
            this.messages.add(message);
            this.lastAccessTime = Instant.now().getEpochSecond();
        }
    }

    public static class Message {
        private String role;
        private String content;

        public Message() {}

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
