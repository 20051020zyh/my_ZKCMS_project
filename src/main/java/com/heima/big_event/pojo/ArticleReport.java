package com.heima.big_event.pojo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.core.util.Json;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleReport {
    @TableId(type = IdType.AUTO)//告诉MP这是一个自增主键
    private Integer id;//主键id
    private Integer articleId;//被举报的文章id
    private Integer reportType;//举报类型
    private String content;//举报描述
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;//传入的数组图片
    private Integer userId;//举报人id
    private Integer status;//审核状态
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;//创建时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;//更新时间
}
