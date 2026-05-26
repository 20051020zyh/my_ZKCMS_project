package com.heima.big_event.pojo.VO;

import jakarta.validation.constraints.Email;
import lombok.Data;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;

@Data
public class ArticleCenterInfoVO {
    private String username;
    private String nickname;
    private String email;
    private String user_pic;
    //已发布文章数
    private Long fabuCount;
    //草稿文章数
    private Long caogaoCount;
    //我的评论总数
    private Long commentCount;
    //我的收藏总数
    private Long collectCount;

}
