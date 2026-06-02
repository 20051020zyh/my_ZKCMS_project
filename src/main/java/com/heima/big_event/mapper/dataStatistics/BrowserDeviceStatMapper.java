package com.heima.big_event.mapper.dataStatistics;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.big_event.pojo.BrowserDeviceStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface BrowserDeviceStatMapper extends BaseMapper<BrowserDeviceStat> {
    
    @Select("SELECT browser, SUM(count) as total_count " +
            "FROM browser_device_stat " +
            "WHERE stat_date >= #{startDate} " +
            "GROUP BY browser " +
            "ORDER BY total_count DESC")
    List<Map<String, Object>> statByBrowser(@Param("startDate") LocalDate startDate);
    
    @Select("SELECT device_type, SUM(count) as total_count " +
            "FROM browser_device_stat " +
            "WHERE stat_date >= #{startDate} " +
            "GROUP BY device_type " +
            "ORDER BY total_count DESC")
    List<Map<String, Object>> statByDevice(@Param("startDate") LocalDate startDate);
    
    @Select("SELECT os, SUM(count) as total_count " +
            "FROM browser_device_stat " +
            "WHERE stat_date >= #{startDate} " +
            "GROUP BY os " +
            "ORDER BY total_count DESC")
    List<Map<String, Object>> statByOs(@Param("startDate") LocalDate startDate);
    
    @Select("SELECT CONCAT(browser, ' / ', os) as label, SUM(count) as total_count " +
            "FROM browser_device_stat " +
            "WHERE stat_date >= #{startDate} " +
            "GROUP BY browser, os " +
            "ORDER BY total_count DESC " +
            "LIMIT 10")
    List<Map<String, Object>> statByBrowserAndOs(@Param("startDate") LocalDate startDate);
}
