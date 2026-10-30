package com.science.platform.modules.system;

import com.science.platform.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口，用于验证后端启动状态与前后端连通性。
 *
 * <p>开发档案附录 A 要求：建立统一响应结构和 GET /api/v1/health 健康检查接口。</p>
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${spring.application.name:science-platform-server}")
    private String applicationName;

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("application", applicationName);
        data.put("project", "基于 Spring Boot + Vue 的科普知识共创与创意传播平台");
        data.put("stage", "阶段 1：项目骨架");
        data.put("serverTime", LocalDateTime.now().format(TIME_FORMAT));
        return ApiResponse.success(data);
    }
}
