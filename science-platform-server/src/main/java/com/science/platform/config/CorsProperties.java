package com.science.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 跨域白名单配置，对应 application.yml 中的 {@code app.cors}。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /** 允许访问的来源，开发期为本机 Vite 开发服务器 */
    private List<String> allowedOrigins = List.of("http://localhost:5173");
}
