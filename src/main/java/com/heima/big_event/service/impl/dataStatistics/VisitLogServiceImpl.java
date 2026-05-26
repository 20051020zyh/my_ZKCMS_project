package com.heima.big_event.service.impl.dataStatistics;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.big_event.mapper.dataStatistics.VisitLogMapper;
import com.heima.big_event.pojo.VisitLog;
import com.heima.big_event.service.dataStatistics.VisitLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

//访问日志表实现类
@Service("visitLogService")
public class VisitLogServiceImpl extends ServiceImpl<VisitLogMapper, VisitLog> implements VisitLogService {
    @Autowired
    private VisitLogMapper visitLogMapper;

    @Async
    @Override
    public void asyncSaveLog(Long articleId, Integer userId, String ip, String userAgent) {
        VisitLog log = new VisitLog();
        log.setBizType("article");
        log.setBizId(articleId);
        log.setUserId(userId);
        log.setIp(ip);
        log.setUserAgent(userAgent);
        log.setCreateTime(LocalDateTime.now());
        visitLogMapper.insert(log);
    }
}

