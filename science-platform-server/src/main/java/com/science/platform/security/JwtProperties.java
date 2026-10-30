package com.science.platform.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置项，对应 application.yml 中的 {@code app.jwt}。
 *
 * <p>密钥不写入版本库：本地配置放在 application-local.yml（已被 .gitignore 忽略），
 * 生产环境建议通过环境变量 {@code APP_JWT_SECRET} 注入。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** HMAC 签名密钥，长度至少 32 字节（HS256 要求） */
    private String secret = "";

    /** 令牌有效期（分钟） */
    private long expireMinutes = 120;

    /** 签发者标识，写入 payload 的 iss */
    private String issuer = "science-platform";
}
