package com.campus.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("review")
public class Review {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long fromUserId;
    private Long toUserId;
    private Integer communicationScore;
    private Integer matchScore;
    private Integer speedScore;
    private String content;
    private LocalDateTime createdAt;
}
