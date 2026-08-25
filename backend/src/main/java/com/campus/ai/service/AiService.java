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

    /**
     * RAG 增量灌库：按 id 覆盖写入向量库（商品发布/编辑时调用）。
     * 失败不抛异常（记 warning），避免影响商品主业务流程。
     *
     * @param productId 商品 id
     * @param text      向量化文本（标题 + 描述）
     * @param price     价格（可为 null）
     * @param condition 成色（可为 null）
     */
    public void ragIndexIncremental(Long productId, String text, Double price, String condition) {
        if (productId == null || !StringUtils.hasText(text)) {
            return;
        }
        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode arr = objectMapper.createArrayNode();
        ObjectNode item = objectMapper.createObjectNode();
        item.put("id", String.valueOf(productId));
        item.put("text", text);
        if (price != null) {
            item.put("price", price);
        }
        if (StringUtils.hasText(condition)) {
            item.put("condition", condition);
        }
        arr.add(item);
        body.set("products", arr);
        try {
            postToPythonRaw("/api/ai/rag/index/incremental", body, "ragIndexIncremental");
        } catch (Exception ex) {
            logger.warn("调用 Python ai-service 增量灌库失败: {}", ex.getMessage());
        }
    }

    /**
     * RAG 删除向量：商品下架/删除时调用，从向量库移除对应记录。
     * 失败不抛异常（记 warning），避免影响商品主业务流程。
     *
     * @param productId 商品 id
     */
    public void ragIndexDelete(Long productId) {
        if (productId == null) {
            return;
        }
        try {
            postToPythonRaw(
                "/api/ai/rag/index/" + productId,
                objectMapper.createObjectNode(),
                "ragIndexDelete",
                "DELETE"
            );
        } catch (Exception ex) {
            logger.warn("调用 Python ai-service 删除向量失败: {}", ex.getMessage());
        }
    }

    /**
     * RAG 流式对话：转发 Python 的 SSE 流，逐块写入下游输出流（打字机效果）。
     * 异常时往输出流写入一个 error 事件并结束。
     *
     * @param query   用户问题
     * @param history 对话历史（多轮），每项含 role + content + 可选 sources
     * @param out     下游输出流（HttpServletResponse.getOutputStream()）
     */
    public void ragChatStream(String query, List<Map<String, Object>> history, java.io.OutputStream out) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("查询不能为空");
        }
        HttpURLConnection conn = null;
        try {
            // 构建请求体：query + history（多轮对话）
            ObjectNode body = objectMapper.createObjectNode();
            body.put("query", query.trim());
            if (history != null && !history.isEmpty()) {
                ArrayNode histArr = objectMapper.createArrayNode();
                for (Map<String, Object> m : history) {
                    ObjectNode item = objectMapper.createObjectNode();
                    item.put("role", String.valueOf(m.getOrDefault("role", "user")));
                    item.put("content", String.valueOf(m.getOrDefault("content", "")));
                    // 工具结果记忆：assistant 消息可带 sources（上轮检索的商品列表）
                    Object sources = m.get("sources");
                    if (sources instanceof List && !((List<?>) sources).isEmpty()) {
                        item.set("sources", objectMapper.valueToTree(sources));
                    }
                    histArr.add(item);
                }
                body.set("history", histArr);
            }
            String jsonBody = objectMapper.writeValueAsString(body);
            byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);

            conn = (HttpURLConnection) URI.create(aiServiceBaseUrl + "/api/ai/rag/chat/stream").toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(120_000); // 流式生成可能较长，放宽读超时
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "text/event-stream");
            conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
                os.flush();
            }

            int code = conn.getResponseCode();
            try (java.io.InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream()) {
                if (is == null) {
                    writeStreamEvent(out, "{\"type\":\"error\",\"message\":\"Python 服务无响应流\"}");
                    return;
                }
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    out.flush();
                }
            }
        } catch (Exception ex) {
            logger.warn("调用 Python ai-service 流式对话失败: {}", ex.getMessage());
            try {
                writeStreamEvent(out, "{\"type\":\"error\",\"message\":\"AI 服务暂不可用\"}");
            } catch (Exception ignored) {
                // 下游已断开则忽略
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /** 写一条 SSE 事件到下游。 */
    private void writeStreamEvent(java.io.OutputStream out, String dataJson) throws java.io.IOException {
        out.write(("data: " + dataJson + "\n\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
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
        return postToPythonRaw(path, bodyObj, tag, "POST");
    }

    private String postToPythonRaw(String path, Object bodyObj, String tag, String method) throws Exception {
        HttpURLConnection conn = null;
        try {
            String jsonBody = objectMapper.writeValueAsString(bodyObj);
            byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            logger.info("调用 Python ai-service: url={}, tag={}, method={}", aiServiceBaseUrl + path, tag, method);

            conn = (HttpURLConnection) URI.create(aiServiceBaseUrl + path).toURL().openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(timeoutSeconds * 1000);
            if ("POST".equals(method)) {
                conn.setDoOutput(true);
            }
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
            if ("POST".equals(method)) {
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bodyBytes);
                    os.flush();
                }
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
