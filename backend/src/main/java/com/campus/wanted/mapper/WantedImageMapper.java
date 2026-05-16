package com.campus.wanted.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.wanted.entity.WantedImage;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WantedImageMapper extends BaseMapper<WantedImage> {
    
    @Select("SELECT * FROM wanted_image WHERE wanted_id = #{wantedId} ORDER BY sort_order")
    List<WantedImage> selectByWantedId(Long wantedId);
}