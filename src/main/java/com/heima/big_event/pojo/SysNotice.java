package com.heima.big_event.pojo;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import com.heima.big_event.anno.State;
import java.time.LocalDateTime;

@Data
@TableName("sys_notice")
public class SysNotice {
        @TableId(type = IdType.AUTO)
        private Long id;
        @NotBlank(message = "公告标题不能为空")
        @Pattern(regexp = "^\\S{1,100}$", message = "文章标题内容为1~10个字符")
        private String title;
        @NotBlank(message = "公告不能为空")
        private String content;//公告内容
        @NotBlank(message = "公告类型不能为空")
        private String noticeType;//公告类型 NORMAL-普通 MAINTENANCE-维护
        private Integer status;//状态 0=草稿 1=已发布 2=定时发布
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")//指定时间格式
        private LocalDateTime publishTime;//发布时间
        private Long createBy;//创建人
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createTime;//创建时间
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updateTime;//更新时间
    
    
    }
