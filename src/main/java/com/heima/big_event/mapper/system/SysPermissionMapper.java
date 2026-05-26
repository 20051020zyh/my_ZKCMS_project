package com.heima.big_event.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.heima.big_event.pojo.SysPermission;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * (SysPermission)表数据库访问层（Mapper）
 *
 * @author makejava
 * @since 2026-05-12 17:02:39
 */

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {
        // 根据 permission_id 列表查询 permission 字段（权限标识）
        @Select("<script>" +
                "SELECT permission FROM sys_permission WHERE id IN " +
                "<foreach collection='permIds' item='pid' open='(' separator=',' close=')'>" +
                "#{pid}" +
                "</foreach>" +
                "</script>")
        List<String> selectPermissionsByIds(@Param("permIds") List<Long> permIds);

}
