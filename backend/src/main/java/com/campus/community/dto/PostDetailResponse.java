package com.campus.community.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostDetailResponse {
    private Long id;
    private String title;
    private String content;
    private Long userId;
    private String userNickname;
    private String userAvatarUrl;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createdAt;
    private List<String> imageUrls;
}
