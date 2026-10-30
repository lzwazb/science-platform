package com.science.platform.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * 安全配置：无状态 JWT 认证 + 基于角色的接口授权。
 *
 * <p>授权顺序遵循「越具体越靠前」，后面的规则不会覆盖前面的。</p>
 *
 * <p>注意：前端菜单按角色隐藏只是体验优化，真正的权限边界在这里。</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/health",
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/error"
    };

    /** 允许游客浏览的只读接口（阶段 1、4 起逐步补充） */
    private static final String[] PUBLIC_GET_ENDPOINTS = {
            "/api/v1/articles/**",
            "/api/v1/categories/**",
            "/api/v1/topics/**",
            "/api/v1/works/**",
            "/api/v1/events/**",
            "/api/v1/comments/**",
            // 详情页需要展示点赞/收藏计数，游客可读；liked/favorited 对游客恒为 false
            "/api/v1/interactions/status"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler,
                                                   CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                // 前后端分离 + 无状态令牌，不需要 CSRF 令牌
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // 预检请求直接放行，否则浏览器拿不到 CORS 响应头
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // 我的投稿属于个人数据，不能被 /events/** 的公开规则放行，必须写在前面
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/*/submissions/**").authenticated()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                        // 账号与角色管理只对超级管理员开放
                        .requestMatchers("/api/v1/admin/users/**").hasRole("SUPER_ADMIN")
                        // 其余后台接口需要管理员及以上
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // 创作者中心：创作者、管理员、超级管理员
                        .requestMatchers("/api/v1/creator/**").hasAnyRole("CREATOR", "ADMIN", "SUPER_ADMIN")
                        // 投稿：创作者及以上
                        .requestMatchers(HttpMethod.POST, "/api/v1/events/*/submissions")
                        .hasAnyRole("CREATOR", "ADMIN", "SUPER_ADMIN")
                        // 审核工作台：评审员、管理员、超级管理员
                        .requestMatchers("/api/v1/review/**").hasAnyRole("REVIEWER", "ADMIN", "SUPER_ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码编码器。BCrypt 自带随机盐，同一明文每次哈希结果不同；
     * seed.sql 中的 {@code $2b$} 前缀同样被本编码器识别。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
