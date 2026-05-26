package com.heima.big_event.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class AddArticleTagDTO {
    private String title;//文章标题
    private String content;//文章内容
    private String cover_img;//图像地址
    private String state;//文章状态
    private Integer categoryId;//分类id
    private List<String> tagNameList;//前端返回的标签集合
    private String seoTitle;//seo标题2
    private String seoDescription;//seo描述内容
    private String seoKeywords;//seo关键词
}
