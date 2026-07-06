package com.kechang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kechang.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
