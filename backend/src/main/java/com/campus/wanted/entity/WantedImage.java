package com.campus.wanted.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("wanted_image")
public class WantedImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long wantedId;
    private String imageUrl;
    private Integer sortOrder;
}