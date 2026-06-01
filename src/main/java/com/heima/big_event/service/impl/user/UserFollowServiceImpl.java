package com.heima.big_event.service.impl.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.big_event.exception.BusinessException;
import com.heima.big_event.mapper.user.UserFollowMapper;
import com.heima.big_event.mapper.user.UserMapper;
import com.heima.big_event.pojo.User;
import com.heima.big_event.pojo.UserFollow;
import com.heima.big_event.pojo.VO.FollowVO;
import com.heima.big_event.service.user.UserFollowService;
import com.heima.big_event.utils.Others.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserFollowServiceImpl extends ServiceImpl<UserFollowMapper, UserFollow> implements UserFollowService {

    @Autowired
    private UserFollowMapper userFollowMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisUtil redisUtil;

    //关注/取关操作：已关注则取关、未关注则关注，同步更新双方用户的关注数和粉丝数，清除Redis缓存
    @Transactional
    @Override
    public FollowVO toggleFollow(Integer followedUserId, Integer userId) {
        if (userId.equals(followedUserId)) {
            throw new BusinessException("不能关注自己");
        }

        User followedUser = userMapper.selectById(followedUserId);
        if (followedUser == null) {
            throw new BusinessException("用户不存在");
        }

        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getUserId, userId)
                .eq(UserFollow::getFollowedUserId, followedUserId);

        UserFollow existFollow = userFollowMapper.selectOne(wrapper);

        boolean isFollowed;

        if (existFollow != null) {
            userFollowMapper.deleteById(existFollow.getId());
            LambdaUpdateWrapper<User> uw = new LambdaUpdateWrapper<>();
            uw.eq(User::getId, userId).setSql("follow_count = follow_count - 1");
            userMapper.update(null, uw);
            LambdaUpdateWrapper<User> uw2 = new LambdaUpdateWrapper<>();
            uw2.eq(User::getId, followedUserId).setSql("fans_count = fans_count - 1");
            userMapper.update(null, uw2);
            isFollowed = false;
        } else {
            UserFollow newFollow = new UserFollow();
            newFollow.setUserId(userId);
            newFollow.setFollowedUserId(followedUserId);
            userFollowMapper.insert(newFollow);
            LambdaUpdateWrapper<User> uw = new LambdaUpdateWrapper<>();
            uw.eq(User::getId, userId).setSql("follow_count = follow_count + 1");
            userMapper.update(null, uw);
            LambdaUpdateWrapper<User> uw2 = new LambdaUpdateWrapper<>();
            uw2.eq(User::getId, followedUserId).setSql("fans_count = fans_count + 1");
            userMapper.update(null, uw2);
            isFollowed = true;
        }

        redisUtil.delete("user:follow:" + userId);
        redisUtil.delete("user:fans:" + followedUserId);
        redisUtil.delete("user:profile:" + followedUserId);

        User current = userMapper.selectById(userId);
        User other = userMapper.selectById(followedUserId);

        FollowVO vo = new FollowVO();
        vo.setIsFollowed(isFollowed);
        vo.setFollowCount(current.getFollowCount());
        vo.setFansCount(other.getFansCount());
        return vo;
    }

    //检查当前用户是否已关注指定用户
    @Override
    public boolean checkFollow(Integer followedUserId, Integer userId) {
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getUserId, userId)
                .eq(UserFollow::getFollowedUserId, followedUserId);
        return userFollowMapper.selectOne(wrapper) != null;
    }

    //获取当前用户的关注列表，按关注时间倒序排列（分页）
    @Override
    public List<Map<String, Object>> getFollowList(Integer userId, Integer pageNum, Integer pageSize) {
        Page<UserFollow> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getUserId, userId).orderByDesc(UserFollow::getCreateTime);
        Page<UserFollow> result = userFollowMapper.selectPage(page, wrapper);

        List<Integer> userIds = result.getRecords().stream()
                .map(UserFollow::getFollowedUserId).collect(Collectors.toList());

        if (userIds.isEmpty()) return Collections.emptyList();

        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Integer, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        return result.getRecords().stream().map(f -> {
            User u = userMap.get(f.getFollowedUserId());
            Map<String, Object> m = new HashMap<>();
            m.put("id", f.getFollowedUserId());
            m.put("username", u != null ? u.getUsername() : null);
            m.put("nickname", u != null ? u.getNickname() : null);
            m.put("userPic", u != null ? u.getUserPic() : null);
            m.put("followTime", f.getCreateTime());
            return m;
        }).collect(Collectors.toList());
    }

    //获取当前用户的粉丝列表，按关注时间倒序排列（分页）
    @Override
    public List<Map<String, Object>> getFansList(Integer userId, Integer pageNum, Integer pageSize) {
        Page<UserFollow> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFollowedUserId, userId).orderByDesc(UserFollow::getCreateTime);
        Page<UserFollow> result = userFollowMapper.selectPage(page, wrapper);

        List<Integer> userIds = result.getRecords().stream()
                .map(UserFollow::getUserId).collect(Collectors.toList());

        if (userIds.isEmpty()) return Collections.emptyList();

        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Integer, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        return result.getRecords().stream().map(f -> {
            User u = userMap.get(f.getUserId());
            Map<String, Object> m = new HashMap<>();
            m.put("id", f.getUserId());
            m.put("username", u != null ? u.getUsername() : null);
            m.put("nickname", u != null ? u.getNickname() : null);
            m.put("userPic", u != null ? u.getUserPic() : null);
            m.put("followTime", f.getCreateTime());
            return m;
        }).collect(Collectors.toList());
    }
}
