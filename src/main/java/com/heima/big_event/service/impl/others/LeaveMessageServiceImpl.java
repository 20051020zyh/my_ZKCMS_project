package com.heima.big_event.service.impl.others;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.big_event.mapper.LeaveMessageMapper;
import com.heima.big_event.pojo.LeaveMessage;
import com.heima.big_event.service.others.LeaveMessageService;
import com.heima.big_event.utils.Others.EmailUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service("leaveMessageService")
public class LeaveMessageServiceImpl extends ServiceImpl<LeaveMessageMapper, LeaveMessage> implements LeaveMessageService {

    private static final Logger log = LoggerFactory.getLogger(LeaveMessageServiceImpl.class);

    @Autowired
    private LeaveMessageMapper leaveMessageMapper;

    @Autowired
    private EmailUtil emailUtil;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(LeaveMessage message) {
        message.setCreateTime(LocalDateTime.now());
        leaveMessageMapper.insert(message);

        new Thread(() -> {
            try {
                emailUtil.sendMsg(message);
            } catch (Exception e) {
                log.error("发送留言通知邮件失败, messageId={}", message.getId(), e);
            }
        }).start();
    }
}

