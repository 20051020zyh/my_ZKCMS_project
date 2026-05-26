package com.heima.big_event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.heima.big_event.pojo.LeaveMessage;

/**
 * 留言表(LeaveMessage)表数据库访问层（Mapper）
 *
 * @author makejava
 * @since 2026-05-17 18:55:06
 */

@Mapper  // 新增你需要的@Mapper注解
public interface LeaveMessageMapper extends BaseMapper<LeaveMessage> {

}
