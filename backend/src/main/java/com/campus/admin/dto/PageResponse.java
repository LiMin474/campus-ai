package com.campus.admin.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 分页响应DTO
 */
@Data
@Builder
public class PageResponse<T> {
    private List<T> items;
    private Long total;
    private Integer page;
    private Integer size;
    private Integer totalPages;
}