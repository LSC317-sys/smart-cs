-- ========== SmartCS 数据库索引创建脚本 ==========
-- 生成时间：2026-05-23
-- 依据：设计文档 v3.0 第 9.1 章

-- ========== users 表索引 ==========
CREATE INDEX idx_users_username ON smart_cs.users(username);
CREATE INDEX idx_users_email ON smart_cs.users(email);
CREATE INDEX idx_users_role ON smart_cs.users(role);
CREATE INDEX idx_users_created_at_role ON smart_cs.users(created_at, role);

-- ========== knowledge_base 表索引 ==========
CREATE INDEX idx_kb_created_by ON smart_cs.knowledge_base(created_by);
CREATE FULLTEXT INDEX idx_kb_name_ft ON smart_cs.knowledge_base(name);

-- ========== documents 表索引 ==========
CREATE INDEX idx_docs_kb_id ON smart_cs.documents(kb_id);
CREATE INDEX idx_docs_status ON smart_cs.documents(status);
CREATE INDEX idx_docs_kb_status ON smart_cs.documents(kb_id, status);
CREATE FULLTEXT INDEX idx_docs_title_ft ON smart_cs.documents(title);

-- ========== conversations 表索引 ==========
CREATE INDEX idx_conv_user_id ON smart_cs.conversations(user_id);
CREATE INDEX idx_conv_updated_at ON smart_cs.conversations(updated_at);
CREATE INDEX idx_conv_user_updated ON smart_cs.conversations(user_id, updated_at DESC);

-- ========== messages 表索引 ==========
CREATE INDEX idx_msgs_conv_id ON smart_cs.messages(conversation_id);
CREATE INDEX idx_msgs_feedback ON smart_cs.messages(feedback);
CREATE INDEX idx_msgs_conv_created ON smart_cs.messages(conversation_id, created_at);

-- ========== 验证索引创建 ==========
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'smart_cs'
ORDER BY TABLE_NAME, INDEX_NAME;
