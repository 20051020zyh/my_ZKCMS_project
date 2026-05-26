package com.heima.big_event.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class ArticleTag {
    @TableId(type = IdType.AUTO)//告诉MP这是一个自增主键
    private Integer id;
    private Integer articleId;
    private Integer tagId;
}
