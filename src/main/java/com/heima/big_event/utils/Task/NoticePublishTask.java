package com.heima.big_event.utils.Task;

import com.heima.big_event.pojo.SysNotice;
import com.heima.big_event.service.system.SysNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class NoticePublishTask {
    // 日志记录（方便排查问题）
    private static final Logger log = LoggerFactory.getLogger(NoticePublishTask.class);

    @Autowired
    private SysNoticeService sysNoticeService;

    /**
     * 定时发布公告：每分钟执行一次，每次只处理一条
     */
    @Scheduled(cron = "0 * * * * ?")
    public void publishScheduledNotice() {
        try {
            // 1. 查询一条待发布的公告
            SysNotice notice = sysNoticeService.getOneScheduledNotice();
            if (notice == null) {
                log.info("暂无待发布的定时公告");
                return;
            }

            // 2. 处理发布（修改状态为已发布）
            boolean success = sysNoticeService.publishNotice(notice.getId());
            if (success) {
                log.info("定时公告发布成功，公告ID：{}，标题：{}", notice.getId(), notice.getTitle());
            } else {
                log.error("定时公告发布失败，公告ID：{}", notice.getId());
            }
        } catch (Exception e) {
            log.error("定时发布公告任务执行异常", e);
        }
    }
}
