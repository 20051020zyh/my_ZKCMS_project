package com.heima.big_event.pojo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Data
@Slf4j
public class CommentLike {
    @TableId(type = IdType.AUTO)//告诉MP这是一个自增主键
    private Integer id;
    private Integer commentId;
    private Integer userId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
