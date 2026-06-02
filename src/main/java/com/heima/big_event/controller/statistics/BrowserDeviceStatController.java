package com.heima.big_event.controller.statistics;

import com.heima.big_event.pojo.Result;
import com.heima.big_event.pojo.VO.BrowserDeviceStatsVO;
import com.heima.big_event.service.dataStatistics.BrowserDeviceStatService;
import com.heima.big_event.utils.Permission.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "浏览器设备统计")
@RestController
@RequestMapping("/statistics")
public class BrowserDeviceStatController {
    
    @Autowired
    private BrowserDeviceStatService browserDeviceStatService;
    
    @Operation(summary = "获取浏览器设备统计")
    @GetMapping("/browser-device")
    @RequirePermission(value = "/statistics/browser-device", checkPermission = false)
    public Result<BrowserDeviceStatsVO> getBrowserDeviceStats(
            @RequestParam(defaultValue = "7") Integer days) {
        if (!days.equals(7) && !days.equals(30) && !days.equals(90)) {
            return Result.error("仅支持查询7天、30天或90天的数据");
        }
        return Result.success(browserDeviceStatService.getStats(days));
    }
    
    @Operation(summary = "手动触发昨日统计聚合")
    @PostMapping("/aggregate")
    @RequirePermission("/statistics/aggregate")
    public Result aggregateYesterday() {
        browserDeviceStatService.aggregateYesterdayStats();
        return Result.success("聚合任务已执行");
    }
}
