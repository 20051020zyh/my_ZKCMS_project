package com.heima.big_event.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.heima.big_event.pojo.SysRole;

/**
 * (SysRole)表数据库访问层（Mapper）
 *
 * @author makejava
 * @since 2026-05-12 17:02:39
 */

@Mapper  // 新增你需要的@Mapper注解
public interface SysRoleMapper extends BaseMapper<SysRole> {

}
