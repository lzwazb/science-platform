package com.science.platform.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 重要操作日志实体。
 *
 * <p>记录审核、发布、删除等关键动作，满足开发档案第 10 章「重要操作需有记录」。</p>
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long operatorId;

    /** 动作编码，如 CONTRIBUTION_APPROVE */
    private String action;

    private String targetType;

    private Long targetId;

    /** 中文描述，便于后台直接展示 */
    private String detail;

    private LocalDateTime createdAt;
}
