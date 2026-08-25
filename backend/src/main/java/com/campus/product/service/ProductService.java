package com.campus.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.ai.service.AiService;
import com.campus.category.entity.Category;
import com.campus.category.mapper.CategoryMapper;
import com.campus.product.dto.ProductCreateRequest;
import com.campus.product.dto.ProductDetailResponse;
import com.campus.product.dto.ProductListItemResponse;
import com.campus.product.dto.ProductUpdateRequest;
import com.campus.product.entity.Product;
import com.campus.product.entity.ProductAttachment;
import com.campus.product.entity.ProductImage;
import com.campus.product.mapper.ProductAttachmentMapper;
import com.campus.product.mapper.ProductImageMapper;
import com.campus.product.mapper.ProductMapper;
import com.campus.user.entity.User;
import com.campus.user.mapper.UserMapper;
import com.campus.user.util.CreditRules;
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
public class ProductService {

    public static final String STATUS_ON_SHELF = "ON_SHELF";
    public static final String STATUS_LOCKED = "LOCKED";
    public static final String STATUS_SOLD = "SOLD";
    public static final String STATUS_OFF_SHELF = "OFF_SHELF";

    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductAttachmentMapper productAttachmentMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final AiService aiService;

    public ProductService(
        ProductMapper productMapper,
        ProductImageMapper productImageMapper,
        ProductAttachmentMapper productAttachmentMapper,
        CategoryMapper categoryMapper,
        UserMapper userMapper,
        AiService aiService
    ) {
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.productAttachmentMapper = productAttachmentMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
        this.aiService = aiService;
    }

    /** 商品向量化文本：标题 + 描述（与 seed_chroma.py 保持一致） */
    private String vectorText(Product p) {
        String title = p.getTitle() == null ? "" : p.getTitle().trim();
        String desc = p.getDescription() == null ? "" : p.getDescription().trim();
        return (title + " " + desc).trim();
    }

    /** 增量同步到向量库（发布/编辑后调用，失败不影响主流程） */
    private void syncToVector(Product p) {
        aiService.ragIndexIncremental(
            p.getId(),
            vectorText(p),
            p.getPrice() != null ? p.getPrice().doubleValue() : null,
            p.getConditionLabel()
        );
    }

    @Transactional
    public Long create(Long sellerId, ProductCreateRequest request) {
        Category category = categoryMapper.selectById(request.getCategoryId());
        if (category == null) {
            throw new IllegalArgumentException("分类不存在");
        }
        Product product = new Product();
        product.setSellerId(sellerId);
        product.setTitle(request.getTitle().trim());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategoryId(request.getCategoryId());
        product.setConditionLabel(request.getConditionLabel());
        product.setStatus(STATUS_ON_SHELF);
        product.setViewCount(0);
        product.setLikeCount(0);
        LocalDateTime now = LocalDateTime.now();
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        productMapper.insert(product);

        saveImages(product.getId(), request.getImageUrls());
        saveAttachments(product.getId(), request.getAttachments());
        // 增量同步到向量库，AI 搜索立即可见
        syncToVector(product);
        return product.getId();
    }

    @Transactional
    public void update(Long userId, Long productId, ProductUpdateRequest request) {
        Product product = requireOwnerProduct(userId, productId);
        if (!STATUS_ON_SHELF.equals(product.getStatus()) && !STATUS_OFF_SHELF.equals(product.getStatus())) {
            throw new IllegalArgumentException("当前状态不可编辑");
        }
        if (StringUtils.hasText(request.getTitle())) {
            product.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryMapper.selectById(request.getCategoryId());
            if (category == null) {
                throw new IllegalArgumentException("分类不存在");
            }
            product.setCategoryId(request.getCategoryId());
        }
        if (request.getConditionLabel() != null) {
            product.setConditionLabel(request.getConditionLabel());
        }
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);

        if (request.getImageUrls() != null) {
            productImageMapper.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, productId));
            saveImages(productId, request.getImageUrls());
        }
        if (request.getAttachments() != null) {
            productAttachmentMapper.delete(new LambdaQueryWrapper<ProductAttachment>()
                .eq(ProductAttachment::getProductId, productId));
            saveAttachments(productId, request.getAttachments());
        }
        // 编辑后增量覆盖向量库，AI 搜索用新信息
        syncToVector(product);
    }

    @Transactional
    public void offShelf(Long userId, Long productId) {
        Product product = requireOwnerProduct(userId, productId);
        if (STATUS_SOLD.equals(product.getStatus())) {
            throw new IllegalArgumentException("已售出商品无法下架");
        }
        product.setStatus(STATUS_OFF_SHELF);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        // 下架后从向量库移除，AI 搜索不再返回
        aiService.ragIndexDelete(productId);
    }

    public Page<ProductListItemResponse> pageItems(Long categoryId, String keyword, String sort, int page, int size) {
        Page<Product> entityPage = pageEntities(categoryId, keyword, sort, page, size);
        Page<ProductListItemResponse> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(buildListItems(entityPage.getRecords()));
        return result;
    }

    public Page<ProductListItemResponse> pageMineItems(Long sellerId, Long categoryId, String keyword, String sort, int page, int size) {
        Page<Product> entityPage = pageMineEntities(sellerId, categoryId, keyword, sort, page, size);
        Page<ProductListItemResponse> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(buildListItems(entityPage.getRecords()));
        return result;
    }

    private Page<Product> pageMineEntities(Long sellerId, Long categoryId, String keyword, String sort, int page, int size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
            .eq(Product::getSellerId, sellerId);
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Product::getTitle, kw).or().like(Product::getDescription, kw));
        }
        if ("hot".equalsIgnoreCase(sort)) {
            wrapper.orderByDesc(Product::getViewCount).orderByDesc(Product::getLikeCount);
        } else {
            wrapper.orderByDesc(Product::getCreatedAt);
        }
        return productMapper.selectPage(new Page<>(page, size), wrapper);
    }

    private Page<Product> pageEntities(Long categoryId, String keyword, String sort, int page, int size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
            .eq(Product::getStatus, STATUS_ON_SHELF);
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Product::getTitle, kw).or().like(Product::getDescription, kw));
        }
        if ("hot".equalsIgnoreCase(sort)) {
            wrapper.orderByDesc(Product::getViewCount).orderByDesc(Product::getLikeCount);
        } else {
            wrapper.orderByDesc(Product::getCreatedAt);
        }
        return productMapper.selectPage(new Page<>(page, size), wrapper);
    }

    private List<ProductListItemResponse> buildListItems(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }
        Set<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toSet());
        Set<Long> categoryIds = products.stream().map(Product::getCategoryId).collect(Collectors.toSet());
        Set<Long> sellerIds = products.stream().map(Product::getSellerId).collect(Collectors.toSet());

        Map<Long, Category> categoryMap = categoryMapper.selectBatchIds(categoryIds).stream()
            .collect(Collectors.toMap(Category::getId, c -> c));
        Map<Long, User> sellerMap = userMapper.selectBatchIds(sellerIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));

        List<ProductImage> images = productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
            .in(ProductImage::getProductId, productIds));
        Map<Long, String> coverMap = images.stream()
            .collect(Collectors.groupingBy(ProductImage::getProductId))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().stream()
                    .min(Comparator.comparingInt(ProductImage::getSortOrder))
                    .map(ProductImage::getImageUrl)
                    .orElse(null)
            ));

        return products.stream().map(p -> {
            Category cat = categoryMap.get(p.getCategoryId());
            User seller = sellerMap.get(p.getSellerId());
            return ProductListItemResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .price(p.getPrice())
                .conditionLabel(p.getConditionLabel())
                .categoryId(p.getCategoryId())
                .categoryName(cat != null ? cat.getName() : null)
                .sellerId(p.getSellerId())
                .sellerNickname(seller != null ? seller.getNickname() : null)
                .sellerCreditLevel(seller != null ? CreditRules.levelLabel(seller.getCreditScore()) : null)
                .viewCount(p.getViewCount())
                .likeCount(p.getLikeCount())
                .coverImage(coverMap.get(p.getId()))
                .createdAt(p.getCreatedAt())
                .status(p.getStatus())
                .build();
        }).toList();
    }

    @Transactional
    public ProductDetailResponse detail(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
            .setSql("view_count = view_count + 1")
            .eq(Product::getId, productId));
        product.setViewCount(product.getViewCount() + 1);

        Category category = categoryMapper.selectById(product.getCategoryId());
        User seller = userMapper.selectById(product.getSellerId());
        List<ProductImage> images = productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
            .eq(ProductImage::getProductId, productId)
            .orderByAsc(ProductImage::getSortOrder));
        List<ProductAttachment> attachments = productAttachmentMapper.selectList(new LambdaQueryWrapper<ProductAttachment>()
            .eq(ProductAttachment::getProductId, productId));

        return ProductDetailResponse.builder()
            .id(product.getId())
            .title(product.getTitle())
            .description(product.getDescription())
            .price(product.getPrice())
            .categoryId(product.getCategoryId())
            .categoryName(category != null ? category.getName() : null)
            .conditionLabel(product.getConditionLabel())
            .status(product.getStatus())
            .viewCount(product.getViewCount())
            .likeCount(product.getLikeCount())
            .createdAt(product.getCreatedAt())
            .imageUrls(images.stream().map(ProductImage::getImageUrl).toList())
            .attachments(attachments.stream().map(a -> ProductDetailResponse.AttachmentVo.builder()
                .fileUrl(a.getFileUrl())
                .fileName(a.getFileName())
                .build()).toList())
            .seller(seller == null ? null : ProductDetailResponse.SellerBrief.builder()
                .id(seller.getId())
                .nickname(seller.getNickname())
                .creditScore(seller.getCreditScore())
                .creditLevel(CreditRules.levelLabel(seller.getCreditScore()))
                .build())
            .build();
    }

    public Product requireProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        return product;
    }

    public void updateStatus(Long productId, String status) {
        Product product = new Product();
        product.setId(productId);
        product.setStatus(status);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
    }

    private Product requireOwnerProduct(Long userId, Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (!Objects.equals(product.getSellerId(), userId)) {
            throw new IllegalArgumentException("无权操作该商品");
        }
        return product;
    }

    private void saveImages(Long productId, List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        int order = 0;
        for (String url : urls) {
            if (!StringUtils.hasText(url)) {
                continue;
            }
            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setImageUrl(url.trim());
            image.setSortOrder(order++);
            productImageMapper.insert(image);
        }
    }

    private void saveAttachments(Long productId, List<ProductCreateRequest.AttachmentDto> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (ProductCreateRequest.AttachmentDto dto : attachments) {
            if (dto == null || !StringUtils.hasText(dto.getFileUrl())) {
                continue;
            }
            ProductAttachment row = new ProductAttachment();
            row.setProductId(productId);
            row.setFileUrl(dto.getFileUrl().trim());
            row.setFileName(dto.getFileName());
            row.setCreatedAt(now);
            productAttachmentMapper.insert(row);
        }
    }

    public List<String> getProductImages(Long productId) {
        List<ProductImage> images = productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
            .eq(ProductImage::getProductId, productId)
            .orderByAsc(ProductImage::getSortOrder));
        return images.stream().map(ProductImage::getImageUrl).toList();
    }

}
