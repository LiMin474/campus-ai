package com.campus.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartConversationRequest {
    @NotNull
    private Long peerUserId;
    /**
     * GENERAL | PRODUCT | WANTED
     */
    private String contextType;
    private Long contextId;
}
