package com.heima.big_event.controller.other;

import com.heima.big_event.pojo.BaiduHotItem;
import com.heima.big_event.pojo.Result;
import com.heima.big_event.service.baidu.BaiduHotSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 百度热搜接口（无需登录）
 */
@Slf4j
@RestController
@RequestMapping("/baidu")
public class BaiduHotSearchController {

    @Autowired
    private BaiduHotSearchService baiduHotSearchService;

    /**
     * 获取百度实时热搜 Top 10
     * GET /baidu/hot
     */
    @GetMapping("/hot")
    public Result<List<BaiduHotItem>> getHotSearch() {
        List<BaiduHotItem> list = baiduHotSearchService.getHotSearchList();
        return Result.success(list);
    }
}
