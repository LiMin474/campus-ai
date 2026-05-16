package com.campus.chat.controller;

/**
 * 聊天系统控制器
 * 负责成员C：聊天系统
 */

import com.campus.chat.dto.ChatMessageResponse;
import com.campus.chat.dto.ConversationListItemResponse;
import com.campus.chat.dto.SendMessageRequest;
import com.campus.chat.dto.StartConversationRequest;
import com.campus.chat.service.ChatService;
import com.campus.common.api.ApiResponse;
import com.campus.common.util.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/conversations/start")
    public ApiResponse<Map<String, Long>> start(@Valid @RequestBody StartConversationRequest request) {
        Long userId = SecurityUtils.requireUserId();
        Long id = chatService.startOrGet(userId, request);
        return ApiResponse.success(Map.of("conversationId", id));
    }

    @GetMapping("/conversations")
    public ApiResponse<List<ConversationListItemResponse>> list() {
        Long userId = SecurityUtils.requireUserId();
        return ApiResponse.success(chatService.listConversations(userId));
    }

    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<ChatMessageResponse>> messages(
        @PathVariable Long id,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        Long userId = SecurityUtils.requireUserId();
        return ApiResponse.success(chatService.listMessages(id, userId, page, size));
    }

    @GetMapping("/conversations/{id}/messages/latest")
    public ApiResponse<List<ChatMessageResponse>> latest(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        return ApiResponse.success(chatService.latestMessages(id, userId));
    }

    @PostMapping("/conversations/{id}/messages")
    public ApiResponse<Map<String, Long>> send(
        @PathVariable Long id,
        @Valid @RequestBody SendMessageRequest request
    ) {
        Long userId = SecurityUtils.requireUserId();
        Long msgId = chatService.sendMessage(id, userId, request);
        return ApiResponse.success(Map.of("messageId", msgId));
    }

    @PostMapping("/conversations/{id}/read")
    public ApiResponse<Void> read(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        chatService.markRead(id, userId);
        return ApiResponse.success();
    }
}
