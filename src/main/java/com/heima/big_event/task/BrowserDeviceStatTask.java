package com.heima.big_event.task;

import com.heima.big_event.service.dataStatistics.BrowserDeviceStatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BrowserDeviceStatTask {
    
    @Autowired
    private BrowserDeviceStatService browserDeviceStatService;
    
    @Scheduled(cron = "0 0 1 * * ?")
    public void aggregateDailyStats() {
        log.info("开始执行浏览器设备统计聚合任务...");
        try {
            browserDeviceStatService.aggregateYesterdayStats();
            log.info("浏览器设备统计聚合任务执行完成");
        } catch (Exception e) {
            log.error("浏览器设备统计聚合任务执行失败", e);
        }
    }
}
