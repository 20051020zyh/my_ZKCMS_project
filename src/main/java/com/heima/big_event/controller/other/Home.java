package com.heima.big_event.controller.other;

import com.heima.big_event.pojo.Result;
import com.heima.big_event.pojo.SysNotice;
import com.heima.big_event.service.system.SysConfigService;
import com.heima.big_event.service.system.SysNoticeService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Validated
@RequestMapping("/index")
public class Home {
    @Autowired
    private SysConfigService sysConfigService;
    @Autowired
    private SysNoticeService sysNoticeService;


    //返回类
    @Data
    static class PopInfoVO {
        private String type; // MAINTENANCE / NOTICE / NONE
        private String msg;
        private SysNotice notice;
    }

    //控制优先级:维护>公告
    @GetMapping("/popInfo")
    public Result<PopInfoVO> getIndexPopInfo() {
        //先判断维护模式是否开启
        boolean statusImpl = sysConfigService.getStatusImpl();
        PopInfoVO vo = new PopInfoVO();

        if (statusImpl) {
            //维护模式开启,只返回维护弹窗
            vo.setType("MAINTENANCE");
            vo.setMsg("系统正在维护中,暂时无法操作");
            return Result.success(vo);
        }

        //没有维护,查询最新的公告
        SysNotice newNoticeImpl = sysNoticeService.getNewNoticeImpl();
        if (newNoticeImpl != null) {
            vo.setType("NOTICE");
            vo.setMsg("请查收新公告");
            vo.setNotice(newNoticeImpl);
        } else {
            vo.setType("NONE");
        }
        return Result.success(vo);
    }
}
