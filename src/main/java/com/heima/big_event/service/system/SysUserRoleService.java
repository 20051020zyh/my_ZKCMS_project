package com.heima.big_event.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.big_event.pojo.SysUserRole;
import com.heima.big_event.pojo.VO.UserRoleAssignVO;

import java.util.List;
import java.util.Set;

/**
 * (SysUserRole)表服务接口
 *
 * @author makejava
 * @since 2026-05-12 17:02:39
 */
public interface SysUserRoleService extends IService<SysUserRole> {

    //用户分配角色
    void UserAssignRole(UserRoleAssignVO userRoleAssignVO);

    //获取用户当前的角色ID列表
    List<Long> getRoleIdsByUserId(Long userId);

    Set<String> getUserPermissions(Long userId);
}

