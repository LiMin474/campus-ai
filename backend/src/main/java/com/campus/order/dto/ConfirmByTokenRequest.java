package com.campus.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmByTokenRequest {
    @NotBlank
    private String token;
}
