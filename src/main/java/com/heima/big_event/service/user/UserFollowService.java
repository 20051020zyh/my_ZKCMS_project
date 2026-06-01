package com.heima.big_event.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.big_event.pojo.UserFollow;
import com.heima.big_event.pojo.VO.FollowVO;

import java.util.List;
import java.util.Map;

public interface UserFollowService extends IService<UserFollow> {
    //关注/取关指定用户，同步更新关注数和粉丝数，并清除Redis缓存
    FollowVO toggleFollow(Integer followedUserId, Integer userId);
    //检查当前用户是否已关注指定用户
    boolean checkFollow(Integer followedUserId, Integer userId);
    //获取当前用户的关注列表（分页）
    List<Map<String, Object>> getFollowList(Integer userId, Integer pageNum, Integer pageSize);
    //获取当前用户的粉丝列表（分页）
    List<Map<String, Object>> getFansList(Integer userId, Integer pageNum, Integer pageSize);
}
