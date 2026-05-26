package com.heima.big_event.service.dataStatistics;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.big_event.pojo.DailyStat;
import com.heima.big_event.pojo.VO.ArticleTrendVO;

/**
 * 每日统计表(DailyStat)表服务接口
 *
 * @author makejava
 * @since 2026-05-14 13:48:28
 */
public interface DailyStatService extends IService<DailyStat> {

    //近7/30天的访问趋势
    ArticleTrendVO getTrend(Integer days);
}

