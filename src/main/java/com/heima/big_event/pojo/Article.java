package com.heima.big_event.pojo;


import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.heima.big_event.anno.State;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("article")
public class Article {
    @TableId(type = IdType.AUTO)//主键自增
    private Integer id;//主键ID
    @NotBlank(message = "文章标题不能为空")
    @Pattern(regexp = "^\\S{1,100}$", message = "文章标题内容为1~100个字符")
    private String title;//文章标题
    private String content;//文章内容
    //@NotBlank(message = "封面不能为空")
    //@URL
    private String coverImg;//封面图像
    @NotBlank(message = "发布状态不能是空")
    @State
    private String state;//发布状态 已发布|草稿
    private Integer categoryId;//文章分类id
    private Integer createUser;//创建人ID
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;//创建时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;//更新时间
    private Integer viewCount;//浏览量
    private Integer likeCount;// 点赞总数
    private Integer collectCount;// 收藏总数
    private Integer isHot;// 是否热门 0=否,1=是
    private Integer isBest;// 是否精选 0=否,1=是
    private Integer commentCount;//评论数
    @JsonAlias("scheduleTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")//指定时间格式
    private LocalDateTime publishTime;
    private Integer isDelete;//回收站判断是否删除
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime deleteTime;//移入回收站的时间
    private String seoTitle;//seo标题2
    private String seoDescription;//seo描述内容
    private String seoKeywords;//seo关键词
    private Integer status;//文章的状态

    @TableField(exist = false)
    private String username;

    @TableField(exist = false)
    private String user_pic;

    @TableField(exist = false)
    private List<String> tagNames;
}
