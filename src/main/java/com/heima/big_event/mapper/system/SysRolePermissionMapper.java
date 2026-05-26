package com.heima.big_event.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.heima.big_event.pojo.SysRolePermission;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * (SysRolePermission)表数据库访问层（Mapper）
 *
 * @author makejava
 * @since 2026-05-12 17:02:39
 */

@Mapper  // 新增你需要的@Mapper注解
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {
    // 根据 roleId 列表查询 permission_id 列表
    @Select("<script>" +
            "SELECT permission_id FROM sys_role_permission WHERE role_id IN " +
            "<foreach collection='roleIds' item='rid' open='(' separator=',' close=')'>" +
            "#{rid}" +
            "</foreach>" +
            "</script>")
    List<Long> selectPermissionIdsByRoleIds(@Param("roleIds") List<Long> roleIds);
}
