package com.campus.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.util.SecurityUtils;
import com.campus.community.dto.CommentCreateRequest;
import com.campus.community.dto.CommentNodeResponse;
import com.campus.community.dto.PostCreateRequest;
import com.campus.community.dto.PostDetailResponse;
import com.campus.community.entity.CommunityComment;
import com.campus.community.entity.CommunityPost;
import com.campus.community.entity.CommunityPostImage;
import com.campus.community.entity.LikeRecord;
import com.campus.community.mapper.CommunityCommentMapper;
import com.campus.community.mapper.CommunityPostImageMapper;
import com.campus.community.mapper.CommunityPostMapper;
import com.campus.community.mapper.LikeRecordMapper;
import com.campus.user.entity.User;
import com.campus.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CommunityService {

    public static final String LIKE_POST = "POST";

    private final CommunityPostMapper postMapper;
    private final CommunityPostImageMapper postImageMapper;
    private final CommunityCommentMapper commentMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final UserMapper userMapper;

    public CommunityService(
        CommunityPostMapper postMapper,
        CommunityPostImageMapper postImageMapper,
        CommunityCommentMapper commentMapper,
        LikeRecordMapper likeRecordMapper,
        UserMapper userMapper
    ) {
        this.postMapper = postMapper;
        this.postImageMapper = postImageMapper;
        this.commentMapper = commentMapper;
        this.likeRecordMapper = likeRecordMapper;
        this.userMapper = userMapper;
    }

    @Transactional
    public Long createPost(Long userId, PostCreateRequest request) {
        CommunityPost post = new CommunityPost();
        post.setUserId(userId);
        post.setTitle(request.getTitle().trim());
        post.setContent(request.getContent().trim());
        post.setLikeCount(0);
        post.setCommentCount(0);
        LocalDateTime now = LocalDateTime.now();
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        postMapper.insert(post);
        if (request.getImageUrls() != null) {
            int order = 0;
            for (String url : request.getImageUrls()) {
                if (!StringUtils.hasText(url)) {
                    continue;
                }
                CommunityPostImage img = new CommunityPostImage();
                img.setPostId(post.getId());
                img.setImageUrl(url.trim());
                img.setSortOrder(order++);
                postImageMapper.insert(img);
            }
        }
        return post.getId();
    }

    public Page<CommunityPost> page(String sort, int page, int size) {
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
        if ("hot".equalsIgnoreCase(sort)) {
            wrapper.orderByDesc(CommunityPost::getLikeCount).orderByDesc(CommunityPost::getCommentCount);
        } else {
            wrapper.orderByDesc(CommunityPost::getCreatedAt);
        }
        return postMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public PostDetailResponse detail(Long postId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        User author = userMapper.selectById(post.getUserId());
        List<CommunityPostImage> images = postImageMapper.selectList(new LambdaQueryWrapper<CommunityPostImage>()
            .eq(CommunityPostImage::getPostId, postId)
            .orderByAsc(CommunityPostImage::getSortOrder));
        return PostDetailResponse.builder()
            .id(post.getId())
            .title(post.getTitle())
            .content(post.getContent())
            .userId(post.getUserId())
            .userNickname(author != null ? author.getNickname() : null)
            .likeCount(post.getLikeCount())
            .commentCount(post.getCommentCount())
            .createdAt(post.getCreatedAt())
            .imageUrls(images.stream().map(CommunityPostImage::getImageUrl).toList())
            .build();
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        boolean admin = SecurityUtils.hasRole("ADMIN");
        if (!admin && !Objects.equals(post.getUserId(), userId)) {
            throw new IllegalArgumentException("无权删除");
        }
        postMapper.deleteById(postId);
        postImageMapper.delete(new LambdaQueryWrapper<CommunityPostImage>().eq(CommunityPostImage::getPostId, postId));
        commentMapper.delete(new LambdaQueryWrapper<CommunityComment>().eq(CommunityComment::getPostId, postId));
    }

    @Transactional
    public Long addComment(Long userId, CommentCreateRequest request) {
        CommunityPost post = postMapper.selectById(request.getPostId());
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        CommunityComment comment = new CommunityComment();
        comment.setPostId(request.getPostId());
        comment.setUserId(userId);
        comment.setParentId(request.getParentId());
        comment.setContent(request.getContent().trim());
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);

        post.setCommentCount(post.getCommentCount() + 1);
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(post);
        return comment.getId();
    }

    public List<CommentNodeResponse> listComments(Long postId) {
        List<CommunityComment> rows = commentMapper.selectList(new LambdaQueryWrapper<CommunityComment>()
            .eq(CommunityComment::getPostId, postId)
            .orderByAsc(CommunityComment::getCreatedAt));
        if (rows.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = rows.stream().map(CommunityComment::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));

        Map<Long, CommentNodeResponse> nodeMap = new HashMap<>();
        for (CommunityComment c : rows) {
            CommentNodeResponse node = new CommentNodeResponse();
            node.setId(c.getId());
            node.setPostId(c.getPostId());
            node.setUserId(c.getUserId());
            User u = userMap.get(c.getUserId());
            node.setUserNickname(u != null ? u.getNickname() : null);
            node.setParentId(c.getParentId());
            node.setContent(c.getContent());
            node.setCreatedAt(c.getCreatedAt());
            nodeMap.put(c.getId(), node);
        }
        List<CommentNodeResponse> roots = new ArrayList<>();
        for (CommunityComment c : rows) {
            CommentNodeResponse node = nodeMap.get(c.getId());
            if (c.getParentId() == null) {
                roots.add(node);
            } else {
                CommentNodeResponse parent = nodeMap.get(c.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        return roots;
    }

    @Transactional
    public boolean toggleLikePost(Long userId, Long postId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        LikeRecord exist = likeRecordMapper.selectOne(new LambdaQueryWrapper<LikeRecord>()
            .eq(LikeRecord::getUserId, userId)
            .eq(LikeRecord::getTargetType, LIKE_POST)
            .eq(LikeRecord::getTargetId, postId));
        if (exist == null) {
            LikeRecord record = new LikeRecord();
            record.setUserId(userId);
            record.setTargetType(LIKE_POST);
            record.setTargetId(postId);
            record.setCreatedAt(LocalDateTime.now());
            likeRecordMapper.insert(record);
            post.setLikeCount(post.getLikeCount() + 1);
            postMapper.updateById(post);
            return true;
        }
        likeRecordMapper.deleteById(exist.getId());
        post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        postMapper.updateById(post);
        return false;
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        CommunityComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new IllegalArgumentException("评论不存在");
        }
        boolean admin = SecurityUtils.hasRole("ADMIN");
        if (!admin && !Objects.equals(comment.getUserId(), userId)) {
            throw new IllegalArgumentException("无权删除");
        }
        Long postId = comment.getPostId();
        commentMapper.deleteById(commentId);
        CommunityPost post = postMapper.selectById(postId);
        if (post != null) {
            post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
            post.setUpdatedAt(LocalDateTime.now());
            postMapper.updateById(post);
        }
    }
}
