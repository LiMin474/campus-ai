package com.campus.community.controller;

/**
 * 社区频道控制器
 * 负责成员D：社区频道 + 用户管理
 */

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.api.ApiResponse;
import com.campus.common.util.SecurityUtils;
import com.campus.community.dto.CommentCreateRequest;
import com.campus.community.dto.CommentNodeResponse;
import com.campus.community.dto.PostCreateRequest;
import com.campus.community.dto.PostDetailResponse;
import com.campus.community.entity.CommunityPost;
import com.campus.community.service.CommunityService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping
    public ApiResponse<Page<CommunityPost>> page(
        @RequestParam(defaultValue = "latest") String sort,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(communityService.page(sort, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<PostDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(communityService.detail(id));
    }

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody PostCreateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return ApiResponse.success(communityService.createPost(userId, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        communityService.deletePost(userId, id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/comments")
    public ApiResponse<List<CommentNodeResponse>> comments(@PathVariable Long id) {
        return ApiResponse.success(communityService.listComments(id));
    }

    @PostMapping("/comments")
    public ApiResponse<Long> comment(@Valid @RequestBody CommentCreateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return ApiResponse.success(communityService.addComment(userId, request));
    }

    @PostMapping("/{id}/like")
    public ApiResponse<Boolean> like(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        return ApiResponse.success(communityService.toggleLikePost(userId, id));
    }

    @DeleteMapping("/comments/{id}")
    public ApiResponse<Void> deleteComment(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        communityService.deleteComment(userId, id);
        return ApiResponse.success();
    }
}
