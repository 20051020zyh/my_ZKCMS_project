package com.heima.big_event.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.big_event.pojo.SysNotice;

public interface SysNoticeService extends IService<SysNotice> {

    boolean saveNotice(SysNotice sysNotice);

    SysNotice getOneScheduledNotice();

    boolean publishNotice(Long noticeId);

    SysNotice getNewNoticeImpl();
}

