package com.heima.big_event.service.others;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.big_event.pojo.LeaveMessage;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 留言表(LeaveMessage)表服务接口
 *
 * @author makejava
 * @since 2026-05-17 19:01:09
 */
public interface LeaveMessageService extends IService<LeaveMessage> {

    @Transactional(rollbackFor = Exception.class)
    void add(LeaveMessage message);

    //批量删除留言
    @Transactional(rollbackFor = Exception.class)
    void batchDelete(List<Integer> ids);
}

