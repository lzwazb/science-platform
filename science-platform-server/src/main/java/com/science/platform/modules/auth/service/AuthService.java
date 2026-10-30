package com.science.platform.modules.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.auth.dto.LoginRequest;
import com.science.platform.modules.auth.dto.RegisterRequest;
import com.science.platform.modules.auth.vo.LoginResponse;
import com.science.platform.modules.user.entity.SysRole;
import com.science.platform.modules.user.entity.SysUser;
import com.science.platform.modules.user.entity.SysUserRole;
import com.science.platform.modules.user.mapper.SysRoleMapper;
import com.science.platform.modules.user.mapper.SysUserMapper;
import com.science.platform.modules.user.mapper.SysUserRoleMapper;
import com.science.platform.modules.user.service.UserService;
import com.science.platform.modules.user.vo.UserProfileVO;
import com.science.platform.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 注册与登录业务逻辑。
 *
 * <p>安全约定：
 * <ul>
 *   <li>密码只存 BCrypt 哈希，日志中不出现明文</li>
 *   <li>登录失败统一提示「用户名或密码错误」，不暴露账号是否存在</li>
 *   <li>公开注册一律赋 USER 角色，高权限账号只能由超级管理员创建</li>
 * </ul>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final String DEFAULT_ROLE = "USER";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(SysUserMapper sysUserMapper,
                       SysRoleMapper sysRoleMapper,
                       SysUserRoleMapper sysUserRoleMapper,
                       UserService userService,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    /**
     * 公开注册普通用户。
     *
     * @param request 注册信息
     * @return 新用户资料（不含令牌，前端注册成功后引导登录）
     */
    @Transactional
    public UserProfileVO register(RegisterRequest request) {
        String username = request.getUsername().trim();

        if (userService.isUsernameTaken(username)) {
            throw new BusinessException("用户名已被占用");
        }

        SysRole defaultRole = sysRoleMapper.selectByRoleCode(DEFAULT_ROLE);
        if (defaultRole == null) {
            // 属于部署问题而非用户输入问题，明确抛出便于排查
            throw new BusinessException(500, "系统未初始化默认角色，请先执行 seed.sql");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname().trim());
        user.setEmail(request.getEmail() == null ? null : request.getEmail().trim());
        user.setStatus(STATUS_ACTIVE);

        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            // 并发注册同名账号时，唯一索引是最后一道防线
            throw new BusinessException("用户名已被占用");
        }

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(defaultRole.getId());
        sysUserRoleMapper.insert(userRole);

        log.info("新用户注册成功: id={}, username={}", user.getId(), username);
        return userService.toVO(user, List.of(DEFAULT_ROLE));
    }

    /**
     * 登录并签发令牌。
     *
     * @param request 登录信息
     * @return 令牌与用户信息
     */
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername().trim();

        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // 两种失败原因合并为同一提示，避免被用来枚举账号
            log.info("登录失败（账号或密码不匹配）: username={}", username);
            throw new BusinessException(401, "用户名或密码错误");
        }

        if (!STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }

        List<String> roles = sysUserMapper.selectRoleCodesByUserId(user.getId());
        String token = tokenProvider.generateToken(user.getId(), user.getUsername(), roles);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setExpiresIn(tokenProvider.getExpireSeconds());
        response.setUser(userService.toVO(user, roles));

        log.info("登录成功: id={}, username={}, roles={}", user.getId(), username, roles);
        return response;
    }
}
