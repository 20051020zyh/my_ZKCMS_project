package com.heima.big_event.service.impl.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.big_event.mapper.system.SysPermissionMapper;
import com.heima.big_event.pojo.SysPermission;
import com.heima.big_event.service.system.SysPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * (SysPermission)表服务实现类
 *
 * @author makejava
 * @since 2026-05-12 17:02:39
 */
@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission> implements SysPermissionService {
    //查询树形结构的权限菜单
    @Override
    public List<SysPermission> listTree(){
        //查询所有权限菜单
        List<SysPermission> allpermissions = this.list();

        //筛选根节点(父级id = 0)
        List<SysPermission> rootNods = allpermissions.stream()
                .filter(permission -> permission.getParentId() == 0)
                .collect(Collectors.toList());

        //递归给每个根节点设置子菜单
        for (SysPermission root : rootNods){
            root.setChildren(getChildren(root.getId() , allpermissions));
        }
        return rootNods;
    }


    //递归获取子菜单
    private List<SysPermission> getChildren(Long parentId , List<SysPermission> allpermission){
        //筛选当前父节点的子节点
        List<SysPermission> children = allpermission.stream()
                .filter(permission -> parentId.equals(permission.getParentId()))
                .collect(Collectors.toList());

        //递归:给子节点再找子节点(二级菜单找三级按钮)
        for (SysPermission child : children){
            child.setChildren(getChildren(child.getId() , allpermission));
        }

        //如果没有子节点,返回空列表
        return CollectionUtils.isEmpty(children) ? null : children;
    }


    //删除权限(级联删除)
    @Override
    public boolean removePermission(Long id){
        //先查询当前权限的所有子权限ID
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getParentId , id);
        List<SysPermission> sysPermissionList = this.list(wrapper);

        //递归删除子权限
        if (!CollectionUtils.isEmpty(sysPermissionList)){
            for (SysPermission child : sysPermissionList){
                removePermission(child.getId());
            }
        }

        //最后删除自己
        return this.removeById(id);
    }

}

