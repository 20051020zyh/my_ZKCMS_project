package com.heima.big_event.service.dataStatistics;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.big_event.pojo.VisitLog;
import org.springframework.scheduling.annotation.Async;


/**
 * 访问日志表(VisitLog)表服务接口
 *
 * @author makejava
 * @since 2026-05-14 13:46:51
 */
public interface VisitLogService extends IService<VisitLog> {


    @Async
    void asyncSaveLog(Long articleId, Integer userId, String ip, String userAgent);
}

