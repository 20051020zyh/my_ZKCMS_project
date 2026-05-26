package com.heima.big_event.controller.system;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heima.big_event.pojo.SysNotice;
import com.heima.big_event.pojo.Result;
import com.heima.big_event.pojo.VO.SysNoticeVO;
import com.heima.big_event.service.system.SysNoticeService;
import com.heima.big_event.utils.Permission.RequirePermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Validated
@RequestMapping("/sysNotice")
public class SysNoticeController {

    @Autowired
    private SysNoticeService sysNoticeService;

    //新增公告
    @PostMapping("/add")
    @RequirePermission("sysNotice/add")
    public Result add(@RequestBody SysNotice sysNotice) {
        try {
            boolean save = sysNoticeService.saveNotice(sysNotice);
            return save ? Result.success("系统公告新增成功") : Result.error("系统公告新增失败");
        } catch (RuntimeException e) {
            // 捕获参数校验的异常，返回友好提示
            return Result.error(e.getMessage());
        }
    }

    //批量删除公告
    @DeleteMapping("/deleteList")
    @RequirePermission("sysNotice/deleteList")
    @Transactional
    public Result delete(@RequestBody List<Integer> ids) {
        if (ids == null || ids.isEmpty()){
            return Result.error("删除系统公告失败");
        }
        for (Integer id : ids){
            sysNoticeService.removeById(id);
        }
        return Result.success("批量删除系统公告成功");
    }

    //查询所有的系统公告
    @GetMapping("/adimin/list")
    @RequirePermission("sysNotice/adimin/list")
    public Result adminList() {
        return Result.success(sysNoticeService.list());
    }

    //查询近3个月的系统公告
    //先创建个VO类,把要返回给用户看的字段,然后加个查询近3个月的记录
    @GetMapping("/user/list")
    @RequirePermission("sysNotice/user/list")
    public Result userList(){
        //获取当前的时间
        LocalDateTime now = LocalDateTime.now();
        //往前推3个月
        LocalDateTime threeMonthsAgo = now.minusMonths(3);

        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotice::getStatus , 1)
                .ge(SysNotice::getCreateTime , threeMonthsAgo)
                .orderByDesc(SysNotice::getCreateTime);

        List<SysNotice> sysNoticeList = sysNoticeService.list(wrapper);

        //stram流把数据进行加功
        List<SysNoticeVO> voList = sysNoticeList.stream()
                .map(notice -> {
                    SysNoticeVO vo = new SysNoticeVO();
                    vo.setTitle(notice.getTitle());
                    vo.setContent(notice.getContent());
                    vo.setPublishTime(notice.getPublishTime());
                    vo.setCreateTime(notice.getCreateTime());
                    return vo;
                }).collect(Collectors.toList());

        return Result.success(voList);
    }

    //获取最新的已发布的公告
    @GetMapping("/get/new")
    public Result<SysNotice> getNewNotice(){
        SysNotice newNoticeImpl = sysNoticeService.getNewNoticeImpl();
        return Result.success(newNoticeImpl);
    }

}
