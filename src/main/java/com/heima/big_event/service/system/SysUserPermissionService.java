package com.heima.big_event.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.big_event.pojo.SysUserPermission;

import java.util.List;

/**
 * (SysUserPermission)表服务接口
 *
 * @author makejava
 * @since 2026-05-23
 */
public interface SysUserPermissionService extends IService<SysUserPermission> {

    //给用户直接分配权限
    void assignPermissionToUser(Long userId, List<Long> permissionIds);

    //获取用户已直接分配的权限ID列表
    List<Long> getPermissionIdsByUserId(Long userId);

    //获取用户全部权限ID（角色权限+直接分配，去重合并）
    List<Long> getAllPermissionIdsByUserId(Long userId);
}
