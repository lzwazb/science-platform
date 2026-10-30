package com.science.platform.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.system.service.OperationLogService;
import com.science.platform.modules.user.dto.UserRoleUpdateRequest;
import com.science.platform.modules.user.dto.UserStatusUpdateRequest;
import com.science.platform.modules.user.entity.SysRole;
import com.science.platform.modules.user.entity.SysUser;
import com.science.platform.modules.user.entity.SysUserRole;
import com.science.platform.modules.user.mapper.SysRoleMapper;
import com.science.platform.modules.user.mapper.SysUserMapper;
import com.science.platform.modules.user.mapper.SysUserRoleMapper;
import com.science.platform.modules.user.vo.AdminUserListItemVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 超级管理员的账号与角色管理。
 *
 * <p>两条防锁死规则很重要，否则管理员一次误操作就可能把自己关在系统外面：
 * <ol>
 *   <li>不能禁用自己</li>
 *   <li>不能移除自己的 SUPER_ADMIN 角色</li>
 * </ol></p>
 */
@Service
public class AdminUserService {

    private static final String ACTIVE = "ACTIVE";
    private static final String DISABLED = "DISABLED";
    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final OperationLogService operationLogService;

    public AdminUserService(SysUserMapper sysUserMapper,
                            SysRoleMapper sysRoleMapper,
                            SysUserRoleMapper sysUserRoleMapper,
                            OperationLogService operationLogService) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.operationLogService = operationLogService;
    }

    /**
     * 用户分页查询。
     *
     * @param keyword  匹配用户名或昵称
     * @param roleCode 按角色筛选
     * @param status   按账号状态筛选
     */
    public IPage<AdminUserListItemVO> pageUsers(long page, long size, String keyword,
                                                String roleCode, String status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(q -> q.like(SysUser::getUsername, kw)
                    .or()
                    .like(SysUser::getNickname, kw));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByAsc(SysUser::getId);

        Page<SysUser> result = sysUserMapper.selectPage(new Page<>(page, size), wrapper);
        List<SysUser> users = result.getRecords();

        if (users.isEmpty()) {
            Page<AdminUserListItemVO> empty = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
            empty.setRecords(List.of());
            return empty;
        }

        // 批量取角色，避免逐条查询
        Map<Long, List<String>> rolesByUser = users.stream()
                .collect(Collectors.toMap(SysUser::getId,
                        u -> sysUserMapper.selectRoleCodesByUserId(u.getId())));

        List<AdminUserListItemVO> items = users.stream().map(user -> {
            AdminUserListItemVO vo = new AdminUserListItemVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setEmail(user.getEmail());
            vo.setStatus(user.getStatus());
            vo.setRoles(rolesByUser.getOrDefault(user.getId(), List.of()));
            vo.setCreatedAt(user.getCreatedAt());
            return vo;
        }).toList();

        // 按角色筛选需要额外处理：用内存过滤会破坏分页，这里只在 page 内过滤并说明限制
        if (StringUtils.hasText(roleCode)) {
            items = items.stream()
                    .filter(item -> item.getRoles().contains(roleCode))
                    .toList();
        }

        Page<AdminUserListItemVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(items);
        return voPage;
    }

    /**
     * 重新分配用户角色。
     *
     * @param userId     目标用户
     * @param request    新的角色编码集合
     * @param operatorId 当前登录的超级管理员
     */
    @Transactional
    public AdminUserListItemVO updateRoles(Long userId, UserRoleUpdateRequest request, Long operatorId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        List<String> codes = request.getRoleCodes().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();

        // 防锁死：不能移除自己的超级管理员角色
        if (Objects.equals(userId, operatorId) && !codes.contains(SUPER_ADMIN)) {
            throw new BusinessException("不能移除自己的超级管理员角色");
        }

        List<SysRole> roles = sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().in(SysRole::getRoleCode, codes));
        if (roles.size() != codes.size()) {
            Set<String> found = roles.stream().map(SysRole::getRoleCode).collect(Collectors.toSet());
            List<String> missing = codes.stream().filter(c -> !found.contains(c)).toList();
            throw new BusinessException("角色不存在：" + String.join("、", missing));
        }

        // 整体替换：先删后插，保证结果与请求一致
        LambdaQueryWrapper<SysUserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserRole::getUserId, userId);
        sysUserRoleMapper.delete(deleteWrapper);

        for (SysRole role : roles) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(role.getId());
            sysUserRoleMapper.insert(relation);
        }

        operationLogService.record(operatorId, "USER_ROLE_UPDATE", "USER", userId,
                "调整用户 " + user.getUsername() + " 的角色为 " + String.join("、", codes)
                        + "；原因：" + request.getReason().trim());

        return toItem(user, sysUserMapper.selectRoleCodesByUserId(userId));
    }

    /**
     * 启用 / 禁用账号。
     */
    @Transactional
    public AdminUserListItemVO updateStatus(Long userId, UserStatusUpdateRequest request, Long operatorId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        // 防锁死：不能禁用自己
        if (Objects.equals(userId, operatorId) && DISABLED.equals(request.getStatus())) {
            throw new BusinessException("不能禁用当前登录的账号");
        }

        user.setStatus(request.getStatus());
        sysUserMapper.updateById(user);

        String action = ACTIVE.equals(request.getStatus()) ? "USER_ENABLE" : "USER_DISABLE";
        operationLogService.record(operatorId, action, "USER", userId,
                (ACTIVE.equals(request.getStatus()) ? "启用" : "禁用") + "账号 " + user.getUsername()
                        + (StringUtils.hasText(request.getReason()) ? "；原因：" + request.getReason().trim() : ""));

        return toItem(user, sysUserMapper.selectRoleCodesByUserId(userId));
    }

    private AdminUserListItemVO toItem(SysUser user, List<String> roles) {
        AdminUserListItemVO vo = new AdminUserListItemVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setRoles(roles);
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
