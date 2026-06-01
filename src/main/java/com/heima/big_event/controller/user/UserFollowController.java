package com.heima.big_event.controller.user;

import com.heima.big_event.pojo.Result;
import com.heima.big_event.pojo.VO.FollowVO;
import com.heima.big_event.service.user.UserFollowService;
import com.heima.big_event.utils.Others.ThreadLocalUtil;
import com.heima.big_event.utils.Permission.RequirePermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@Validated
@RequestMapping("/user/follow")
public class UserFollowController {

    @Autowired
    private UserFollowService userFollowService;

    //关注/取关用户接口
    @PostMapping("/toggle")
    @RequirePermission(value = "/user/follow/toggle", checkPermission = false)
    public Result<FollowVO> toggleFollow(Integer followedUserId) {
        Integer userId = ThreadLocalUtil.getUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        FollowVO vo = userFollowService.toggleFollow(followedUserId, userId);
        return Result.success(vo);
    }

    //检查当前登录用户是否已关注该用户
    @GetMapping("/check")
    @RequirePermission(value = "/user/follow/check", checkPermission = false)
    public Result<Boolean> checkFollow(Integer followedUserId) {
        Integer userId = ThreadLocalUtil.getUserId();
        if (userId == null) {
            return Result.success(false);
        }
        boolean isFollowed = userFollowService.checkFollow(followedUserId, userId);
        return Result.success(isFollowed);
    }

    //获取当前用户的关注列表（分页）
    @GetMapping("/list")
    @RequirePermission(value = "/user/follow/list", checkPermission = false)
    public Result<List<Map<String, Object>>> getFollowList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer userId = ThreadLocalUtil.getUserId();
        List<Map<String, Object>> list = userFollowService.getFollowList(userId, pageNum, pageSize);
        return Result.success(list);
    }

    //获取当前用户的粉丝列表（分页）
    @GetMapping("/fans/list")
    @RequirePermission(value = "/user/follow/fans/list", checkPermission = false)
    public Result<List<Map<String, Object>>> getFansList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer userId = ThreadLocalUtil.getUserId();
        List<Map<String, Object>> list = userFollowService.getFansList(userId, pageNum, pageSize);
        return Result.success(list);
    }
}
