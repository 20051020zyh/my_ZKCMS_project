package com.heima.big_event.pojo;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import com.heima.big_event.anno.State;
import java.time.LocalDateTime;

/**
 * 留言表(LeaveMessage)表实体类
 *
 * @author makejava
 * @since 2026-05-17 18:55:06
 */
@Data
@TableName("leave_message")
public class LeaveMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    @NotBlank(message = "姓名不能为空")
    @Pattern(regexp = "^\\S{1,50}$", message = "姓名为1~50个字符")
    private String name;
    @NotBlank(message = "电话号码不能为空")
    @Pattern(regexp = "^\\S{11}$", message = "电话号码应为11个字符")
    private String phone;
    @NotBlank(message = "邮箱不能为空")
    @Email
    private String email;
    @NotBlank(message = "内容不能为空")
    private String content;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    
    }
