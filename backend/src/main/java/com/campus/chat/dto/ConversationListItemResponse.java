package com.campus.chat.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConversationListItemResponse {
    private Long id;
    private Long peerUserId;
    private String peerNickname;
    private String peerAvatarUrl;
    private String contextType;
    private Long contextId;
    private String contextTitle;
    private String contextCoverUrl;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private Long unreadCount;
}
