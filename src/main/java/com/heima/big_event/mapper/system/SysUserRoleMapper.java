package com.heima.big_event.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.heima.big_event.pojo.SysUserRole;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * (SysUserRole)表数据库访问层（Mapper）
 *
 * @author makejava
 * @since 2026-05-12 17:02:39
 */

@Mapper  // 新增你需要的@Mapper注解
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
    // 根据 userId 查询 roleId 列表
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(Long userId);
}
