package com.campus.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.order.dto.ConfirmByTokenRequest;
import com.campus.order.dto.OrderConfirmTokenResponse;
import com.campus.order.dto.OrderCreateRequest;
import com.campus.order.dto.OrderResponse;
import com.campus.order.entity.TradeOrder;
import com.campus.order.mapper.TradeOrderMapper;
import com.campus.product.entity.Product;
import com.campus.product.mapper.ProductMapper;
import com.campus.product.service.ProductService;
import com.campus.user.entity.User;
import com.campus.user.mapper.UserMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OrderService {

    public static final String STATUS_PENDING = "PENDING_CONFIRM";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SHIPPED = "SHIPPED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private final TradeOrderMapper tradeOrderMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final String frontendBaseUrl;

    public OrderService(
        TradeOrderMapper tradeOrderMapper,
        ProductMapper productMapper,
        UserMapper userMapper,
        @Value("${app.frontend-base-url:http://localhost:5173}") String frontendBaseUrl
    ) {
        this.tradeOrderMapper = tradeOrderMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
        this.frontendBaseUrl = frontendBaseUrl.endsWith("/")
            ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
            : frontendBaseUrl;
    }

    @Transactional
    public Long create(Long buyerId, OrderCreateRequest request) {
        Product product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (!ProductService.STATUS_ON_SHELF.equals(product.getStatus())) {
            throw new IllegalArgumentException("商品不可购买");
        }
        if (Objects.equals(product.getSellerId(), buyerId)) {
            throw new IllegalArgumentException("不能购买自己的商品");
        }
        TradeOrder order = new TradeOrder();
        order.setProductId(product.getId());
        order.setBuyerId(buyerId);
        order.setSellerId(product.getSellerId());
        order.setStatus(STATUS_PENDING);
        order.setFinalPrice(product.getPrice());
        LocalDateTime now = LocalDateTime.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        tradeOrderMapper.insert(order);

        product.setStatus(ProductService.STATUS_LOCKED);
        product.setUpdatedAt(now);
        productMapper.updateById(product);
        return order.getId();
    }

    @Transactional
    public void cancel(Long userId, Long orderId) {
        TradeOrder order = requireOrder(orderId);
        if (!Objects.equals(order.getBuyerId(), userId)) {
            throw new IllegalArgumentException("仅买家可取消订单");
        }
        if (!STATUS_PENDING.equals(order.getStatus())) {
            throw new IllegalArgumentException("当前状态不可取消");
        }
        order.setStatus(STATUS_CANCELLED);
        order.setConfirmToken(null);
        order.setConfirmTokenExpire(null);
        order.setUpdatedAt(LocalDateTime.now());
        tradeOrderMapper.updateById(order);

        Product product = productMapper.selectById(order.getProductId());
        if (product != null && ProductService.STATUS_LOCKED.equals(product.getStatus())) {
            product.setStatus(ProductService.STATUS_ON_SHELF);
            product.setUpdatedAt(LocalDateTime.now());
            productMapper.updateById(product);
        }
    }

    @Transactional
    public void confirmOrder(Long sellerId, Long orderId) {
        TradeOrder order = requireOrder(orderId);
        if (!Objects.equals(order.getSellerId(), sellerId)) {
            throw new IllegalArgumentException("仅卖家可确认订单");
        }
        if (!STATUS_PENDING.equals(order.getStatus())) {
            throw new IllegalArgumentException("当前状态不可确认订单");
        }
        order.setStatus(STATUS_PROCESSING);
        order.setUpdatedAt(LocalDateTime.now());
        tradeOrderMapper.updateById(order);
    }

    @Transactional
    public void markAsShipped(Long sellerId, Long orderId) {
        TradeOrder order = requireOrder(orderId);
        if (!Objects.equals(order.getSellerId(), sellerId)) {
            throw new IllegalArgumentException("仅卖家可标记发货");
        }
        if (!STATUS_PROCESSING.equals(order.getStatus())) {
            throw new IllegalArgumentException("当前状态不可标记发货");
        }
        order.setStatus(STATUS_SHIPPED);
        order.setUpdatedAt(LocalDateTime.now());
        tradeOrderMapper.updateById(order);
    }

    @Transactional
    public void confirm(Long userId, Long orderId) {
        TradeOrder order = requireOrder(orderId);
        if (!Objects.equals(order.getBuyerId(), userId)) {
            throw new IllegalArgumentException("仅买家可确认收货");
        }
        if (!STATUS_PENDING.equals(order.getStatus()) && !STATUS_SHIPPED.equals(order.getStatus())) {
            throw new IllegalArgumentException("当前状态不可确认收货");
        }
        completeOrder(order);
    }

    @Transactional
    public OrderConfirmTokenResponse generateConfirmToken(Long sellerId, Long orderId) {
        TradeOrder order = requireOrder(orderId);
        if (!Objects.equals(order.getSellerId(), sellerId)) {
            throw new IllegalArgumentException("仅卖家可生成确认码");
        }
        if (!STATUS_PENDING.equals(order.getStatus())) {
            throw new IllegalArgumentException("当前状态不可生成确认码");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime exp = LocalDateTime.now().plusHours(24);
        order.setConfirmToken(token);
        order.setConfirmTokenExpire(exp);
        order.setUpdatedAt(LocalDateTime.now());
        tradeOrderMapper.updateById(order);
        String encoded = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String confirmUrl = frontendBaseUrl + "/orders/confirm?token=" + encoded;
        return OrderConfirmTokenResponse.builder()
            .token(token)
            .expiresAt(exp)
            .confirmUrl(confirmUrl)
            .build();
    }

    @Transactional
    public void confirmWithToken(Long buyerId, ConfirmByTokenRequest request) {
        String token = request.getToken().trim();
        TradeOrder order = tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrder>()
            .eq(TradeOrder::getConfirmToken, token));
        if (order == null) {
            throw new IllegalArgumentException("确认码无效");
        }
        if (order.getConfirmTokenExpire() == null || order.getConfirmTokenExpire().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("确认码已过期");
        }
        if (!Objects.equals(order.getBuyerId(), buyerId)) {
            throw new IllegalArgumentException("当前账号不是买家，无法确认");
        }
        if (!STATUS_PENDING.equals(order.getStatus())) {
            throw new IllegalArgumentException("订单状态已变更");
        }
        completeOrder(order);
    }

    private void completeOrder(TradeOrder order) {
        order.setStatus(STATUS_COMPLETED);
        order.setConfirmToken(null);
        order.setConfirmTokenExpire(null);
        order.setUpdatedAt(LocalDateTime.now());
        tradeOrderMapper.updateById(order);

        Product product = productMapper.selectById(order.getProductId());
        if (product != null) {
            product.setStatus(ProductService.STATUS_SOLD);
            product.setUpdatedAt(LocalDateTime.now());
            productMapper.updateById(product);
        }

        User seller = userMapper.selectById(order.getSellerId());
        User buyer = userMapper.selectById(order.getBuyerId());
        if (seller != null) {
            seller.setCarbonPoints(seller.getCarbonPoints() + 10);
            seller.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(seller);
        }
        if (buyer != null) {
            buyer.setCarbonPoints(buyer.getCarbonPoints() + 5);
            buyer.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(buyer);
        }
    }

    public Page<OrderResponse> page(Long userId, String role, String status, int page, int size) {
        LambdaQueryWrapper<TradeOrder> wrapper = new LambdaQueryWrapper<>();
        if ("seller".equalsIgnoreCase(role)) {
            wrapper.eq(TradeOrder::getSellerId, userId);
        } else {
            wrapper.eq(TradeOrder::getBuyerId, userId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(TradeOrder::getStatus, status.trim());
        }
        wrapper.orderByDesc(TradeOrder::getCreatedAt);
        Page<TradeOrder> entityPage = tradeOrderMapper.selectPage(new Page<>(page, size), wrapper);
        Page<OrderResponse> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(toResponses(entityPage.getRecords()));
        return result;
    }

    public OrderResponse detail(Long userId, Long orderId) {
        TradeOrder order = requireOrder(orderId);
        if (!Objects.equals(order.getBuyerId(), userId) && !Objects.equals(order.getSellerId(), userId)) {
            throw new IllegalArgumentException("无权查看该订单");
        }
        return toResponses(List.of(order)).get(0);
    }

    public TradeOrder requireOrder(Long orderId) {
        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        return order;
    }

    private List<OrderResponse> toResponses(List<TradeOrder> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        Set<Long> productIds = orders.stream().map(TradeOrder::getProductId).collect(Collectors.toSet());
        Set<Long> userIds = orders.stream()
            .flatMap(o -> java.util.stream.Stream.of(o.getBuyerId(), o.getSellerId()))
            .collect(Collectors.toSet());
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
            .collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));

        return orders.stream().map(o -> {
            Product p = productMap.get(o.getProductId());
            User buyer = userMap.get(o.getBuyerId());
            User seller = userMap.get(o.getSellerId());
            return OrderResponse.builder()
                .id(o.getId())
                .productId(o.getProductId())
                .productTitle(p != null ? p.getTitle() : null)
                .buyerId(o.getBuyerId())
                .buyerNickname(buyer != null ? buyer.getNickname() : null)
                .sellerId(o.getSellerId())
                .sellerNickname(seller != null ? seller.getNickname() : null)
                .status(o.getStatus())
                .finalPrice(o.getFinalPrice())
                .createdAt(o.getCreatedAt())
                .build();
        }).toList();
    }
}
