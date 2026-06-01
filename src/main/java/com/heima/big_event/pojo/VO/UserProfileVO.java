package com.heima.big_event.pojo.VO;

import com.heima.big_event.pojo.Article;
import lombok.Data;

import java.util.List;

//用户主页信息VO（包含用户资料、文章列表、关注/粉丝数）
@Data
public class UserProfileVO {
    private Integer id;
    private String username;
    private String nickname;
    private String userPic;
    private String email;
    private Long articleCount;//文章总数
    private Integer followCount;//关注数
    private Integer fansCount;//粉丝数
    private Boolean isFollowed;//当前登录用户是否已关注
    private List<Article> articles;//文章列表
    private Integer total;//文章总数（分页用）
    private Integer pageNum;
    private Integer pageSize;
}
