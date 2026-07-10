package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.PageResult;
import com.kechang.entity.OperationLog;
import com.kechang.entity.SysRole;
import com.kechang.entity.SysUser;
import com.kechang.mapper.OperationLogMapper;
import com.kechang.mapper.SysRoleMapper;
import com.kechang.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper logMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    public void record(Long operatorId, String module, String action, String targetType,
            Long targetId, String targetName, String description) {
        OperationLog log = new OperationLog();
        log.setOperatorId(operatorId);
        SysUser user = operatorId == null ? null : userMapper.selectById(operatorId);
        if (user != null) {
            log.setOperatorName(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
            SysRole role = user.getRoleId() == null ? null : roleMapper.selectById(user.getRoleId());
            log.setRoleCode(role != null ? role.getRoleCode() : null);
        }
        log.setModule(module);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setTargetName(targetName);
        log.setDescription(description);
        logMapper.insert(log);
    }

    public PageResult<OperationLog> list(int page, int size, String module, String action) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(module)) {
            wrapper.eq(OperationLog::getModule, module);
        }
        if (StringUtils.hasText(action)) {
            wrapper.eq(OperationLog::getAction, action);
        }
        wrapper.orderByDesc(OperationLog::getCreateTime);
        Page<OperationLog> result = logMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public Map<String, Long> overview() {
        return Map.of(
                "total", logMapper.selectCount(new LambdaQueryWrapper<OperationLog>()),
                "today", logMapper.selectCount(new LambdaQueryWrapper<OperationLog>()
                        .apply("DATE(create_time) = CURDATE()"))
        );
    }
}
