package com.kechang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kechang.entity.CourseStudent;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseStudentMapper extends BaseMapper<CourseStudent> {

    /**
     * 查询选课记录（包含逻辑删除的记录）
     */
    @Select("SELECT * FROM course_student WHERE course_id = #{courseId} AND student_id = #{studentId}")
    List<CourseStudent> selectListWithDeleted(@Param("courseId") Long courseId, @Param("studentId") Long studentId);

    /**
     * 物理删除选课记录（绕过逻辑删除）
     */
    @Delete("DELETE FROM course_student WHERE course_id = #{courseId} AND student_id = #{studentId}")
    int deletePhysically(@Param("courseId") Long courseId, @Param("studentId") Long studentId);
}
