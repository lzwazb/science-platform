package com.science.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * JWT 生成与解析。
 *
 * <p>令牌只承载身份与角色，不放敏感信息；服务端不保存会话，属于无状态认证。</p>
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    /** HS256 要求密钥长度不少于 256 位，即 32 字节 */
    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties properties;
    private SecretKey key;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        String secret = properties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret 未配置或长度不足 " + MIN_SECRET_BYTES + " 字节。"
                            + "请在 application-local.yml 中配置，或设置环境变量 APP_JWT_SECRET。");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("JWT 签名密钥已加载，令牌有效期 {} 分钟", properties.getExpireMinutes());
    }

    /**
     * 生成访问令牌。
     *
     * @param userId   用户 ID，写入 sub
     * @param username 登录名
     * @param roles    角色编码列表
     * @return 紧凑格式的 JWT 字符串
     */
    public String generateToken(Long userId, String username, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.getExpireMinutes() * 60);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("roles", roles)
                .issuer(properties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验令牌签名与有效期。
     *
     * @param token 令牌字符串
     * @return 载荷；令牌非法或过期时返回 null
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            // 签名不符、结构损坏、已过期都归为「令牌不可用」，不向调用方区分原因
            log.debug("JWT 校验失败: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * 令牌有效期（秒），用于登录响应告知前端。
     */
    public long getExpireSeconds() {
        return properties.getExpireMinutes() * 60;
    }
}
