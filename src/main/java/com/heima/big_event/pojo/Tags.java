package com.heima.big_event.pojo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Tags {
    @TableId(type = IdType.AUTO)//告诉MP这是一个自增主键
    private Long id;
    private Integer sort = 0;
    private String name;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
