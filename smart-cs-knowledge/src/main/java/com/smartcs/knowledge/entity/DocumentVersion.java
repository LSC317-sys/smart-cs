package com.smartcs.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档版本历史实体
 */
@Data
@TableName("document_versions")
public class DocumentVersion {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 关联的文档ID */
    private Long docId;
    
    /** 版本号 */
    private Integer version;
    
    /** 唯一标识：docId_version */
    private String docIdVersion;
    
    /** MinIO 文件路径 */
    private String fileUrl;
    
    /** 文件大小 */
    private Long fileSize;
    
    /** 变更说明 */
    private String changeSummary;
    
    /** 创建时间 */
    private LocalDateTime createdAt;
    
    /** 创建者 */
    private Long createdBy;
}
