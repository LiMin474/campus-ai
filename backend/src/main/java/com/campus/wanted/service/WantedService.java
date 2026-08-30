package com.campus.wanted.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.ai.service.AiService;
import com.campus.category.entity.Category;
import com.campus.category.mapper.CategoryMapper;
import com.campus.product.entity.Product;
import com.campus.product.entity.ProductImage;
import com.campus.product.mapper.ProductImageMapper;
import com.campus.product.mapper.ProductMapper;
import com.campus.user.entity.User;
import com.campus.user.mapper.UserMapper;
import com.campus.wanted.dto.MatchedProduct;
import com.campus.wanted.dto.WantedRequest;
import com.campus.wanted.dto.WantedResponse;
import com.campus.wanted.entity.Wanted;
import com.campus.wanted.entity.WantedImage;
import com.campus.wanted.mapper.WantedImageMapper;
import com.campus.wanted.mapper.WantedMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public WantedService(
        WantedMapper wantedMapper,
        WantedImageMapper wantedImageMapper,
        CategoryMapper categoryMapper,
        UserMapper userMapper,
        ProductMapper productMapper,
        ProductImageMapper productImageMapper,
        AiService aiService,
        ObjectMapper objectMapper
    ) {
        this.wantedMapper = wantedMapper;
        this.wantedImageMapper = wantedImageMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
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
        // 求购入向量库（失败不影响主流程）
        indexWantedToVector(wanted);
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
        // 重新入向量库（覆盖更新）
        if (STATUS_OPEN.equals(wanted.getStatus())) {
            indexWantedToVector(wanted);
        }
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
        // 从向量库删除（关闭后不再被匹配）
        aiService.ragDeleteWanted(wanted.getId());
    }

    @Transactional
    public void reopen(Long userId, Long id) {
        Wanted wanted = requireOwner(userId, id);
        wanted.setStatus(STATUS_OPEN);
        wanted.setUpdatedAt(LocalDateTime.now());
        wantedMapper.updateById(wanted);
        // 重新入向量库
        indexWantedToVector(wanted);
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

    /**
     * 求购找商品（阶段一 · 被动匹配）：用求购文本调 Python 检索商品库，
     * 再补全商品标题/价格/封面图返回。
     * Python 不可用或无可匹配商品时返回空列表，不影响求购详情主流程。
     */
    public List<MatchedProduct> matchProducts(Long id, int topK) {
        Wanted wanted = wantedMapper.selectById(id);
        if (wanted == null) {
            throw new IllegalArgumentException("求购不存在");
        }
        // 只对 OPEN 状态求购做匹配
        if (!STATUS_OPEN.equals(wanted.getStatus())) {
            return List.of();
        }
        String queryText = (wanted.getTitle() == null ? "" : wanted.getTitle().trim())
            + " "
            + (wanted.getDescription() == null ? "" : wanted.getDescription().trim());
        Double maxPrice = wanted.getBudgetMax() != null ? wanted.getBudgetMax().doubleValue() : null;

        String json = aiService.ragMatchProducts(queryText, maxPrice, topK);
        List<Long> productIds = parseMatchedProductIds(json);
        if (productIds.isEmpty()) {
            return List.of();
        }

        // 补全商品信息 + 封面图（只保留在售商品）
        List<ProductImage> allCovers = productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
            .in(ProductImage::getProductId, productIds)
            .orderByAsc(ProductImage::getSortOrder));
        Map<Long, String> coverMap = new java.util.HashMap<>();
        for (ProductImage pi : allCovers) {
            coverMap.putIfAbsent(pi.getProductId(), pi.getImageUrl());
        }

        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds)
            .stream()
            .collect(Collectors.toMap(Product::getId, p -> p));

        List<MatchedProduct> result = new java.util.ArrayList<>();
        for (Long pid : productIds) {
            Product p = productMap.get(pid);
            if (p == null || !"ON_SHELF".equals(p.getStatus())) {
                continue;
            }
            result.add(MatchedProduct.builder()
                .productId(pid)
                .title(p.getTitle())
                .price(p.getPrice())
                .coverImage(coverMap.get(pid))
                .build());
        }
        return result;
    }

    /** 解析 Python 返回 JSON 中的商品 id 列表（保持顺序）。 */
    private List<Long> parseMatchedProductIds(String json) {
        List<Long> ids = new java.util.ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode items = root.path("items");
            if (items.isArray()) {
                for (JsonNode it : items) {
                    String rawId = it.path("id").asText(null);
                    if (rawId != null && !rawId.isBlank()) {
                        try {
                            ids.add(Long.parseLong(rawId));
                        } catch (NumberFormatException ignored) {
                            // 跳过无法解析的 id
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // 解析失败返回空
        }
        return ids;
    }

    /** 构建求购文本并调 Python 入向量库（失败静默）。 */
    private void indexWantedToVector(Wanted wanted) {
        String text = (wanted.getTitle() == null ? "" : wanted.getTitle().trim())
            + " "
            + (wanted.getDescription() == null ? "" : wanted.getDescription().trim());
        aiService.ragIndexWanted(
            wanted.getId(),
            text.trim(),
            wanted.getBudgetMin() != null ? wanted.getBudgetMin().doubleValue() : null,
            wanted.getBudgetMax() != null ? wanted.getBudgetMax().doubleValue() : null,
            wanted.getStatus()
        );
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
