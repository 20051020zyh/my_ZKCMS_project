package com.heima.big_event.pojo.VO;

import lombok.Data;

//关注/取关操作返回VO（包含操作后的关注状态、关注数和粉丝数）
@Data
public class FollowVO {
    private Boolean isFollowed;//当前是否已关注
    private Integer followCount;//当前用户关注数
    private Integer fansCount;//对方粉丝数
}
