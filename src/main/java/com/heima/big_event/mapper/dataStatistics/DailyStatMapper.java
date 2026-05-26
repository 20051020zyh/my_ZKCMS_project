package com.heima.big_event.mapper.dataStatistics;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.big_event.pojo.DailyStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DailyStatMapper extends BaseMapper<DailyStat> {
    @Select("SELECT * FROM daily_stat " +
            "WHERE stat_date BETWEEN #{start} AND #{end} " +
            "ORDER BY stat_date ASC")
    List<DailyStat> selectByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
