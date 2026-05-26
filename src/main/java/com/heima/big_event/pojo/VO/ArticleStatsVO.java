package com.heima.big_event.pojo.VO;

import lombok.Data;

@Data
public class ArticleStatsVO {
    private Long totalArticles;
    private Long publishedCount;
    private Long draftCount;
    private Long totalViews;
    private Long totalLikes;
    private Long totalCollects;
}