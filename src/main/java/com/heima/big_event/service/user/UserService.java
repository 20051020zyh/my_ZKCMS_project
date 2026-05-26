package com.heima.big_event.service.user;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.big_event.pojo.User;
import com.heima.big_event.pojo.VO.AdminHomeStatsVO;
import com.heima.big_event.pojo.VO.ArticleCenterInfoVO;
import com.heima.big_event.pojo.VO.UserWithRolesVO;

public interface UserService extends IService<User> {
    //根据用户名查询用户
    User findByUserName(String username);

    //注册
    void register(String username, String password);


    //更新用户头像
    void updateAvatar(Integer userId , String avatarUrl);


    //更新用户密码
    void updatePwd(Integer userId, String newPwd);

    //退出登录
    void logout(String authorization);

    //个人中心接口
    ArticleCenterInfoVO UserCenterInfoImpl(Integer userId);

    //后台首页统计接口
    AdminHomeStatsVO AdminHomeStatsImpl();

    //全用户分页管理
    IPage<UserWithRolesVO> getUserPageList(Integer pageNum, Integer pageSize, String keyword);

    //注销用户（级联删除该用户所有相关数据）
    void deleteUserWithAllData(Integer userId);

    //获取用户状态统计（正常/禁用）
    java.util.Map<String, Long> getUserStatusStats();
}
