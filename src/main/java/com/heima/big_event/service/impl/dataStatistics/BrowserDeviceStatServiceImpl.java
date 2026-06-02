package com.heima.big_event.service.impl.dataStatistics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.big_event.mapper.dataStatistics.BrowserDeviceStatMapper;
import com.heima.big_event.mapper.dataStatistics.VisitLogMapper;
import com.heima.big_event.pojo.BrowserDeviceStat;
import com.heima.big_event.pojo.VisitLog;
import com.heima.big_event.pojo.VO.BrowserDeviceStatsVO;
import com.heima.big_event.service.dataStatistics.BrowserDeviceStatService;
import com.heima.big_event.utils.UserAgentParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service("browserDeviceStatService")
public class BrowserDeviceStatServiceImpl extends ServiceImpl<BrowserDeviceStatMapper, BrowserDeviceStat> 
        implements BrowserDeviceStatService {
    
    @Autowired
    private BrowserDeviceStatMapper browserDeviceStatMapper;
    
    @Autowired
    private VisitLogMapper visitLogMapper;
    
    @Override
    public BrowserDeviceStatsVO getStats(Integer days) {
        if (days == null || days <= 0) {
            days = 7;
        }
        
        LocalDate startDate = LocalDate.now().minusDays(days);
        
        BrowserDeviceStatsVO vo = new BrowserDeviceStatsVO();
        vo.setBrowserStats(browserDeviceStatMapper.statByBrowser(startDate));
        vo.setDeviceStats(browserDeviceStatMapper.statByDevice(startDate));
        vo.setOsStats(browserDeviceStatMapper.statByOs(startDate));
        vo.setBrowserOsStats(browserDeviceStatMapper.statByBrowserAndOs(startDate));
        
        return vo;
    }
    
    @Override
    @Transactional
    public void aggregateYesterdayStats() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime start = yesterday.atStartOfDay();
        LocalDateTime end = yesterday.atTime(23, 59, 59);
        
        List<VisitLog> logs = visitLogMapper.selectList(
            new LambdaQueryWrapper<VisitLog>()
                .between(VisitLog::getCreateTime, start, end)
        );
        
        if (logs.isEmpty()) {
            return;
        }
        //Stream 分组聚合，代码更简洁、性能更高
        Map<String, Long> grouped = logs.stream()
                .map(log -> UserAgentParser.parse(log.getUserAgent()))
                .collect(Collectors.groupingBy(
                        m -> m.get("browser") + "|" + m.get("deviceType") + "|" + m.get("os"),
                        Collectors.counting()
                ));
        
        //使用Mybatis-plus saveBatch本身是批量插入，数据库压力很小
        List<BrowserDeviceStat> stats = grouped.entrySet().stream().map(entry -> {
            String[] parts = entry.getKey().split("\\|");
            BrowserDeviceStat stat = new BrowserDeviceStat();
            stat.setStatDate(yesterday);
            stat.setBrowser(parts[0]);
            stat.setDeviceType(parts[1]);
            stat.setOs(parts[2]);
            stat.setCount(entry.getValue().intValue());
            return stat;
        }).toList();
    
        saveBatch(stats);
    }
}
