package com.heima.big_event.service.impl.dataStatistics;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.big_event.mapper.dataStatistics.DailyStatMapper;
import com.heima.big_event.pojo.DailyStat;
import com.heima.big_event.pojo.VO.ArticleTrendVO;
import com.heima.big_event.service.dataStatistics.DailyStatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service("dailyStatService")
public class DailyStatServiceImpl extends ServiceImpl<DailyStatMapper, DailyStat> implements DailyStatService {
    @Autowired
    private DailyStatMapper dailyStatMapper;
    //近7/30天的访问趋势
    @Override
    public ArticleTrendVO getTrend(Integer days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);

        // 1. 生成连续日期列表
        List<String> dateList = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            dateList.add(start.plusDays(i).toString());
        }

        // 2. 查询统计数据
        List<DailyStat> statList = dailyStatMapper.selectByDateRange(start, end);
        Map<String, DailyStat> statMap = statList.stream()
                .collect(Collectors.toMap(stat -> stat.getStatDate().toString(), Function.identity()));

        // 3. 组装数据并补0
        List<Integer> pvList = new ArrayList<>();
        List<Long> publishList = new ArrayList<>();
        List<Integer> userList = new ArrayList<>();
        for (String date : dateList) {
            DailyStat stat = statMap.getOrDefault(date, new DailyStat());
            pvList.add(stat.getPv() == null ? 0 : stat.getPv());
            publishList.add(stat.getPublishCount() == null ? 0 : stat.getPublishCount());
            userList.add(stat.getUserCount() == null ? 0 : stat.getUserCount());
        }

        ArticleTrendVO vo = new ArticleTrendVO();
        vo.setDateList(dateList);
        vo.setViewList(pvList);
        vo.setPublishList(publishList);
        vo.setUserList(userList);
        return vo;
    }


}

