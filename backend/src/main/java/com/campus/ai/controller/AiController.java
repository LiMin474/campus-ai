package com.campus.ai.controller;

import com.campus.ai.service.AiService;
import com.campus.common.api.ApiResponse;
import com.campus.common.util.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public AiController(AiService aiService, ObjectMapper objectMapper) {
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/polish")
    public ApiResponse<Map<String, String>> polish(@RequestBody Map<String, String> body) {
        SecurityUtils.requireUserId();
        String text = body.get("text");
        String polished = aiService.polishDescription(text);
        return ApiResponse.success(Map.of("text", polished));
    }

    /**
     * RAG 对话：前端发来用户问题，转发到 Python 检索 + 生成。
     * 返回 Python 原始 JSON（含 answer 与 sources）。
     */
    @PostMapping("/rag/chat")
    public ApiResponse<JsonNode> ragChat(@RequestBody Map<String, String> body) {
        // 1. 权限校验 必须登录
        SecurityUtils.requireUserId();
        // 2. 拿到前端返回来的 提问
        String query = body.get("query");
         // 3. AiService发HTTP请求调用FastAPI，拿到Python返回的原始JSON字符串
        String raw = aiService.ragChat(query);
         // 4. 字符串 → JsonNode 对象
        try {
            JsonNode node = objectMapper.readTree(raw);
             // 5. 包一层统一返回体 ApiResponse，返回前端
            return ApiResponse.success(node);
        } catch (Exception e) {
            return ApiResponse.fail("解析 RAG 响应失败: " + e.getMessage());
        }
    }

    /**
     * RAG 灌库：手动触发，全量重灌商品到向量库。
     * 请求体格式：{"products": [{"id":"1","text":"商品描述"}, ...]}
     */
    @PostMapping("/rag/index")
    public ApiResponse<JsonNode> ragIndex(@RequestBody Map<String, Object> body) {
        SecurityUtils.requireUserId();
        Object arr = body.get("products");
        List<Map<String, Object>> products = arr instanceof List ? (List<Map<String, Object>>) arr : List.of();
        String raw = aiService.ragIndex(products);
        try {
            JsonNode node = objectMapper.readTree(raw);
            return ApiResponse.success(node);
        } catch (Exception e) {
            return ApiResponse.fail("解析灌库响应失败: " + e.getMessage());
        }
    }
}
