package com.campus.chat.controller;

import com.campus.chat.dto.ChatMessageResponse;
import com.campus.chat.service.ChatService;
import com.campus.common.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
public class WebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat/conversations/{id}/messages")
    public void sendMessage(@DestinationVariable Long id, Map<String, String> message) {
        try {
            Long userId = SecurityUtils.requireUserId();
            String content = message.get("content");
            if (content == null || content.trim().isEmpty()) {
                return;
            }

            // 创建消息并保存到数据库
            Long messageId = chatService.sendMessage(id, userId, content);
            
            // 获取会话的另一方用户ID
            Long otherUserId = chatService.getOtherUserId(id, userId);
            
            // 构建消息响应
            ChatMessageResponse response = chatService.getMessageById(messageId);
            
            // 发送消息给发送者
            messagingTemplate.convertAndSend("/queue/chat/conversations/" + id, response);
            
            // 发送消息给接收者
            if (otherUserId != null) {
                messagingTemplate.convertAndSend("/queue/chat/conversations/" + id, response);
            }
        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/chat/conversations/{id}/read")
    public void markRead(@DestinationVariable Long id) {
        try {
            Long userId = SecurityUtils.requireUserId();
            chatService.markRead(id, userId);
            
            // 获取会话的另一方用户ID
            Long otherUserId = chatService.getOtherUserId(id, userId);
            
            // 通知对方消息已读
            if (otherUserId != null) {
                messagingTemplate.convertAndSend("/queue/chat/conversations/" + id + "/read", Map.of("userId", userId));
            }
        } catch (Exception e) {
            log.error("Error marking messages as read: {}", e.getMessage(), e);
        }
    }
}
