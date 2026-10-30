package com.science.platform.modules.user.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户资料展示对象。
 *
 * <p>刻意不含 passwordHash、deleted 等内部字段。</p>
 */
@Data
public class UserProfileVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String email;

    private String bio;

    /** ACTIVE / DISABLED */
    private String status;

    /** 角色编码列表，如 [USER, CREATOR] */
    private List<String> roles;

    private LocalDateTime createdAt;
}
