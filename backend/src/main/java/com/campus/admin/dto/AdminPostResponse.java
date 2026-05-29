package com.campus.admin.dto;

import com.campus.community.entity.CommunityPost;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 管理员帖子列表响应DTO
 */
@Data
@Builder
public class AdminPostResponse {
    private Long id;
    private String title;
    private String content;
    private String authorNickname;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createdAt;

    public static AdminPostResponse fromEntity(CommunityPost post, String authorNickname) {
        return AdminPostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorNickname(authorNickname)
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .build();
    }
}