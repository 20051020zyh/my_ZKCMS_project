package com.heima.big_event.utils.Others;

import com.heima.big_event.pojo.LeaveMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

    //发送留言通知
    @Component
    public class EmailUtil {

        @Autowired
        private JavaMailSender mailSender;

        @Value("${spring.mail.username}")
        private String fromEmail;

        @Value("${system.notify.email}")
        private String toEmail;

        public void sendMsg(LeaveMessage message) {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(toEmail);
            msg.setSubject("【网站新留言】");

            String content = "你收到一条新留言：\n" +
                    "姓名：" + message.getName() + "\n" +
                    "电话：" + message.getPhone() + "\n" +
                    "邮箱：" + message.getEmail() + "\n" +
                    "内容：" + message.getContent();

            msg.setText(content);
            mailSender.send(msg);
        }
    }
