package com.campus.wanted.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.category.entity.Category;
import com.campus.category.mapper.CategoryMapper;
import com.campus.user.entity.User;
import com.campus.user.mapper.UserMapper;
import com.campus.wanted.dto.WantedRequest;
import com.campus.wanted.dto.WantedResponse;
import com.campus.wanted.entity.Wanted;
import com.campus.wanted.entity.WantedImage;
import com.campus.wanted.mapper.WantedImageMapper;
import com.campus.wanted.mapper.WantedMapper;
import java.time.LocalDateTime;
import java.util.Comparator;
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
    private final WantedImageMapper wantedImageMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;

    public WantedService(
        WantedMapper wantedMapper,
        WantedImageMapper wantedImageMapper,
        CategoryMapper categoryMapper,
        UserMapper userMapper
    ) {
        this.wantedMapper = wantedMapper;
        this.wantedImageMapper = wantedImageMapper;
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
        wanted.setViewCount(0);
        LocalDateTime now = LocalDateTime.now();
        wanted.setCreatedAt(now);
        wanted.setUpdatedAt(now);
        wantedMapper.insert(wanted);
        saveImages(wanted.getId(), request.getImageUrls());
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
        if (request.getImageUrls() != null) {
            wantedImageMapper.delete(new LambdaQueryWrapper<WantedImage>().eq(WantedImage::getWantedId, id));
            saveImages(id, request.getImageUrls());
        }
    }

    @Transactional
    public void close(Long userId, Long id) {
        Wanted wanted = requireOwner(userId, id);
        wanted.setStatus(STATUS_CLOSED);
        wanted.setUpdatedAt(LocalDateTime.now());
        wantedMapper.updateById(wanted);
    }

    @Transactional
    public void reopen(Long userId, Long id) {
        Wanted wanted = requireOwner(userId, id);
        wanted.setStatus(STATUS_OPEN);
        wanted.setUpdatedAt(LocalDateTime.now());
        wantedMapper.updateById(wanted);
    }

    public Page<WantedResponse> page(Long categoryId, String keyword, String sort, int page, int size) {
        LambdaQueryWrapper<Wanted> wrapper = new LambdaQueryWrapper<Wanted>()
            .eq(Wanted::getStatus, STATUS_OPEN);
        if (categoryId != null) {
            wrapper.eq(Wanted::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Wanted::getTitle, kw).or().like(Wanted::getDescription, kw));
        }
        applySort(wrapper, sort);
        Page<Wanted> entityPage = wantedMapper.selectPage(new Page<>(page, size), wrapper);
        Page<WantedResponse> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(toResponses(entityPage.getRecords()));
        return result;
    }

    private void applySort(LambdaQueryWrapper<Wanted> wrapper, String sort) {
        if ("hot".equalsIgnoreCase(sort)) {
            wrapper.orderByDesc(Wanted::getViewCount).orderByDesc(Wanted::getCreatedAt);
        } else if ("price_asc".equalsIgnoreCase(sort)) {
            wrapper.orderByAsc(Wanted::getBudgetMin).orderByDesc(Wanted::getCreatedAt);
        } else if ("price_desc".equalsIgnoreCase(sort)) {
            wrapper.orderByDesc(Wanted::getBudgetMax).orderByDesc(Wanted::getCreatedAt);
        } else {
            wrapper.orderByDesc(Wanted::getCreatedAt);
        }
    }

    public Page<WantedResponse> pageMine(Long userId, int page, int size) {
        Page<Wanted> entityPage = wantedMapper.selectPage(
            new Page<>(page, size),
            new LambdaQueryWrapper<Wanted>()
                .eq(Wanted::getUserId, userId)
                .orderByDesc(Wanted::getCreatedAt)
        );
        Page<WantedResponse> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(toResponses(entityPage.getRecords()));
        return result;
    }

    @Transactional
    public WantedResponse detail(Long id) {
        Wanted wanted = wantedMapper.selectById(id);
        if (wanted == null) {
            throw new IllegalArgumentException("求购不存在");
        }
        wantedMapper.update(null, new LambdaUpdateWrapper<Wanted>()
            .setSql("view_count = view_count + 1")
            .eq(Wanted::getId, id));
        int views = wanted.getViewCount() == null ? 0 : wanted.getViewCount();
        wanted.setViewCount(views + 1);
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
        Set<Long> wantedIds = list.stream().map(Wanted::getId).collect(Collectors.toSet());
        Set<Long> catIds = list.stream().map(Wanted::getCategoryId).collect(Collectors.toSet());
        Set<Long> userIds = list.stream().map(Wanted::getUserId).collect(Collectors.toSet());
        Map<Long, Category> catMap = categoryMapper.selectBatchIds(catIds).stream()
            .collect(Collectors.toMap(Category::getId, c -> c));
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));

        List<WantedImage> images = wantedImageMapper.selectList(new LambdaQueryWrapper<WantedImage>()
            .in(WantedImage::getWantedId, wantedIds)
            .orderByAsc(WantedImage::getSortOrder));
        Map<Long, List<String>> imageUrlsMap = images.stream()
            .collect(Collectors.groupingBy(
                WantedImage::getWantedId,
                Collectors.mapping(WantedImage::getImageUrl, Collectors.toList())
            ));
        Map<Long, String> coverMap = images.stream()
            .collect(Collectors.groupingBy(WantedImage::getWantedId))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().stream()
                    .min(Comparator.comparingInt(WantedImage::getSortOrder))
                    .map(WantedImage::getImageUrl)
                    .orElse(null)
            ));

        return list.stream().map(w -> {
            Category cat = catMap.get(w.getCategoryId());
            User u = userMap.get(w.getUserId());
            List<String> imageUrls = imageUrlsMap.getOrDefault(w.getId(), List.of());
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
                .viewCount(w.getViewCount() == null ? 0 : w.getViewCount())
                .createdAt(w.getCreatedAt())
                .coverImage(coverMap.get(w.getId()))
                .imageUrls(imageUrls)
                .build();
        }).toList();
    }

    private void saveImages(Long wantedId, List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        int order = 0;
        for (String url : urls) {
            if (!StringUtils.hasText(url)) {
                continue;
            }
            WantedImage image = new WantedImage();
            image.setWantedId(wantedId);
            image.setImageUrl(url.trim());
            image.setSortOrder(order++);
            wantedImageMapper.insert(image);
        }
    }
}
