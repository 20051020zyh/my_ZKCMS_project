package com.heima.big_event.service.impl.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.big_event.mapper.system.SysPermissionMapper;
import com.heima.big_event.mapper.system.SysRoleMapper;
import com.heima.big_event.mapper.system.SysRolePermissionMapper;
import com.heima.big_event.mapper.system.SysUserRoleMapper;
import com.heima.big_event.pojo.SysRole;
import com.heima.big_event.pojo.SysUserRole;
import com.heima.big_event.pojo.VO.UserRoleAssignVO;
import com.heima.big_event.service.system.SysUserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * (SysUserRole)表服务实现类
 *
 * @author makejava
 * @since 2026-05-12 17:02:39
 */
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements SysUserRoleService {

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;
    @Autowired
    private SysRolePermissionMapper sysRolePermissionMapper;
    @Autowired
    private SysPermissionMapper sysPermissionMapper;
    @Autowired
    private SysRoleMapper sysRoleMapper;


    //用户分配角色
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void UserAssignRole(UserRoleAssignVO userRoleAssignVO){
        //获取要被分配角色的用户id
        Long userId = userRoleAssignVO.getUserId();
        //获取要分配的角色id列表
        List<Long> roleIds = userRoleAssignVO.getRoleIds();

        //先删除该用户原有的角色
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId , userId);
        boolean remove = this.remove(wrapper);

        //如果角色列表是空的,也就是不赋予新角色,清空原有的角色
        if (roleIds.isEmpty() || roleIds == null){
            return;
        }

        //批量插入新角色
        List<SysUserRole> userRoleList = new ArrayList<>();
        for (Long role : roleIds){
            SysUserRole sysUserRole = new SysUserRole();
            sysUserRole.setUserId(userId);
            sysUserRole.setRoleId(role);
            userRoleList.add(sysUserRole);
        }

        //MP的批量插入方法
        this.saveBatch(userRoleList);
    }



    //获取用户当前的角色ID列表
    @Override
    public List<Long> getRoleIdsByUserId(Long userId) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId);
        List<SysUserRole> list = this.list(wrapper);
        return list.stream().map(SysUserRole::getRoleId).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取用户拥有的所有权限标识（去重）
     */
    @Override
    public Set<String> getUserPermissions(Long userId) {
        System.out.println("=== getUserPermissions 被调用，userId: " + userId + " ===");
        
        // 1. 查询用户角色ID列表
        List<Long> roleIds = sysUserRoleMapper.selectRoleIdsByUserId(userId);
        System.out.println("用户角色ID列表: " + roleIds);
        
        if (roleIds.isEmpty()) {
            System.out.println("用户没有分配任何角色");
            return Collections.emptySet();
        }

        // 2. 检查是否是超级管理员（roleCode为ADMIN）
        LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.in(SysRole::getId, roleIds);
        List<SysRole> roles = sysRoleMapper.selectList(roleWrapper);
        
        System.out.println("查询到的角色: ");
        for (SysRole role : roles) {
            System.out.println("  - id: " + role.getId() + ", name: " + role.getRoleName() + ", code: " + role.getRoleCode());
        }

        // 检查是否是超级管理员（roleCode 为 ADMIN 或 super_admin 均视为超级管理员）
        boolean isAdmin = roles.stream()
                .anyMatch(role -> "ADMIN".equals(role.getRoleCode()) || "super_admin".equals(role.getRoleCode()));
        
        System.out.println("是否是超级管理员: " + isAdmin);

        if (isAdmin) {
            // 超级管理员拥有所有权限
            Set<String> adminPermissions = new HashSet<>();
            adminPermissions.add("*:*:*");
            System.out.println("返回超级管理员权限: " + adminPermissions);
            return adminPermissions;
        }

        // 3. 查询角色对应的权限ID列表
        List<Long> permIds = sysRolePermissionMapper.selectPermissionIdsByRoleIds(roleIds);
        System.out.println("权限ID列表: " + permIds);
        
        if (permIds.isEmpty()) {
            System.out.println("角色没有分配任何权限");
            return Collections.emptySet();
        }

        // 4. 查询权限标识
        List<String> perms = sysPermissionMapper.selectPermissionsByIds(permIds);
        System.out.println("权限标识列表: " + perms);

        // 转为 Set 去重
        return new HashSet<>(perms);
    }

}

