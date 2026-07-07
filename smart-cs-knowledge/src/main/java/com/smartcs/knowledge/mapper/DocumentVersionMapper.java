package com.smartcs.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcs.knowledge.entity.DocumentVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文档版本 Mapper
 */
@Mapper
public interface DocumentVersionMapper extends BaseMapper<DocumentVersion> {
    
    /**
     * 获取文档的所有版本（按版本号降序）
     */
    @Select("SELECT * FROM document_versions WHERE doc_id = #{docId} ORDER BY version DESC")
    List<DocumentVersion> findByDocId(@Param("docId") Long docId);
    
    /**
     * 获取文档的最新版本号
     */
    @Select("SELECT MAX(version) FROM document_versions WHERE doc_id = #{docId}")
    Integer findMaxVersion(@Param("docId") Long docId);
    
    /**
     * 获取文档的特定版本
     */
    @Select("SELECT * FROM document_versions WHERE doc_id = #{docId} AND version = #{version}")
    DocumentVersion findByDocIdAndVersion(@Param("docId") Long docId, @Param("version") Integer version);
}
