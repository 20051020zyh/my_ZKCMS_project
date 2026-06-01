package com.heima.big_event.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.big_event.pojo.UserFollow;
import org.apache.ibatis.annotations.Mapper;

//用户关注关系表Mapper
@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {
}
