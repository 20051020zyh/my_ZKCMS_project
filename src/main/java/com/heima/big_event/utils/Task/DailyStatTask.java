package com.heima.big_event.utils.Task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.big_event.mapper.dataStatistics.DailyStatMapper;
import com.heima.big_event.mapper.dataStatistics.VisitLogMapper;
import com.heima.big_event.mapper.article.ArticleMapper;
import com.heima.big_event.mapper.user.UserMapper;
import com.heima.big_event.pojo.Article;
import com.heima.big_event.pojo.DailyStat;
import com.heima.big_event.pojo.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class DailyStatTask {

    @Autowired
    private VisitLogMapper visitLogMapper;
    @Autowired
    private DailyStatMapper dailyStatMapper;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private UserMapper userMapper;

    // 每天凌晨1点执行
    @Scheduled(cron = "0 0 1 * * ?")
    public void computeDailyStat() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // 1. 统计访问数据（MySQL COUNT返回Long，需通过Number安全转型）
        Map<String, Object> visitStat = visitLogMapper.statVisit(yesterday);
        Integer pv = ((Number) visitStat.getOrDefault("pv", 0)).intValue();
        Integer uv = ((Number) visitStat.getOrDefault("uv", 0)).intValue();
        Integer ipCount = ((Number) visitStat.getOrDefault("ipCount", 0)).intValue();

        // 2. 统计发文数据
        Long publishCount = articleMapper.selectCount(
                Wrappers.lambdaQuery(Article.class)
                        .between(Article::getCreateTime, yesterday.atStartOfDay(), yesterday.plusDays(1).atStartOfDay())
        );

        // 3. 统计新增用户
        Long newUserCount = userMapper.selectCount(
                Wrappers.lambdaQuery(User.class)
                        .between(User::getCreateTime, yesterday.atStartOfDay(), yesterday.plusDays(1).atStartOfDay())
        );

        // 4. 插入或更新统计表
        DailyStat stat = new DailyStat();
        stat.setStatDate(yesterday);
        stat.setPv(pv);
        stat.setUv(uv);
        stat.setIpCount(ipCount);
        stat.setPublishCount(publishCount);
        stat.setUserCount(newUserCount != null ? newUserCount.intValue() : 0);

        LambdaQueryWrapper<DailyStat> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DailyStat::getStatDate, yesterday);
        if (dailyStatMapper.selectCount(wrapper) > 0) {
            dailyStatMapper.update(stat, wrapper);
        } else {
            dailyStatMapper.insert(stat);
        }
    }
}
