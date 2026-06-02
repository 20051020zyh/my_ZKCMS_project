package com.heima.big_event.service.dataStatistics;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.big_event.pojo.BrowserDeviceStat;
import com.heima.big_event.pojo.VO.BrowserDeviceStatsVO;

public interface BrowserDeviceStatService extends IService<BrowserDeviceStat> {
    
    BrowserDeviceStatsVO getStats(Integer days);
    
    void aggregateYesterdayStats();
}
