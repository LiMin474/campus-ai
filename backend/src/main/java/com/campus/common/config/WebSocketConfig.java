package com.campus.common.config;

import com.campus.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.Collections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;

    public WebSocketConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new org.springframework.web.socket.server.support.DefaultHandshakeHandler())
                .withSockJS()
                .setHeartbeatTime(25000);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(1024 * 1024);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null) {
                    StompCommand command = accessor.getCommand();
                    if (StompCommand.CONNECT.equals(command)) {
                        String authHeader = accessor.getFirstNativeHeader("Authorization");
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            String token = authHeader.substring(7);
                            try {
                                Claims claims = jwtUtil.parseClaims(token);
                                Long userId = Long.parseLong(claims.getSubject());
                                String role = claims.get("role", String.class);
                                String authority = "ROLE_" + (role != null ? role : "STUDENT");
                                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                    userId, null, Collections.singletonList(new SimpleGrantedAuthority(authority))
                                );
                                accessor.setUser(auth);
                                SecurityContextHolder.getContext().setAuthentication(auth);
                                System.out.println("WebSocket CONNECT auth success: userId=" + userId);
                            } catch (JwtException | IllegalArgumentException e) {
                                System.out.println("JWT parse error: " + e.getMessage());
                            }
                        }
                    } else if (StompCommand.SEND.equals(command)) {
                        if (SecurityContextHolder.getContext().getAuthentication() == null) {
                            String authHeader = accessor.getFirstNativeHeader("Authorization");
                            if (authHeader == null) {
                                java.util.Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                                if (sessionAttributes != null && sessionAttributes.containsKey("Authorization")) {
                                    authHeader = (String) sessionAttributes.get("Authorization");
                                }
                            }
                            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                                String token = authHeader.substring(7);
                                try {
                                    Claims claims = jwtUtil.parseClaims(token);
                                    Long userId = Long.parseLong(claims.getSubject());
                                    String role = claims.get("role", String.class);
                                    String authority = "ROLE_" + (role != null ? role : "STUDENT");
                                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                        userId, null, Collections.singletonList(new SimpleGrantedAuthority(authority))
                                    );
                                    accessor.setUser(auth);
                                    SecurityContextHolder.getContext().setAuthentication(auth);
                                    System.out.println("WebSocket SEND auth success: userId=" + userId);
                                } catch (JwtException | IllegalArgumentException e) {
                                    System.out.println("JWT parse error on SEND: " + e.getMessage());
                                }
                            }
                        }
                    }
                }
                return message;
            }
        });
    }
}
