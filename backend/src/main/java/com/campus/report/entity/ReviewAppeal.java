package com.campus.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("review_appeal")
public class ReviewAppeal {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reviewId;
    private Long userId;
    private String reason;
    private String evidenceUrl;
    private String status;
    private String adminOpinion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}