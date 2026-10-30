package com.science.platform.security;

import com.science.platform.modules.user.entity.SysUser;
import com.science.platform.modules.user.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：从 Authorization 头解析令牌并写入 Spring Security 上下文。
 *
 * <p>设计取舍：每次请求都用数据库校验一次用户状态与角色，而不是只信任令牌里的角色声明。
 * 这样管理员禁用账号或调整角色后立即生效，不必等令牌过期。
 * 代价是每个受保护请求多一次主键查询，在本科毕设规模下可以接受。</p>
 *
 * <p>令牌缺失或非法时不抛异常、不写认证信息，由 Security 的授权规则决定是 401 还是直接放行
 * （公开接口即使携带失效令牌仍可访问）。</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final JwtTokenProvider tokenProvider;
    private final SysUserMapper sysUserMapper;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, SysUserMapper sysUserMapper) {
        this.tokenProvider = tokenProvider;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);
        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = tokenProvider.parseToken(token);
            if (claims != null) {
                authenticate(request, claims);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, Claims claims) {
        Long userId;
        try {
            userId = Long.valueOf(claims.getSubject());
        } catch (NumberFormatException ex) {
            log.debug("令牌 sub 不是合法的用户 ID: {}", claims.getSubject());
            return;
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !STATUS_ACTIVE.equals(user.getStatus())) {
            // 用户已删除或被禁用：令牌不再有效，不建立认证上下文
            log.debug("令牌对应用户 {} 不存在或已被禁用，忽略该令牌", userId);
            return;
        }

        List<String> roles = sysUserMapper.selectRoleCodesByUserId(userId);
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getNickname(), roles);

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
                .toList();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (StringUtils.hasText(header) && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length()).trim();
        }
        return null;
    }
}
