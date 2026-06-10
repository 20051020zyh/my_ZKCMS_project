package com.heima.big_event.task;

import com.heima.big_event.service.baidu.BaiduHotSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 百度热搜定时刷新任务
 *
 * <p>核心设计：
 * <ul>
 *   <li>{@code initialDelay = 0}  —— 服务启动后立即执行一次，确保前端有数据可显示</li>
 *   <li>{@code fixedDelay}（非 fixedRate）—— 下一次执行在上一次执行<b>完成后</b>
 *       才开始计时，天然避免两次调用时间重叠</li>
 *   <li>若上一次刷新失败（超时 / 接口异常），旧缓存不会被清除（TTL = 20 min），
 *       下一个周期仍会重新尝试拉取</li>
 * </ul>
 */
@Slf4j
@Component
public class BaiduHotSearchTask {

    /** 刷新间隔：10 分钟（毫秒） */
    private static final long REFRESH_INTERVAL_MS = 10 * 60 * 1000L;

    @Autowired
    private BaiduHotSearchService baiduHotSearchService;

    /**
     * 启动即执行（initialDelay = 0），
     * 之后每次在上一次执行完成后等待 10 分钟再执行下一次。
     */
    @Scheduled(fixedDelay = REFRESH_INTERVAL_MS, initialDelay = 0)
    public void refreshBaiduHotSearch() {
        log.info("[百度热搜] 开始刷新...");
        boolean success = baiduHotSearchService.refreshHotSearch();
        if (success) {
            log.info("[百度热搜] 刷新完成，已更新 Redis 缓存");
        } else {
            log.warn("[百度热搜] 本次刷新失败，旧缓存保持不变，等待下一周期重试");
        }
    }
}
