package com.science.platform.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.user.dto.UpdateProfileRequest;
import com.science.platform.modules.user.entity.SysUser;
import com.science.platform.modules.user.mapper.SysUserMapper;
import com.science.platform.modules.user.vo.UserProfileVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户资料业务逻辑。
 *
 * <p>所有方法都以「当前登录用户 ID」为入口，Controller 负责从认证上下文取 ID，
 * 不接受前端传入 userId，避免越权读取他人资料。</p>
 */
@Service
public class UserService {

    private final SysUserMapper sysUserMapper;

    public UserService(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 查询用户资料。
     *
     * @param userId 用户 ID
     * @return 资料视图，含角色
     * @throws BusinessException 用户不存在时抛出 404
     */
    public UserProfileVO getProfile(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return toVO(user, sysUserMapper.selectRoleCodesByUserId(userId));
    }

    /**
     * 更新可编辑的个人资料。
     *
     * <p>只接受昵称、头像、邮箱、简介四个字段；用户名与角色不在本方法职责内。</p>
     *
     * @param userId  用户 ID
     * @param request 更新内容，字段为 null 表示不修改
     * @return 更新后的资料
     */
    @Transactional
    public UserProfileVO updateProfile(Long userId, UpdateProfileRequest request) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        if (request.getNickname() != null) {
            if (!StringUtils.hasText(request.getNickname())) {
                throw new BusinessException("昵称不能为空");
            }
            user.setNickname(request.getNickname().trim());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar().trim());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail().trim());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio().trim());
        }

        sysUserMapper.updateById(user);
        return toVO(user, sysUserMapper.selectRoleCodesByUserId(userId));
    }

    /**
     * 查询用户名是否已被占用。
     *
     * <p>只用于注册前的友好提示；并发场景下仍以数据库唯一约束为准。</p>
     */
    public boolean isUsernameTaken(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        return sysUserMapper.selectCount(wrapper) > 0;
    }

    /**
     * 实体转视图对象。
     */
    public UserProfileVO toVO(SysUser user, List<String> roles) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setBio(user.getBio());
        vo.setStatus(user.getStatus());
        vo.setRoles(roles);
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
