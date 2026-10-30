package com.science.platform.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置（对应开发档案 6.1：集中配置分页、逻辑删除和字段填充）。
 *
 * <p>逻辑删除与主键策略已在 application.yml 的 mybatis-plus.global-config 中声明，
 * 此处只注册分页插件。</p>
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页上限，防止前端传入超大 size 拖垮数据库
        pagination.setMaxLimit(100L);
        // 页码超出总页数时返回空列表，而不是跳到第一页，避免分页行为难以预期
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);

        return interceptor;
    }
}
