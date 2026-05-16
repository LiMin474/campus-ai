package com.campus.ai.controller;

import com.campus.ai.service.AiService;
import com.campus.common.api.ApiResponse;
import com.campus.common.util.SecurityUtils;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/polish")
    public ApiResponse<Map<String, String>> polish(@RequestBody Map<String, String> body) {
        SecurityUtils.requireUserId();
        String text = body.get("text");
        String polished = aiService.polishDescription(text);
        return ApiResponse.success(Map.of("text", polished));
    }
}
