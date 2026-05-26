package com.heima.big_event.mapper.dataStatistics;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.big_event.pojo.VisitLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.Map;

@Mapper
public interface VisitLogMapper extends BaseMapper<VisitLog> {
    // 统计某一天的PV、UV、IP数
    @Select("SELECT " +
            "COUNT(*) AS pv, " +
            "COUNT(DISTINCT user_id) AS uv, " +
            "COUNT(DISTINCT ip) AS ipCount " +
            "FROM visit_log " +
            "WHERE DATE(create_time) = #{date}")
    Map<String, Object> statVisit(@Param("date") LocalDate date);
}
