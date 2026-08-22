package com.campus.chat.controller;

import com.campus.chat.dto.ChatMessageResponse;
import com.campus.chat.service.ChatService;
import com.campus.common.util.JwtUtil;
import com.campus.common.util.SecurityUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
public class WebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final JwtUtil jwtUtil;

    public WebSocketController(ChatService chatService, SimpMessagingTemplate messagingTemplate, JwtUtil jwtUtil) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.jwtUtil = jwtUtil;
    }

    @MessageMapping("/chat/conversations/{id}/messages")
    public void sendMessage(org.springframework.messaging.Message<?> stompMessage, @DestinationVariable Long id, Map<String, String> message) {
        try {
            Long userId = getUserIdFromStomp(stompMessage);
            if (userId == null) {
                log.error("User not authenticated via WebSocket");
                return;
            }
            String content = message.get("content");
            if (content == null || content.trim().isEmpty()) {
                return;
            }

            Long messageId = chatService.sendMessage(id, userId, content);
            Long otherUserId = chatService.getOtherUserId(id, userId);
            ChatMessageResponse response = chatService.getMessageById(messageId);
            messagingTemplate.convertAndSend("/queue/chat/conversations/" + id, response);
        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage(), e);
        }
    }

    private Long getUserIdFromStomp(org.springframework.messaging.Message<?> message) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && accessor.getUser() != null) {
            Object principal = accessor.getUser();
            if (principal instanceof Long) {
                return (Long) principal;
            }
        }
        
        String authHeader = null;
        if (accessor != null) {
            authHeader = accessor.getFirstNativeHeader("Authorization");
        }
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.error("No Bearer token found in WebSocket headers");
            return null;
        }
        String token = authHeader.substring(7);
        try {
            Claims claims = jwtUtil.parseClaims(token);
            return Long.parseLong(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT parse error: {}", e.getMessage());
            return null;
        }
    }

    @MessageMapping("/chat/conversations/{id}/read")
    public void markRead(org.springframework.messaging.Message<?> stompMessage, @DestinationVariable Long id) {
        try {
            Long userId = getUserIdFromStomp(stompMessage);
            if (userId == null) {
                log.error("User not authenticated via WebSocket for markRead");
                return;
            }
            chatService.markRead(id, userId);
            Long otherUserId = chatService.getOtherUserId(id, userId);
            if (otherUserId != null) {
                messagingTemplate.convertAndSend("/queue/chat/conversations/" + id + "/read", Map.of("userId", userId));
            }
        } catch (Exception e) {
            log.error("Error marking messages as read: {}", e.getMessage(), e);
        }
    }
}
