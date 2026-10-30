package com.science.platform.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.science.platform.modules.user.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 角色 Mapper。
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 按角色编码查询角色。
     *
     * @param roleCode 角色编码，如 USER
     * @return 角色实体；不存在时返回 null
     */
    @Select("SELECT * FROM sys_role WHERE role_code = #{roleCode}")
    SysRole selectByRoleCode(@Param("roleCode") String roleCode);
}
