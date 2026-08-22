package com.campus.order.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewResponse {
    private Long id;
    private Long orderId;
    private String orderProductTitle;
    private String productImageUrl;
    private Long fromUserId;
    private String fromUserNickname;
    private Long toUserId;
    private String toUserNickname;
    private Integer communicationScore;
    private Integer matchScore;
    private Integer speedScore;
    private Double avgScore;
    private String content;
    private LocalDateTime createdAt;
}