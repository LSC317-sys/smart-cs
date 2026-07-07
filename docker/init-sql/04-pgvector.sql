-- ============================================================
-- pgvector 向量存储初始化脚本
-- 执行方式: psql -U liu317228 -d smart_cs_vector -f 04-pgvector.sql
-- 或通过 DBeaver/pgAdmin 执行
-- ============================================================

-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================================
-- 向量存储表（与 MySQL document_chunks 表配合使用）
-- MySQL 存业务数据，pgvector 存向量
-- ============================================================
CREATE TABLE IF NOT EXISTS chunk_vectors (
    id BIGSERIAL PRIMARY KEY,
    chunk_id BIGINT NOT NULL,           -- 对应 MySQL document_chunks.id
    doc_id BIGINT NOT NULL,              -- 对应 MySQL documents.id
    kb_id BIGINT NOT NULL DEFAULT 1,     -- 知识库 ID（保留字段）
    embedding vector(1024) NOT NULL,     -- bge-m3 embedding 向量，1024 维
    created_at TIMESTAMP DEFAULT NOW()
);

-- 注释
COMMENT ON TABLE chunk_vectors IS '向量存储表（pgvector），与 MySQL document_chunks 配合使用';
COMMENT ON COLUMN chunk_vectors.chunk_id IS '对应 MySQL document_chunks.id';
COMMENT ON COLUMN chunk_vectors.embedding IS 'bge-m3 模型生成的 1024 维向量';

-- ============================================================
-- HNSW 索引（近似最近邻搜索的核心）
-- cosine 操作符 = 余弦相似度，HNSW 算法 O(log N) 查询
-- 注意：HNSW 需要更多内存但查询更快，适合高 QPS 场景
-- ============================================================
DROP INDEX IF EXISTS idx_chunk_vectors_hnsw;
CREATE INDEX idx_chunk_vectors_hnsw ON chunk_vectors
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- 可选：doc_id 上的 B-tree 索引（按文档删除时快速定位）
CREATE INDEX IF NOT EXISTS idx_chunk_vectors_doc_id ON chunk_vectors(doc_id);

-- 可选：kb_id 上的 B-tree 索引（按知识库检索时快速过滤）
CREATE INDEX IF NOT EXISTS idx_chunk_vectors_kb_id ON chunk_vectors(kb_id);

-- ============================================================
-- 清理函数（按 doc_id 删除向量，用于文档删除时同步清理）
-- ============================================================
CREATE OR REPLACE FUNCTION delete_vectors_by_doc_id(p_doc_id BIGINT)
RETURNS INTEGER AS \$\$
DECLARE
    v_count INTEGER;
BEGIN
    DELETE FROM chunk_vectors WHERE doc_id = p_doc_id;
    GET DIAGNOSTICS v_count = ROW_COUNT;
    RETURN v_count;
END;
\$\$ LANGUAGE plpgsql;

-- ============================================================
-- 验证：确认扩展已正确安装
-- ============================================================
SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';
