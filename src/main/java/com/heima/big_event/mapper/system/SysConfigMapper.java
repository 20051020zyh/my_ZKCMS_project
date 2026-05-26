package com.heima.big_event.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.heima.big_event.pojo.SysConfig;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统全局配置表(SysConfig)表数据库访问层（Mapper）
 *
 * @author makejava
 * @since 2026-05-14 16:09:00
 */

@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {
    // 根据key查配置值
    @Select("select config_value from sys_config where config_key = #{configKey}")
    String getValueByKey(@Param("configKey") String configKey);
}
