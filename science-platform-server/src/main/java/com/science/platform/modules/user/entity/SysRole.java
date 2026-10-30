package com.science.platform.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色字典实体。
 */
@Data
@TableName("sys_role")
public class SysRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** USER / CREATOR / REVIEWER / ADMIN / SUPER_ADMIN */
    private String roleCode;

    private String roleName;

    private String description;

    private Integer sortOrder;

    private LocalDateTime createdAt;
}
