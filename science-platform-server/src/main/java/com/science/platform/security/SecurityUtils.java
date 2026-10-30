package com.science.platform.security;

import com.science.platform.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 Spring Security 上下文取当前登录用户的工具类。
 *
 * <p>所有需要「操作者身份」的地方都通过这里获取，**不接受前端传入的 userId / roleCode**。</p>
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户。
     *
     * @return 登录主体
     * @throws BusinessException 未登录时抛出 401，由全局异常处理器转成统一响应
     */
    public static LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        throw new BusinessException(401, "登录状态已失效，请重新登录");
    }

    /**
     * 获取当前登录用户 ID。
     */
    public static Long currentUserId() {
        return currentUser().getId();
    }
}
