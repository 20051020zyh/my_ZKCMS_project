package com.heima.big_event.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.heima.big_event.pojo.SysUserPermission;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * (SysUserPermission)表数据库访问层（Mapper）
 *
 * @author makejava
 * @since 2026-05-23
 */

@Mapper
public interface SysUserPermissionMapper extends BaseMapper<SysUserPermission> {
    // 根据 userId 查询 permissionId 列表
    @Select("SELECT permission_id FROM sys_user_permission WHERE user_id = #{userId}")
    List<Long> selectPermissionIdsByUserId(@Param("userId") Long userId);
}
