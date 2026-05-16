package com.campus.community.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

@Data
public class PostCreateRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String content;
    private List<String> imageUrls;
}
