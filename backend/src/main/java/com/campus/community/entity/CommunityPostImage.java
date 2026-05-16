package com.campus.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("community_post_image")
public class CommunityPostImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private String imageUrl;
    private Integer sortOrder;
}
