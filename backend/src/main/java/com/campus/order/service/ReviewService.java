package com.campus.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.order.dto.ReviewCreateRequest;
import com.campus.order.entity.Review;
import com.campus.order.entity.TradeOrder;
import com.campus.order.mapper.ReviewMapper;
import com.campus.order.mapper.TradeOrderMapper;
import com.campus.user.entity.User;
import com.campus.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final UserMapper userMapper;

    public ReviewService(ReviewMapper reviewMapper, TradeOrderMapper tradeOrderMapper, UserMapper userMapper) {
        this.reviewMapper = reviewMapper;
        this.tradeOrderMapper = tradeOrderMapper;
        this.userMapper = userMapper;
    }

    @Transactional
    public void create(Long fromUserId, ReviewCreateRequest request) {
        TradeOrder order = tradeOrderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!OrderService.STATUS_COMPLETED.equals(order.getStatus())) {
            throw new IllegalArgumentException("订单未完成，无法评价");
        }
        if (!Objects.equals(order.getBuyerId(), fromUserId) && !Objects.equals(order.getSellerId(), fromUserId)) {
            throw new IllegalArgumentException("无权评价该订单");
        }
        if (!Objects.equals(request.getToUserId(), order.getBuyerId())
            && !Objects.equals(request.getToUserId(), order.getSellerId())) {
            throw new IllegalArgumentException("评价对象不合法");
        }
        if (Objects.equals(fromUserId, request.getToUserId())) {
            throw new IllegalArgumentException("不能评价自己");
        }
        Long exists = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
            .eq(Review::getOrderId, request.getOrderId())
            .eq(Review::getFromUserId, fromUserId));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("该订单已评价");
        }

        Review review = new Review();
        review.setOrderId(request.getOrderId());
        review.setFromUserId(fromUserId);
        review.setToUserId(request.getToUserId());
        review.setCommunicationScore(request.getCommunicationScore());
        review.setMatchScore(request.getMatchScore());
        review.setSpeedScore(request.getSpeedScore());
        review.setContent(request.getContent());
        review.setCreatedAt(LocalDateTime.now());
        reviewMapper.insert(review);

        double avg = (request.getCommunicationScore() + request.getMatchScore() + request.getSpeedScore()) / 3.0;
        User toUser = userMapper.selectById(request.getToUserId());
        if (toUser == null) {
            return;
        }
        int score = toUser.getCreditScore();
        if (avg >= 4.0) {
            score += 1;
        } else if (avg <= 2.0) {
            score -= 3;
        }
        toUser.setCreditScore(score);
        toUser.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(toUser);
    }
}
