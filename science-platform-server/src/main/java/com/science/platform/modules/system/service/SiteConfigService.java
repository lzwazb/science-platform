package com.science.platform.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.science.platform.modules.system.dto.SiteSettingsRequest;
import com.science.platform.modules.system.entity.SysConfig;
import com.science.platform.modules.system.mapper.SysConfigMapper;
import com.science.platform.modules.system.service.OperationLogService;
import com.science.platform.modules.system.vo.SiteInfoVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台基础配置的读写。
 *
 * <p>刻意不做内存缓存：配置只有几条，按唯一键查询极快；
 * 一旦加缓存就要处理"改完配置首页还在显示旧值"的一致性问题，得不偿失。</p>
 */
@Service
public class SiteConfigService {

    /** 配置键常量，避免各处硬编码字符串 */
    public static final String KEY_NAME = "site.name";
    public static final String KEY_SUBTITLE = "site.subtitle";
    public static final String KEY_DESCRIPTION = "site.description";
    public static final String KEY_BANNER = "site.banner";
    public static final String KEY_CONTACT = "site.contact";

    private static final Map<String, String> DEFAULT_VALUES = Map.of(
            KEY_NAME, "科普知识共创与创意传播平台",
            KEY_SUBTITLE, "Knowledge Co-creation & Creative Communication",
            KEY_DESCRIPTION, "科普知识浏览、知识共建纠错、创意作品与主题活动。",
            KEY_BANNER, "发现不准确？提交纠错，让知识更靠谱",
            KEY_CONTACT, ""
    );

    private final SysConfigMapper configMapper;
    private final OperationLogService operationLogService;

    public SiteConfigService(SysConfigMapper configMapper, OperationLogService operationLogService) {
        this.configMapper = configMapper;
        this.operationLogService = operationLogService;
    }

    /**
     * 读取全部配置为 Map。
     */
    public Map<String, String> loadAll() {
        Map<String, String> result = new HashMap<>(DEFAULT_VALUES);
        List<SysConfig> rows = configMapper.selectList(null);
        for (SysConfig row : rows) {
            if (StringUtils.hasText(row.getConfigKey())) {
                result.put(row.getConfigKey(), row.getConfigValue() == null ? "" : row.getConfigValue());
            }
        }
        return result;
    }

    /**
     * 组装前台首页需要的平台信息（不含公告，由调用方补充）。
     */
    public SiteInfoVO loadSiteInfo() {
        Map<String, String> config = loadAll();
        SiteInfoVO vo = new SiteInfoVO();
        vo.setName(config.get(KEY_NAME));
        vo.setSubtitle(config.get(KEY_SUBTITLE));
        vo.setDescription(config.get(KEY_DESCRIPTION));
        vo.setBanner(config.get(KEY_BANNER));
        vo.setContact(config.get(KEY_CONTACT));
        return vo;
    }

    /**
     * 更新平台基础信息（超级管理员）。
     *
     * <p>逐项 upsert：配置项可能不存在（例如以后新增的键），存在则更新、不存在则插入。</p>
     */
    @Transactional
    public SiteInfoVO update(SiteSettingsRequest request, Long operatorId) {
        upsert(KEY_NAME, request.getName().trim(), "平台名称，首页与浏览器标题使用", operatorId);
        upsert(KEY_SUBTITLE, trimToEmpty(request.getSubtitle()), "平台副标题（英文）", operatorId);
        upsert(KEY_DESCRIPTION, request.getDescription().trim(), "平台简介，首页展示", operatorId);
        upsert(KEY_BANNER, trimToEmpty(request.getBanner()), "首页横幅文案", operatorId);
        upsert(KEY_CONTACT, trimToEmpty(request.getContact()), "联系邮箱（可选）", operatorId);

        operationLogService.record(operatorId, "SITE_SETTINGS_UPDATE", "SYSTEM", null,
                "更新平台基础设置：名称=" + request.getName().trim());

        return loadSiteInfo();
    }

    private void upsert(String key, String value, String description, Long operatorId) {
        LambdaQueryWrapper<SysConfig> query = new LambdaQueryWrapper<>();
        query.eq(SysConfig::getConfigKey, key);
        SysConfig existing = configMapper.selectOne(query);

        if (existing == null) {
            SysConfig config = new SysConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription(description);
            config.setUpdatedBy(operatorId);
            configMapper.insert(config);
            return;
        }

        LambdaUpdateWrapper<SysConfig> update = new LambdaUpdateWrapper<>();
        update.eq(SysConfig::getId, existing.getId())
                .set(SysConfig::getConfigValue, value)
                .set(SysConfig::getUpdatedBy, operatorId)
                .set(SysConfig::getUpdatedAt, LocalDateTime.now());
        configMapper.update(null, update);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
