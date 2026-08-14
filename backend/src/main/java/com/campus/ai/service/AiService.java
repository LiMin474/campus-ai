package com.campus.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * AI 服务。
 * 现阶段（阶段 C）：转发到 Python ai-service（FastAPI），由 Python 侧统一处理 LLM 调用。
 * Python 服务不可用时降级为演示模式，保证主流程不阻断。
 */
@Service
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.ai-service.base-url:http://127.0.0.1:8001}")
    private String aiServiceBaseUrl;

    @Value("${app.ai-service.timeout-seconds:30}")
    private int timeoutSeconds;

    public AiService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * 润色商品描述：转发到 Python ai-service 的 /api/ai/polish。
     */
    public String polishDescription(String text) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("描述不能为空");
        }

        try {
            String raw = restClient.post()
                .uri(aiServiceBaseUrl + "/api/ai/polish")
                .header("Content-Type", "application/json")
                .body(Map.of("text", text.trim()))
                .retrieve()
                .body(String.class);
            JsonNode root = objectMapper.readTree(raw);
            String polished = root.path("text").asText(null);
            if (!StringUtils.hasText(polished)) {
                throw new IllegalStateException("Python 服务返回空文本");
            }
            return polished;
        } catch (Exception ex) {
            logger.warn("调用 Python ai-service 失败，降级演示模式: {}", ex.getMessage());
            return "【演示模式】AI 服务暂不可用，原文如下：\n" + text.trim();
        }
    }
}
