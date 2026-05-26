package com.heima.big_event.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class ArticleStatusDTO {
    //文章id数组
    private List<Integer> articleId;
    // 文章状态（draft/published）
    private String state;
}
