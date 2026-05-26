package com.heima.big_event.pojo.VO;

import lombok.Data;

@Data
public class ArticleLikeVO {
    //这个类和评论点赞类共用
    private Integer likeCount;
    private Boolean isLike;
}
