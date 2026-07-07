package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kechang.common.BusinessException;
import com.kechang.config.JwtUtil;
import com.kechang.dto.LoginDTO;
import com.kechang.dto.PasswordDTO;
import com.kechang.dto.ProfileDTO;
import com.kechang.dto.RegisterDTO;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import com.kechang.vo.LoginVO;
import com.kechang.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 用户认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final TeacherProfileMapper teacherProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    /**
     * 登录
     */
    public LoginVO login(LoginDTO dto) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == 1) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        return buildLoginVO(user);
    }

    /**
     * 注册
     */
    @Transactional
    public LoginVO register(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw BusinessException.badRequest("两次密码输入不一致");
        }
        // 检查邮箱唯一
        if (sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, dto.getEmail())) != null) {
            throw BusinessException.badRequest("该邮箱已被注册");
        }
        // 判断角色类型：teacher -> ROLE_TEACHER，否则默认 ROLE_STUDENT
        boolean isTeacher = "teacher".equalsIgnoreCase(dto.getRoleType());
        String roleCode = isTeacher ? "ROLE_TEACHER" : "ROLE_STUDENT";
        SysRole role = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
        if (role == null) {
            throw new BusinessException("系统角色配置异常");
        }
        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(dto.getEmail());  // 邮箱作为登录账号
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        String nickname = hasText(dto.getNickname()) ? dto.getNickname().trim() : dto.getEmail().split("@")[0];
        String realName = hasText(dto.getRealName()) ? dto.getRealName().trim() : nickname;
        user.setNickname(nickname);
        user.setRoleId(role.getId());
        user.setStatus(0);
        sysUserMapper.insert(user);

        // 根据角色创建对应的扩展信息
        if (isTeacher) {
            TeacherProfile profile = new TeacherProfile();
            profile.setUserId(user.getId());
            profile.setRealName(realName);
            profile.setDepartment(clean(dto.getDepartment()));
            profile.setTitle(clean(dto.getTitle()));
            teacherProfileMapper.insert(profile);
        } else {
            String studentNo = hasText(dto.getStudentNo()) ? dto.getStudentNo().trim() : generateStudentNo();
            if (hasText(dto.getStudentNo()) && studentProfileMapper.selectOne(
                    new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getStudentNo, studentNo)) != null) {
                throw BusinessException.badRequest("该学号已被使用");
            }
            StudentProfile profile = new StudentProfile();
            profile.setUserId(user.getId());
            profile.setStudentNo(studentNo);
            profile.setRealName(realName);
            profile.setGrade(clean(dto.getGrade()));
            profile.setMajor(clean(dto.getMajor()));
            profile.setClassName(clean(dto.getClassName()));
            studentProfileMapper.insert(profile);
        }

        return buildLoginVO(user);
    }

    /**
     * 获取当前用户信息
     */
    public UserVO getCurrentUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        SysRole role = sysRoleMapper.selectById(user.getRoleId());
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        if (role != null) {
            vo.setRoleCode(role.getRoleCode());
            vo.setRoleName(role.getRoleName());
        }
        // 扩展信息
        if ("ROLE_TEACHER".equals(role != null ? role.getRoleCode() : "")) {
            TeacherProfile tp = teacherProfileMapper.selectOne(
                    new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getUserId, userId));
            if (tp != null) {
                vo.setRealName(tp.getRealName());
                vo.setTitle(tp.getTitle());
                vo.setDepartment(tp.getDepartment());
            }
        } else if ("ROLE_STUDENT".equals(role != null ? role.getRoleCode() : "")) {
            StudentProfile sp = studentProfileMapper.selectOne(
                    new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, userId));
            if (sp != null) {
                vo.setRealName(sp.getRealName());
                vo.setStudentNo(sp.getStudentNo());
                vo.setGrade(sp.getGrade());
                vo.setMajor(sp.getMajor());
                vo.setClassName(sp.getClassName());
            }
        }
        return vo;
    }

    /**
     * 修改密码
     */
    public void changePassword(Long userId, PasswordDTO dto) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw BusinessException.badRequest("旧密码不正确");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        sysUserMapper.updateById(user);
    }

    /**
     * 更新用户资料
     */
    public void updateProfile(Long userId, ProfileDTO dto) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getEmail() != null) {
            // 检查邮箱是否被其他用户占用
            SysUser exist = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getEmail, dto.getEmail())
                            .ne(SysUser::getId, userId));
            if (exist != null) {
                throw BusinessException.badRequest("该邮箱已被其他用户使用");
            }
            user.setEmail(dto.getEmail());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        sysUserMapper.updateById(user);
    }

    /**
     * 退出登录，Token 加入黑名单
     */
    public void logout(String token) {
        // 计算剩余有效时间并加入 Redis 黑名单
        try {
            long expiration = jwtUtil.parseToken(token).getExpiration().getTime() - System.currentTimeMillis();
            if (expiration > 0) {
                redisTemplate.opsForValue().set("blacklist:" + token, "1", Duration.ofMillis(expiration));
            }
        } catch (Exception e) {
            log.warn("Token 黑名单处理失败: {}", e.getMessage());
        }
    }

    /**
     * 构建 LoginVO
     */
    private LoginVO buildLoginVO(SysUser user) {
        SysRole role = sysRoleMapper.selectById(user.getRoleId());
        String roleCode = role != null ? role.getRoleCode() : "";
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roleCode);
        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .roleCode(roleCode)
                .roleName(role != null ? role.getRoleName() : "")
                .build();
    }

    /**
     * 生成学号：年份 + 4位序号
     */
    private String generateStudentNo() {
        String year = String.valueOf(LocalDate.now().getYear());
        Long count = studentProfileMapper.selectCount(null);
        return year + String.format("%04d", (count + 1) % 10000);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String clean(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
