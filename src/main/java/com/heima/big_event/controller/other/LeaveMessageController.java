package com.heima.big_event.controller.other;

import com.heima.big_event.pojo.LeaveMessage;
import com.heima.big_event.pojo.Result;
import com.heima.big_event.service.others.LeaveMessageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/leaveMessage")
@Validated
public class LeaveMessageController {

    @Autowired
    private LeaveMessageService leaveMessageService;

    @PostMapping("/add")
    public Result add(@Valid @RequestBody LeaveMessage message) {
        leaveMessageService.add(message);
        return Result.success("留言成功");
    }
}
