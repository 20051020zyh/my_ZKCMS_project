package com.heima.big_event.service.impl.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.big_event.exception.BusinessException;
import com.heima.big_event.mapper.system.SysNoticeMapper;
import com.heima.big_event.pojo.SysNotice;
import com.heima.big_event.service.system.SysNoticeService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service("sysNoticeService")
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements SysNoticeService {


    @Override
    public boolean saveNotice(SysNotice sysNotice) {
        // 1. 基础校验（如果是定时发布，必须传publishTime且大于当前时间）
        if (sysNotice.getStatus() == 2) { // 状态为2：定时发布
            if (sysNotice.getPublishTime() == null) {
                throw new BusinessException("定时发布必须填写发布时间");
            }
            if (sysNotice.getPublishTime().isBefore(LocalDateTime.now())) {
                throw new BusinessException("定时发布时间必须晚于当前时间");
            }
        }
        // 直接发布时，如果未设置发布时间则自动设为当前时间
        if (sysNotice.getStatus() == 1 && sysNotice.getPublishTime() == null) {
            sysNotice.setPublishTime(LocalDateTime.now());
        }
        // 2. 保存完整对象到数据库（不管什么状态，先保存）
        return this.save(sysNotice);
    }

    /**
     * 定时任务专用：查询一条待发布的公告(状态2+发布时间<=现在)
     * @return 单条待发布公告
     */
    @Override
    public SysNotice getOneScheduledNotice() {
        return this.getOne(Wrappers.<SysNotice>lambdaQuery()
                .eq(SysNotice::getStatus, 2) // 状态=2(定时发布)
                .le(SysNotice::getPublishTime, LocalDateTime.now()) // 发布时间<=当前时间
                .last("LIMIT 1") // 只查一条
        );
    }

    /**
     * 更新公告状态为已发布
     * @param noticeId 公告ID
     * @return 是否更新成功
     */
    @Override
    public boolean publishNotice(Long noticeId) {
        SysNotice notice = new SysNotice();
        notice.setId(noticeId);
        notice.setStatus(1); // 改为已发布状态
        notice.setPublishTime(LocalDateTime.now()); // 设置发布时间
        return this.updateById(notice);
    }



    //获取最新的已发布的公告
    @Override
    public SysNotice getNewNoticeImpl(){
        Page<SysNotice> page = new Page<>(1,1);
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotice::getStatus , 1)
                .orderByDesc(SysNotice::getPublishTime);
        Page<SysNotice> sysNoticePage = this.page(page, wrapper);
        SysNotice notice = sysNoticePage.getRecords().isEmpty() ? null : sysNoticePage.getRecords().get(0);
        return notice;
    }

}

