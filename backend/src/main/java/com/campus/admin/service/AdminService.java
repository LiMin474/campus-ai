package com.campus.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.admin.dto.AdminDashboardResponse;
import com.campus.admin.dto.AdminDashboardResponse.CategoryStat;
import com.campus.admin.dto.AdminDashboardResponse.DailyStat;
import com.campus.admin.dto.AdminDashboardResponse.StatusStat;
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
            .build();
    }

    private List<DailyStat> generateDailyStats() {
        List<DailyStat> stats = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            // 每日用户注册数
            long dailyUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .between(User::getCreatedAt, start, end));

            // 每日商品发布数
            long dailyProducts = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .between(Product::getCreatedAt, start, end));

            // 每日订单数
            long dailyOrders = orderMapper.selectCount(new LambdaQueryWrapper<TradeOrder>()
                .between(TradeOrder::getCreatedAt, start, end));

            // 每日销售额
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
            // 分类商品数
            long productCount = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, category.getId()));

            // 分类销售额
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
}
