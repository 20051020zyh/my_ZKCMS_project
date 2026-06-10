package com.heima.big_event.service.baidu;

import com.heima.big_event.pojo.BaiduHotItem;

import java.util.List;

/**
 * 百度热搜服务接口
 */
public interface BaiduHotSearchService {

    /**
     * 拉取百度实时热搜并刷新 Redis 缓存
     * 成功返回 true，失败返回 false（不影响旧缓存）
     */
    boolean refreshHotSearch();

    /**
     * 从 Redis 读取当前热搜列表（最多 10 条）
     */
    List<BaiduHotItem> getHotSearchList();
}
