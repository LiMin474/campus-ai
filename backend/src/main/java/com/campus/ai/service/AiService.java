package com.campus.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class AiService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.deepseek.api-key:}")
    private String apiKey;

    @Value("${app.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    public AiService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public String polishDescription(String text) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("描述不能为空");
        }
        if (!StringUtils.hasText(apiKey)) {
            return "【演示模式】未配置 app.deepseek.api-key。原文如下：\n" + text.trim();
        }
        Map<String, Object> body = Map.of(
            "model", "deepseek-chat",
            "messages", List.of(
                Map.of("role", "system", "content", "你是校园二手交易平台的文案助手，请把用户提供的商品描述润色得更清晰、真诚、有吸引力，不要编造不存在的参数。"),
                Map.of("role", "user", "content", "请润色以下商品描述：\n" + text.trim())
            )
        );
        String raw = restClient.post()
            .uri(baseUrl + "/v1/chat/completions")
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .body(body)
            .retrieve()
            .body(String.class);
        try {
            JsonNode root = objectMapper.readTree(raw);
            return root.path("choices").path(0).path("message").path("content").asText(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("解析 AI 响应失败", ex);
        }
    }
}
