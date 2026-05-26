package com.heima.big_event.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_like")
public class ArticleLike {
    //点赞表
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer articleId;//文章的id
    private Integer userId;//点赞用户id
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;//点赞时间
}
