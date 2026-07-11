package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.BusinessException;
import com.kechang.common.PageResult;
import com.kechang.dto.AdminPasswordResetDTO;
import com.kechang.dto.AdminUserCreateDTO;
import com.kechang.dto.AdminUserUpdateDTO;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import com.kechang.vo.AdminUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员用户管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final TeacherProfileMapper teacherProfileMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询非管理员用户列表
     */
    public PageResult<AdminUserVO> list(int page, int size, String keyword, Long roleId, Integer status) {
        // 获取管理员角色ID，用于排除
        SysRole adminRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "ROLE_ADMIN"));
        Long adminRoleId = adminRole != null ? adminRole.getId() : null;

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (adminRoleId != null) {
            wrapper.ne(SysUser::getRoleId, adminRoleId);
        }
        // 按角色筛选
        if (roleId != null) {
            wrapper.eq(SysUser::getRoleId, roleId);
        }
        // 按状态筛选
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        // 关键词搜索：用户名 / 昵称 / 邮箱
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword)
                    .or().like(SysUser::getEmail, keyword));
        }
        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> result = sysUserMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量获取角色和profile信息
        List<SysRole> allRoles = sysRoleMapper.selectList(null);
        Map<Long, SysRole> roleMap = allRoles.stream()
                .collect(Collectors.toMap(SysRole::getId, r -> r));

        List<Long> userIds = result.getRecords().stream().map(SysUser::getId).toList();
        Map<Long, TeacherProfile> teacherMap = Collections.emptyMap();
        Map<Long, StudentProfile> studentMap = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            List<TeacherProfile> tps = teacherProfileMapper.selectList(
                    new LambdaQueryWrapper<TeacherProfile>().in(TeacherProfile::getUserId, userIds));
            teacherMap = tps.stream().collect(Collectors.toMap(TeacherProfile::getUserId, tp -> tp));
            List<StudentProfile> sps = studentProfileMapper.selectList(
                    new LambdaQueryWrapper<StudentProfile>().in(StudentProfile::getUserId, userIds));
            studentMap = sps.stream().collect(Collectors.toMap(StudentProfile::getUserId, sp -> sp));
        }

        List<AdminUserVO> vos = new ArrayList<>();
        for (SysUser user : result.getRecords()) {
            AdminUserVO vo = toVO(user, roleMap, teacherMap, studentMap);
            vos.add(vo);
        }

        return PageResult.of(vos, result.getTotal(), page, size);
    }

    /**
     * 用户统计
     */
    public Map<String, Long> stats() {
        SysRole adminRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "ROLE_ADMIN"));
        Long adminRoleId = adminRole != null ? adminRole.getId() : null;

        LambdaQueryWrapper<SysUser> nonAdminWrapper = new LambdaQueryWrapper<>();
        if (adminRoleId != null) {
            nonAdminWrapper.ne(SysUser::getRoleId, adminRoleId);
        }
        long totalUsers = sysUserMapper.selectCount(nonAdminWrapper);

        // 教师数量
        SysRole teacherRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "ROLE_TEACHER"));
        long teacherCount = 0;
        if (teacherRole != null) {
            teacherCount = sysUserMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, teacherRole.getId()));
        }

        // 学生数量
        SysRole studentRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "ROLE_STUDENT"));
        long studentCount = 0;
        if (studentRole != null) {
            studentCount = sysUserMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, studentRole.getId()));
        }

        // 本月新增
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LambdaQueryWrapper<SysUser> newWrapper = new LambdaQueryWrapper<SysUser>()
                .ge(SysUser::getCreateTime, monthStart);
        if (adminRoleId != null) {
            newWrapper.ne(SysUser::getRoleId, adminRoleId);
        }
        long newThisMonth = sysUserMapper.selectCount(newWrapper);

        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("teacherCount", teacherCount);
        stats.put("studentCount", studentCount);
        stats.put("totalUsers", totalUsers);
        stats.put("newThisMonth", newThisMonth);
        stats.put("teacherRoleId", teacherRole != null ? teacherRole.getId() : null);
        stats.put("studentRoleId", studentRole != null ? studentRole.getId() : null);
        return stats;
    }

    /**
     * 获取用户详情
     */
    public AdminUserVO detail(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        SysRole role = sysRoleMapper.selectById(user.getRoleId());
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setRoleId(user.getRoleId());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        if (role != null) {
            vo.setRoleCode(role.getRoleCode());
            vo.setRoleName(role.getRoleName());
        }
        fillProfile(vo, userId, role);
        return vo;
    }

    /**
     * 管理员创建用户
     */
    @Transactional
    public void create(AdminUserCreateDTO dto) {
        // 验证用户名唯一
        if (sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername())) != null) {
            throw BusinessException.badRequest("该用户名已被使用");
        }
        // 验证邮箱唯一
        if (StringUtils.hasText(dto.getEmail()) && sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, dto.getEmail())) != null) {
            throw BusinessException.badRequest("该邮箱已被注册");
        }
        // 验证角色
        SysRole role = sysRoleMapper.selectById(dto.getRoleId());
        if (role == null) {
            throw BusinessException.badRequest("角色不存在");
        }
        if ("ROLE_ADMIN".equals(role.getRoleCode())) {
            throw BusinessException.badRequest("不允许创建管理员账号");
        }

        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname().trim() : dto.getUsername());
        user.setEmail(StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : dto.getUsername());
        user.setPhone(StringUtils.hasText(dto.getPhone()) ? dto.getPhone().trim() : null);
        user.setRoleId(role.getId());
        user.setStatus(0);
        sysUserMapper.insert(user);

        // 创建扩展profile
        try {
            createProfile(user.getId(), role.getRoleCode(), dto);
        } catch (Exception e) {
            log.error("创建用户profile失败: userId={}, roleCode={}, error={}", user.getId(), role.getRoleCode(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 管理员更新用户信息
     */
    @Transactional
    public void update(Long userId, AdminUserUpdateDTO dto) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        SysRole role = sysRoleMapper.selectById(user.getRoleId());
        if (role != null && "ROLE_ADMIN".equals(role.getRoleCode())) {
            throw BusinessException.badRequest("不允许编辑管理员账号");
        }

        // 更新基础信息
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname().trim());
        }
        if (dto.getEmail() != null) {
            SysUser exist = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getEmail, dto.getEmail())
                            .ne(SysUser::getId, userId));
            if (exist != null) {
                throw BusinessException.badRequest("该邮箱已被其他用户使用");
            }
            user.setEmail(dto.getEmail().trim());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone().trim());
        }
        sysUserMapper.updateById(user);

        // 更新扩展profile
        if (role != null) {
            updateProfile(userId, role.getRoleCode(), dto);
        }
    }

    /**
     * 切换用户状态（启用/禁用）
     */
    public void toggleStatus(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        SysRole role = sysRoleMapper.selectById(user.getRoleId());
        if (role != null && "ROLE_ADMIN".equals(role.getRoleCode())) {
            throw BusinessException.badRequest("不允许操作管理员账号");
        }
        user.setStatus(user.getStatus() == 0 ? 1 : 0);
        sysUserMapper.updateById(user);
    }

    /**
     * 重置用户密码
     */
    public void resetPassword(Long userId, AdminPasswordResetDTO dto) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        sysUserMapper.updateById(user);
    }

    /**
     * 删除用户（软删除用户及关联profile）
     */
    @Transactional
    public void deleteUser(Long userId, Long currentAdminId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (userId.equals(currentAdminId)) {
            throw BusinessException.badRequest("不能删除自己的账号");
        }
        SysRole role = sysRoleMapper.selectById(user.getRoleId());
        if (role != null && "ROLE_ADMIN".equals(role.getRoleCode())) {
            throw BusinessException.badRequest("不允许删除管理员账号");
        }

        // 删除扩展profile
        if (role != null) {
            if ("ROLE_TEACHER".equals(role.getRoleCode())) {
                TeacherProfile tp = teacherProfileMapper.selectOne(
                        new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getUserId, userId));
                if (tp != null) {
                    teacherProfileMapper.deleteById(tp.getId());
                }
            } else if ("ROLE_STUDENT".equals(role.getRoleCode())) {
                StudentProfile sp = studentProfileMapper.selectOne(
                        new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, userId));
                if (sp != null) {
                    studentProfileMapper.deleteById(sp.getId());
                }
            }
        }
        sysUserMapper.deleteById(userId);
    }

    // ==================== 私有辅助方法 ====================

    private AdminUserVO toVO(SysUser user, Map<Long, SysRole> roleMap,
            Map<Long, TeacherProfile> teacherMap, Map<Long, StudentProfile> studentMap) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setRoleId(user.getRoleId());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());

        SysRole role = roleMap.get(user.getRoleId());
        if (role != null) {
            vo.setRoleCode(role.getRoleCode());
            vo.setRoleName(role.getRoleName());
        }

        // 填充扩展字段
        if (role != null && "ROLE_TEACHER".equals(role.getRoleCode())) {
            TeacherProfile tp = teacherMap.get(user.getId());
            if (tp != null) {
                vo.setRealName(tp.getRealName());
                vo.setTitle(tp.getTitle());
                vo.setDepartment(tp.getDepartment());
            }
        } else if (role != null && "ROLE_STUDENT".equals(role.getRoleCode())) {
            StudentProfile sp = studentMap.get(user.getId());
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

    private void fillProfile(AdminUserVO vo, Long userId, SysRole role) {
        if (role == null) return;
        if ("ROLE_TEACHER".equals(role.getRoleCode())) {
            TeacherProfile tp = teacherProfileMapper.selectOne(
                    new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getUserId, userId));
            if (tp != null) {
                vo.setRealName(tp.getRealName());
                vo.setTitle(tp.getTitle());
                vo.setDepartment(tp.getDepartment());
            }
        } else if ("ROLE_STUDENT".equals(role.getRoleCode())) {
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
    }

    private void createProfile(Long userId, String roleCode, AdminUserCreateDTO dto) {
        if ("ROLE_TEACHER".equals(roleCode)) {
            TeacherProfile profile = new TeacherProfile();
            profile.setUserId(userId);
            profile.setRealName(clean(dto.getRealName()));
            profile.setTitle(clean(dto.getTitle()));
            profile.setDepartment(clean(dto.getDepartment()));
            teacherProfileMapper.insert(profile);
        } else if ("ROLE_STUDENT".equals(roleCode)) {
            String studentNo = StringUtils.hasText(dto.getStudentNo()) ? dto.getStudentNo().trim() : generateStudentNo();
            if (StringUtils.hasText(dto.getStudentNo()) && studentProfileMapper.selectOne(
                    new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getStudentNo, studentNo)) != null) {
                throw BusinessException.badRequest("该学号已被使用");
            }
            StudentProfile profile = new StudentProfile();
            profile.setUserId(userId);
            profile.setStudentNo(studentNo);
            profile.setRealName(clean(dto.getRealName()));
            profile.setGrade(clean(dto.getGrade()));
            profile.setMajor(clean(dto.getMajor()));
            profile.setClassName(clean(dto.getClassName()));
            studentProfileMapper.insert(profile);
        }
    }

    private void updateProfile(Long userId, String roleCode, AdminUserUpdateDTO dto) {
        if ("ROLE_TEACHER".equals(roleCode)) {
            TeacherProfile tp = teacherProfileMapper.selectOne(
                    new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getUserId, userId));
            if (tp == null) {
                tp = new TeacherProfile();
                tp.setUserId(userId);
            }
            if (dto.getRealName() != null) tp.setRealName(dto.getRealName().trim());
            if (dto.getTitle() != null) tp.setTitle(dto.getTitle().trim());
            if (dto.getDepartment() != null) tp.setDepartment(dto.getDepartment().trim());
            if (tp.getId() != null) {
                teacherProfileMapper.updateById(tp);
            } else {
                teacherProfileMapper.insert(tp);
            }
        } else if ("ROLE_STUDENT".equals(roleCode)) {
            StudentProfile sp = studentProfileMapper.selectOne(
                    new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, userId));
            if (sp == null) {
                sp = new StudentProfile();
                sp.setUserId(userId);
            }
            if (dto.getRealName() != null) sp.setRealName(dto.getRealName().trim());
            if (dto.getStudentNo() != null) {
                StudentProfile exist = studentProfileMapper.selectOne(
                        new LambdaQueryWrapper<StudentProfile>()
                                .eq(StudentProfile::getStudentNo, dto.getStudentNo())
                                .ne(StudentProfile::getUserId, userId));
                if (exist != null) {
                    throw BusinessException.badRequest("该学号已被使用");
                }
                sp.setStudentNo(dto.getStudentNo().trim());
            }
            if (dto.getGrade() != null) sp.setGrade(dto.getGrade().trim());
            if (dto.getMajor() != null) sp.setMajor(dto.getMajor().trim());
            if (dto.getClassName() != null) sp.setClassName(dto.getClassName().trim());
            if (sp.getId() != null) {
                studentProfileMapper.updateById(sp);
            } else {
                studentProfileMapper.insert(sp);
            }
        }
    }

    private String clean(String value) {
        return value != null && !value.trim().isEmpty() ? value.trim() : null;
    }

    private String generateStudentNo() {
        String year = String.valueOf(LocalDate.now().getYear());
        String maxNo = studentProfileMapper.selectMaxStudentNo();
        long nextSeq = 1;
        if (maxNo != null && maxNo.startsWith(year)) {
            try {
                nextSeq = Long.parseLong(maxNo.substring(year.length())) + 1;
            } catch (NumberFormatException ignored) {
                // 解析失败则从 1 开始
            }
        }
        return year + String.format("%04d", nextSeq % 10000);
    }
}
