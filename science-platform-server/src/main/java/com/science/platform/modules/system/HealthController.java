package com.science.platform.modules.system;

import com.science.platform.common.ApiResponse;
import com.science.platform.modules.system.service.SystemHealthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口，用于验证后端启动状态、数据库连通与前后端连通性。
 *
 * <p>开发档案附录 A 要求：建立统一响应结构和 GET /api/v1/health 健康检查接口。</p>
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SystemHealthService systemHealthService;

    @Value("${spring.application.name:science-platform-server}")
    private String applicationName;

    public HealthController(SystemHealthService systemHealthService) {
        this.systemHealthService = systemHealthService;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("application", applicationName);
        data.put("project", "基于 Spring Boot + Vue 的科普知识共创与创意传播平台");
        data.put("stage", "阶段 2：数据库与基础规范");
        data.put("serverTime", LocalDateTime.now().format(TIME_FORMAT));

        // 真实访问数据库，失败时由全局异常处理器返回 500，避免「服务在跑但数据库断开」被误报为健康
        Map<String, Object> database = new LinkedHashMap<>();
        database.put("status", "UP");
        database.put("database", "science_platform");
        database.put("publishedArticleCount", systemHealthService.countPublishedArticles());
        data.put("db", database);

        return ApiResponse.success(data);
    }
}
