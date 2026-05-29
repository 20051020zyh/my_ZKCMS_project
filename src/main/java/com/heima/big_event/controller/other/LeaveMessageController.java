package com.heima.big_event.controller.other;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.big_event.pojo.LeaveMessage;
import com.heima.big_event.pojo.Result;
import com.heima.big_event.service.others.LeaveMessageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leaveMessage")
@Validated
public class LeaveMessageController {

    @Autowired
    private LeaveMessageService leaveMessageService;

    //留言功能
    @PostMapping("/add")
    public Result add(@Valid @RequestBody LeaveMessage message) {
        leaveMessageService.add(message);
        return Result.success("留言成功");
    }

    //分页查询留言
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize) {
        Page<LeaveMessage> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<LeaveMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(LeaveMessage::getCreateTime);
        Page<LeaveMessage> result = leaveMessageService.page(page, wrapper);
        return Result.success(result);
    }

    //批量删除留言
    @PostMapping("/batchDelete")
    public Result batchDelte(@RequestBody List<Integer> ids){
        if (ids == null || ids.isEmpty()){
            return Result.error("请选择要删除的留言");
        }
        leaveMessageService.batchDelete(ids);
        return Result.success("删除成功");
    }
}
