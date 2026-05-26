package com.heima.big_event.pojo.VO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleRankVO {
    private Integer id;           // 文章ID
    private String title;      // 文章标题
    private Integer viewCount; // 阅读量
    private Integer likeCount; // 点赞数
    private Integer collectCount; // 收藏数
    private LocalDateTime createTime;   // 创建时间
}
