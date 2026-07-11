package com.kechang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kechang.entity.StudentProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StudentProfileMapper extends BaseMapper<StudentProfile> {

    /** 查询最大学号（绕过 @TableLogic，包含已软删除记录） */
    @Select("SELECT COALESCE(MAX(student_no), '0') FROM student_profile")
    String selectMaxStudentNo();
}
