package com.campus.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * AI 服务。
 * 现阶段（阶段 C）：转发到 Python ai-service（FastAPI），由 Python 侧统一处理 LLM 调用。
 * Python 服务不可用时降级为演示模式，保证主流程不阻断。
 */
@Service
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    private final ObjectMapper objectMapper;

    @Value("${app.ai-service.base-url:http://127.0.0.1:8001}")
    private String aiServiceBaseUrl;

    @Value("${app.ai-service.timeout-seconds:30}")
    private int timeoutSeconds;

    public AiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 润色商品描述：转发到 Python ai-service 的 /api/ai/polish。
     */
    public String polishDescription(String text) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("描述不能为空");
        }

        HttpURLConnection conn = null;
        try {
            String jsonBody = objectMapper.writeValueAsString(Map.of("text", text.trim()));
            byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            logger.info("调用 Python ai-service: url={}, body={}", aiServiceBaseUrl + "/api/ai/polish", jsonBody);

            conn = (HttpURLConnection) URI.create(aiServiceBaseUrl + "/api/ai/polish").toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(timeoutSeconds * 1000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
                os.flush();
            }

            int code = conn.getResponseCode();
            String raw;
            try (java.io.InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream()) {
                raw = is != null ? new String(is.readAllBytes(), StandardCharsets.UTF_8) : "";
            }

            if (code != 200) {
                throw new IllegalStateException("Python 服务返回 " + code + ": " + raw);
            }

            JsonNode root = objectMapper.readTree(raw);
            String polished = root.path("text").asText(null);
            if (!StringUtils.hasText(polished)) {
                throw new IllegalStateException("Python 服务返回空文本");
            }
            return polished;
        } catch (Exception ex) {
            logger.warn("调用 Python ai-service 失败，降级演示模式: {}", ex.getMessage());
            return "【演示模式】AI 服务暂不可用，原文如下：\n" + text.trim();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    // ==================================================================
    // RAG 相关：转发到 Python ai-service 的 /api/ai/rag/*
    // ==================================================================

    /**
     * RAG 对话：检索相关商品 + LLM 生成推荐回答。
     *
     * @param query 用户问题
     * @return Python 返回的完整 JSON（含 answer 与 sources），失败降级演示模式
     */
    public String ragChat(String query) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("查询不能为空");
        }
        return postToPython(
                "/api/ai/rag/chat",
                Map.of("query", query.trim()),
                "ragChat",
                "{\"answer\":\"【演示模式】AI 服务暂不可用，原文如下：" + query.trim() + "\",\"sources\":[]}"
        );
    }

    /**
     * RAG 灌库：全量重灌商品到向量库。
     *
     * @param products 商品列表，每项含 id 与 text
     * @return Python 返回的 JSON（含 indexed 条数），失败返回 {"indexed":0}
     */
    public String ragIndex(List<Map<String, Object>> products) {
        if (products == null || products.isEmpty()) {
            return "{\"indexed\":0}";
        }
        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode arr = objectMapper.createArrayNode();
        for (Map<String, Object> p : products) {
            ObjectNode item = objectMapper.createObjectNode();
            item.put("id", String.valueOf(p.get("id")));
            item.put("text", String.valueOf(p.get("text")));
            arr.add(item);
        }
        body.set("products", arr);
        try {
            return postToPythonRaw("/api/ai/rag/index", body, "ragIndex");
        } catch (Exception ex) {
            logger.warn("调用 Python ai-service 灌库失败: {}", ex.getMessage());
            return "{\"indexed\":0}";
        }
    }

    // ------------------------------------------------------------------
    // 通用 HTTP 转发工具
    // ------------------------------------------------------------------

    private String postToPython(String path, Object bodyObj, String tag, String fallbackJson) {
        try {
            return postToPythonRaw(path, bodyObj, tag);
        } catch (Exception ex) {
            logger.warn("调用 Python ai-service {} 失败，降级演示模式: {}", tag, ex.getMessage());
            return fallbackJson;
        }
    }

// 底层通用 HTTP 工具，发送 POST‑JSON 请求访问 FastAPI
    private String postToPythonRaw(String path, Object bodyObj, String tag) throws Exception {
        HttpURLConnection conn = null;
        try {
            String jsonBody = objectMapper.writeValueAsString(bodyObj);
            byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            logger.info("调用 Python ai-service: url={}, tag={}", aiServiceBaseUrl + path, tag);

            conn = (HttpURLConnection) URI.create(aiServiceBaseUrl + path).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(timeoutSeconds * 1000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
                os.flush();
            }

            int code = conn.getResponseCode();
            String raw;
            try (java.io.InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream()) {
                raw = is != null ? new String(is.readAllBytes(), StandardCharsets.UTF_8) : "";
            }

            if (code != 200) {
                throw new IllegalStateException("Python 服务返回 " + code + ": " + raw);
            }
            return raw;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
