package com.smartcs.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("documents")
public class Document {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String fileName;

    private Long fileSize;

    private String fileType;

    private String filePath;

    private String status; // pending / processing / completed / failed

    private Integer chunkCount;

    private Long textLen;

    private String errorMsg;

    private Long kbId; // 知识库ID

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
