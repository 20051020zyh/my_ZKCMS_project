package com.heima.big_event.service.impl.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.big_event.mapper.system.SysRolePermissionMapper;
import com.heima.big_event.mapper.system.SysUserPermissionMapper;
import com.heima.big_event.mapper.system.SysUserRoleMapper;
import com.heima.big_event.pojo.SysUserPermission;
import com.heima.big_event.service.system.SysUserPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * (SysUserPermission)表服务实现类
 *
 * @author makejava
 * @since 2026-05-23
 */
@Service
public class SysUserPermissionServiceImpl extends ServiceImpl<SysUserPermissionMapper, SysUserPermission> implements SysUserPermissionService {

    @Autowired
    private SysUserPermissionMapper sysUserPermissionMapper;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissionToUser(Long userId, List<Long> permissionIds) {
        LambdaQueryWrapper<SysUserPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserPermission::getUserId, userId);
        this.remove(wrapper);

        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<SysUserPermission> list = permissionIds.stream().map(pid -> {
                SysUserPermission up = new SysUserPermission();
                up.setUserId(userId);
                up.setPermissionId(pid);
                return up;
            }).collect(Collectors.toList());
            this.saveBatch(list);
        }
    }

    @Override
    public List<Long> getPermissionIdsByUserId(Long userId) {
        LambdaQueryWrapper<SysUserPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserPermission::getUserId, userId);
        List<SysUserPermission> list = this.list(wrapper);
        return list.stream().map(SysUserPermission::getPermissionId).collect(Collectors.toList());
    }

    @Override
    public List<Long> getAllPermissionIdsByUserId(Long userId) {
        Set<Long> allPermIds = new HashSet<>();

        // 1. 获取用户通过角色获得的权限ID
        List<Long> roleIds = sysUserRoleMapper.selectRoleIdsByUserId(userId);
        if (!roleIds.isEmpty()) {
            List<Long> rolePermIds = sysRolePermissionMapper.selectPermissionIdsByRoleIds(roleIds);
            if (rolePermIds != null) {
                allPermIds.addAll(rolePermIds);
            }
        }

        // 2. 获取用户直接分配的权限ID
        List<Long> directPermIds = getPermissionIdsByUserId(userId);
        allPermIds.addAll(directPermIds);

        return allPermIds.stream().collect(Collectors.toList());
    }
}
