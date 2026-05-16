package com.campus.community.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class CommentNodeResponse {
    private Long id;
    private Long postId;
    private Long userId;
    private String userNickname;
    private Long parentId;
    private String content;
    private LocalDateTime createdAt;
    private List<CommentNodeResponse> children = new ArrayList<>();
}
