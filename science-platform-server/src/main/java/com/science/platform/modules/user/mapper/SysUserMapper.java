package com.science.platform.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.science.platform.modules.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户 Mapper。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 查询用户拥有的角色编码，按角色 id 升序。
     *
     * <p>联表一次取回，避免「先查关联表再逐一查角色」的 N+1 问题。</p>
     *
     * @param userId 用户 ID
     * @return 角色编码列表，如 [USER, CREATOR]
     */
    @Select("""
            SELECT r.role_code
              FROM sys_role r
              JOIN sys_user_role ur ON ur.role_id = r.id
             WHERE ur.user_id = #{userId}
             ORDER BY r.id
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}
