package com.campus.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.admin.dto.AdminDashboardResponse;
import com.campus.admin.dto.AdminDashboardResponse.CategoryStat;
import com.campus.admin.dto.AdminDashboardResponse.DailyStat;
import com.campus.admin.dto.AdminDashboardResponse.RecentActivity;
import com.campus.admin.dto.AdminDashboardResponse.StatusStat;
import com.campus.admin.dto.AdminOrderResponse;
import com.campus.admin.dto.AdminPostResponse;
import com.campus.admin.dto.AdminProductResponse;
import com.campus.admin.dto.PageResponse;
import com.campus.category.entity.Category;
import com.campus.category.mapper.CategoryMapper;
import com.campus.community.entity.CommunityPost;
import com.campus.community.mapper.CommunityPostMapper;
import com.campus.order.entity.TradeOrder;
import com.campus.order.mapper.TradeOrderMapper;
import com.campus.product.entity.Product;
import com.campus.product.mapper.ProductMapper;
import com.campus.report.entity.Report;
import com.campus.report.mapper.ReportMapper;
import com.campus.user.entity.User;
import com.campus.user.mapper.UserMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final ReportMapper reportMapper;
    private final TradeOrderMapper orderMapper;
    private final CommunityPostMapper postMapper;
    private final CategoryMapper categoryMapper;

    public AdminService(UserMapper userMapper, ProductMapper productMapper, ReportMapper reportMapper, 
                       TradeOrderMapper orderMapper, CommunityPostMapper postMapper, CategoryMapper categoryMapper) {
        this.userMapper = userMapper;
        this.productMapper = productMapper;
        this.reportMapper = reportMapper;
        this.orderMapper = orderMapper;
        this.postMapper = postMapper;
        this.categoryMapper = categoryMapper;
    }

    public AdminDashboardResponse dashboard() {
        // 基础统计
        long users = userMapper.selectCount(null);
        long products = productMapper.selectCount(null);
        long pending = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
            .eq(Report::getStatus, "PENDING"));
        long orders = orderMapper.selectCount(null);
        long posts = postMapper.selectCount(null);
        BigDecimal totalSales = orderMapper.selectList(new LambdaQueryWrapper<TradeOrder>()
            .eq(TradeOrder::getStatus, "COMPLETED"))
            .stream()
            .map(TradeOrder::getFinalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 每日统计（最近7天）
        List<DailyStat> dailyStats = generateDailyStats();

        // 分类统计
        List<CategoryStat> categoryStats = generateCategoryStats();

        // 订单状态统计
        List<StatusStat> orderStatusStats = generateOrderStatusStats();

        // 商品状态统计
        List<StatusStat> productStatusStats = generateProductStatusStats();

        // 最近活动
        List<RecentActivity> recentActivities = generateRecentActivities();

        return AdminDashboardResponse.builder()
            .userCount(users)
            .productCount(products)
            .pendingReportCount(pending)
            .orderCount(orders)
            .postCount(posts)
            .totalSales(totalSales)
            .dailyStats(dailyStats)
            .categoryStats(categoryStats)
            .orderStatusStats(orderStatusStats)
            .productStatusStats(productStatusStats)
            .recentActivities(recentActivities)
            .build();
    }

    private List<DailyStat> generateDailyStats() {
        List<DailyStat> stats = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            long dailyUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .between(User::getCreatedAt, start, end));

            long dailyProducts = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .between(Product::getCreatedAt, start, end));

            long dailyOrders = orderMapper.selectCount(new LambdaQueryWrapper<TradeOrder>()
                .between(TradeOrder::getCreatedAt, start, end));

            BigDecimal dailySales = orderMapper.selectList(new LambdaQueryWrapper<TradeOrder>()
                .between(TradeOrder::getCreatedAt, start, end)
                .eq(TradeOrder::getStatus, "COMPLETED"))
                .stream()
                .map(TradeOrder::getFinalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            stats.add(DailyStat.builder()
                .date(date)
                .userCount(dailyUsers)
                .productCount(dailyProducts)
                .orderCount(dailyOrders)
                .salesAmount(dailySales)
                .build());
        }

        return stats;
    }

    private List<CategoryStat> generateCategoryStats() {
        List<Category> categories = categoryMapper.selectList(null);
        List<CategoryStat> stats = new ArrayList<>();

        for (Category category : categories) {
            long productCount = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, category.getId()));

            BigDecimal salesAmount = orderMapper.selectList(new LambdaQueryWrapper<TradeOrder>()
                .inSql(TradeOrder::getProductId, "SELECT id FROM product WHERE category_id = " + category.getId())
                .eq(TradeOrder::getStatus, "COMPLETED"))
                .stream()
                .map(TradeOrder::getFinalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            stats.add(CategoryStat.builder()
                .categoryName(category.getName())
                .productCount(productCount)
                .salesAmount(salesAmount)
                .build());
        }

        return stats;
    }

    private List<StatusStat> generateOrderStatusStats() {
        List<TradeOrder> orders = orderMapper.selectList(null);
        Map<String, Long> statusCountMap = orders.stream()
            .collect(Collectors.groupingBy(TradeOrder::getStatus, Collectors.counting()));

        return statusCountMap.entrySet().stream()
            .map(entry -> StatusStat.builder()
                .status(entry.getKey())
                .count(entry.getValue())
                .build())
            .collect(Collectors.toList());
    }

    private List<StatusStat> generateProductStatusStats() {
        List<Product> products = productMapper.selectList(null);
        Map<String, Long> statusCountMap = products.stream()
            .collect(Collectors.groupingBy(Product::getStatus, Collectors.counting()));

        return statusCountMap.entrySet().stream()
            .map(entry -> StatusStat.builder()
                .status(entry.getKey())
                .count(entry.getValue())
                .build())
            .collect(Collectors.toList());
    }

    private List<RecentActivity> generateRecentActivities() {
        List<RecentActivity> activities = new ArrayList<>();
        
        // 获取最近的用户注册
        List<User> recentUsers = userMapper.selectList(new LambdaQueryWrapper<User>()
            .orderByDesc(User::getCreatedAt)
            .last("LIMIT 5"));
        for (User user : recentUsers) {
            activities.add(RecentActivity.builder()
                .type("USER_REGISTER")
                .description("用户 " + user.getNickname() + " 注册")
                .timestamp(user.getCreatedAt())
                .build());
        }

        // 获取最近的商品发布
        List<Product> recentProducts = productMapper.selectList(new LambdaQueryWrapper<Product>()
            .orderByDesc(Product::getCreatedAt)
            .last("LIMIT 5"));
        for (Product product : recentProducts) {
            activities.add(RecentActivity.builder()
                .type("PRODUCT_CREATE")
                .description("商品 " + product.getTitle() + " 发布")
                .timestamp(product.getCreatedAt())
                .build());
        }

        // 获取最近的订单创建
        List<TradeOrder> recentOrders = orderMapper.selectList(new LambdaQueryWrapper<TradeOrder>()
            .orderByDesc(TradeOrder::getCreatedAt)
            .last("LIMIT 5"));
        for (TradeOrder order : recentOrders) {
            activities.add(RecentActivity.builder()
                .type("ORDER_CREATE")
                .description("订单 #" + order.getId() + " 创建")
                .timestamp(order.getCreatedAt())
                .build());
        }

        // 按时间排序
        activities.sort(Comparator.comparing(RecentActivity::getTimestamp).reversed());
        return activities.stream().limit(10).collect(Collectors.toList());
    }

    public List<User> listUsers(String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(User::getNickname, kw)
                .or().like(User::getStudentNo, kw));
        }
        wrapper.orderByDesc(User::getCreatedAt);
        return userMapper.selectList(wrapper);
    }

    @Transactional
    public void setBanned(Long userId, boolean banned) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if ("ADMIN".equals(user.getRole())) {
            throw new IllegalArgumentException("不能封禁管理员");
        }
        user.setBanned(banned);
        userMapper.updateById(user);
    }

    @Transactional
    public void resetCredit(Long userId, int score) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setCreditScore(score);
        userMapper.updateById(user);
    }

    // ========== 商品管理 ==========
    
    public PageResponse<AdminProductResponse> listProducts(int page, int size, String keyword, String status) {
        IPage<Product> productPage = productMapper.selectPage(
            new Page<>(page, size),
            new LambdaQueryWrapper<Product>()
                .like(keyword != null && !keyword.isBlank(), Product::getTitle, keyword.trim())
                .eq(status != null && !status.isBlank(), Product::getStatus, status)
                .orderByDesc(Product::getCreatedAt)
        );

        List<Long> sellerIds = productPage.getRecords().stream()
            .map(Product::getSellerId)
            .distinct()
            .collect(Collectors.toList());
        
        Map<Long, User> sellerMap = sellerIds.isEmpty() ? Map.of() :
            userMapper.selectBatchIds(sellerIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<Long> categoryIds = productPage.getRecords().stream()
            .map(Product::getCategoryId)
            .distinct()
            .collect(Collectors.toList());
        
        Map<Long, Category> categoryMap = categoryIds.isEmpty() ? Map.of() :
            categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        List<AdminProductResponse> items = productPage.getRecords().stream()
            .map(product -> AdminProductResponse.fromEntity(
                product,
                categoryMap.get(product.getCategoryId()) != null ? 
                    categoryMap.get(product.getCategoryId()).getName() : "未知",
                sellerMap.get(product.getSellerId()) != null ? 
                    sellerMap.get(product.getSellerId()).getNickname() : "未知"
            ))
            .collect(Collectors.toList());

        return PageResponse.<AdminProductResponse>builder()
            .items(items)
            .total(productPage.getTotal())
            .page(page)
            .size(size)
            .totalPages((int) Math.ceil((double) productPage.getTotal() / size))
            .build();
    }

    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        productMapper.deleteById(productId);
    }

    @Transactional
    public void updateProductStatus(Long productId, String status) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        product.setStatus(status);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
    }

    // ========== 帖子管理 ==========
    
    public PageResponse<AdminPostResponse> listPosts(int page, int size, String keyword) {
        IPage<CommunityPost> postPage = postMapper.selectPage(
            new Page<>(page, size),
            new LambdaQueryWrapper<CommunityPost>()
                .like(keyword != null && !keyword.isBlank(), CommunityPost::getTitle, keyword.trim())
                .orderByDesc(CommunityPost::getCreatedAt)
        );

        List<Long> userIds = postPage.getRecords().stream()
            .map(CommunityPost::getUserId)
            .distinct()
            .collect(Collectors.toList());
        
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of() :
            userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<AdminPostResponse> items = postPage.getRecords().stream()
            .map(post -> AdminPostResponse.fromEntity(
                post,
                userMap.get(post.getUserId()) != null ? 
                    userMap.get(post.getUserId()).getNickname() : "未知"
            ))
            .collect(Collectors.toList());

        return PageResponse.<AdminPostResponse>builder()
            .items(items)
            .total(postPage.getTotal())
            .page(page)
            .size(size)
            .totalPages((int) Math.ceil((double) postPage.getTotal() / size))
            .build();
    }

    @Transactional
    public void deletePost(Long postId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        postMapper.deleteById(postId);
    }

    // ========== 订单管理 ==========
    
    public PageResponse<AdminOrderResponse> listOrders(int page, int size, String status) {
        IPage<TradeOrder> orderPage = orderMapper.selectPage(
            new Page<>(page, size),
            new LambdaQueryWrapper<TradeOrder>()
                .eq(status != null && !status.isBlank(), TradeOrder::getStatus, status)
                .orderByDesc(TradeOrder::getCreatedAt)
        );

        List<Long> productIds = orderPage.getRecords().stream()
            .map(TradeOrder::getProductId)
            .distinct()
            .collect(Collectors.toList());
        
        Map<Long, Product> productMap = productIds.isEmpty() ? Map.of() :
            productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<Long> userIds = new ArrayList<>();
        orderPage.getRecords().forEach(order -> {
            userIds.add(order.getBuyerId());
            userIds.add(order.getSellerId());
        });
        
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of() :
            userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<AdminOrderResponse> items = orderPage.getRecords().stream()
            .map(order -> AdminOrderResponse.fromEntity(
                order,
                productMap.get(order.getProductId()) != null ? 
                    productMap.get(order.getProductId()).getTitle() : "未知商品",
                userMap.get(order.getBuyerId()) != null ? 
                    userMap.get(order.getBuyerId()).getNickname() : "未知",
                userMap.get(order.getSellerId()) != null ? 
                    userMap.get(order.getSellerId()).getNickname() : "未知"
            ))
            .collect(Collectors.toList());

        return PageResponse.<AdminOrderResponse>builder()
            .items(items)
            .total(orderPage.getTotal())
            .page(page)
            .size(size)
            .totalPages((int) Math.ceil((double) orderPage.getTotal() / size))
            .build();
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        TradeOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }
}