package com.campus.wanted.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.category.entity.Category;
import com.campus.category.mapper.CategoryMapper;
import com.campus.user.entity.User;
import com.campus.user.mapper.UserMapper;
import com.campus.wanted.dto.WantedRequest;
import com.campus.wanted.dto.WantedResponse;
import com.campus.wanted.entity.Wanted;
import com.campus.wanted.mapper.WantedMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WantedService {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_CLOSED = "CLOSED";

    private final WantedMapper wantedMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;

    public WantedService(WantedMapper wantedMapper, CategoryMapper categoryMapper, UserMapper userMapper) {
        this.wantedMapper = wantedMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
    }

    @Transactional
    public Long create(Long userId, WantedRequest request) {
        Category category = categoryMapper.selectById(request.getCategoryId());
        if (category == null) {
            throw new IllegalArgumentException("分类不存在");
        }
        Wanted wanted = new Wanted();
        wanted.setUserId(userId);
        wanted.setTitle(request.getTitle().trim());
        wanted.setDescription(request.getDescription());
        wanted.setBudgetMin(request.getBudgetMin());
        wanted.setBudgetMax(request.getBudgetMax());
        wanted.setCategoryId(request.getCategoryId());
        wanted.setStatus(STATUS_OPEN);
        LocalDateTime now = LocalDateTime.now();
        wanted.setCreatedAt(now);
        wanted.setUpdatedAt(now);
        wantedMapper.insert(wanted);
        return wanted.getId();
    }

    @Transactional
    public void update(Long userId, Long id, WantedRequest request) {
        Wanted wanted = requireOwner(userId, id);
        if (!STATUS_OPEN.equals(wanted.getStatus())) {
            throw new IllegalArgumentException("已关闭的求购不可编辑");
        }
        wanted.setTitle(request.getTitle().trim());
        wanted.setDescription(request.getDescription());
        wanted.setBudgetMin(request.getBudgetMin());
        wanted.setBudgetMax(request.getBudgetMax());
        if (request.getCategoryId() != null) {
            Category category = categoryMapper.selectById(request.getCategoryId());
            if (category == null) {
                throw new IllegalArgumentException("分类不存在");
            }
            wanted.setCategoryId(request.getCategoryId());
        }
        wanted.setUpdatedAt(LocalDateTime.now());
        wantedMapper.updateById(wanted);
    }

    @Transactional
    public void close(Long userId, Long id) {
        Wanted wanted = requireOwner(userId, id);
        wanted.setStatus(STATUS_CLOSED);
        wanted.setUpdatedAt(LocalDateTime.now());
        wantedMapper.updateById(wanted);
    }

    public Page<WantedResponse> page(Long categoryId, int page, int size) {
        LambdaQueryWrapper<Wanted> wrapper = new LambdaQueryWrapper<Wanted>()
            .eq(Wanted::getStatus, STATUS_OPEN)
            .orderByDesc(Wanted::getCreatedAt);
        if (categoryId != null) {
            wrapper.eq(Wanted::getCategoryId, categoryId);
        }
        Page<Wanted> entityPage = wantedMapper.selectPage(new Page<>(page, size), wrapper);
        Page<WantedResponse> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(toResponses(entityPage.getRecords()));
        return result;
    }

    public WantedResponse detail(Long id) {
        Wanted wanted = wantedMapper.selectById(id);
        if (wanted == null) {
            throw new IllegalArgumentException("求购不存在");
        }
        return toResponses(List.of(wanted)).get(0);
    }

    private Wanted requireOwner(Long userId, Long id) {
        Wanted wanted = wantedMapper.selectById(id);
        if (wanted == null) {
            throw new IllegalArgumentException("求购不存在");
        }
        if (!Objects.equals(wanted.getUserId(), userId)) {
            throw new IllegalArgumentException("无权操作");
        }
        return wanted;
    }

    private List<WantedResponse> toResponses(List<Wanted> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Set<Long> catIds = list.stream().map(Wanted::getCategoryId).collect(Collectors.toSet());
        Set<Long> userIds = list.stream().map(Wanted::getUserId).collect(Collectors.toSet());
        Map<Long, Category> catMap = categoryMapper.selectBatchIds(catIds).stream()
            .collect(Collectors.toMap(Category::getId, c -> c));
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));

        return list.stream().map(w -> {
            Category cat = catMap.get(w.getCategoryId());
            User u = userMap.get(w.getUserId());
            return WantedResponse.builder()
                .id(w.getId())
                .title(w.getTitle())
                .description(w.getDescription())
                .budgetMin(w.getBudgetMin())
                .budgetMax(w.getBudgetMax())
                .categoryId(w.getCategoryId())
                .categoryName(cat != null ? cat.getName() : null)
                .userId(w.getUserId())
                .userNickname(u != null ? u.getNickname() : null)
                .status(w.getStatus())
                .createdAt(w.getCreatedAt())
                .build();
        }).toList();
    }
}
