package com.heima.big_event.service.impl.others;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.big_event.mapper.LeaveMessageMapper;
import com.heima.big_event.pojo.LeaveMessage;
import com.heima.big_event.service.others.LeaveMessageService;
import com.heima.big_event.utils.Others.EmailUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 留言表(LeaveMessage)表服务实现类
 *
 * @author makejava
 * @since 2026-05-17 19:01:09
 */
@Service("leaveMessageService")
public class LeaveMessageServiceImpl extends ServiceImpl<LeaveMessageMapper, LeaveMessage> implements LeaveMessageService {
    @Autowired
    private LeaveMessageMapper leaveMessageMapper;

    @Autowired
    private EmailUtil emailUtil;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(LeaveMessage message) {
        // 存入数据库
        message.setCreateTime(LocalDateTime.now());
        leaveMessageMapper.insert(message);

        // 异步发邮件（不阻塞页面）
        new Thread(() -> {
            try {
                emailUtil.sendMsg(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}

