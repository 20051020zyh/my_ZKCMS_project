package com.heima.big_event.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.big_event.pojo.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    //判断用户名是否被占用
//    @Select("select * from user where username=#{username}")
//    User findByUserName(String username);


    //添加 ,注册
//    @Insert("insert into user(username , password ,create_time ,update_time)" +
//            "values (#{username},#{password},now(),now())")
//    void add(@Param("username") String username, @Param("password") String password);

    void update(User user);
}
