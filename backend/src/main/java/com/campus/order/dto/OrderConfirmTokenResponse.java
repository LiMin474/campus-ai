package com.campus.order.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderConfirmTokenResponse {
    private String token;
    private LocalDateTime expiresAt;
    /**
     * 买家扫码打开的完整链接（前端路由 + token）
     */
    private String confirmUrl;
}
