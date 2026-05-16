package com.campus.chat.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessageResponse {
    private Long id;
    private Long senderId;
    private String senderNickname;
    private String content;
    private LocalDateTime createdAt;
    private boolean mine;
}
