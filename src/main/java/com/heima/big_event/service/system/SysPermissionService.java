package com.heima.big_event.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.big_event.pojo.SysPermission;

import java.util.List;


/**
 * (SysPermission)表服务接口
 *
 * @author makejava
 * @since 2026-05-12 17:02:39
 */
public interface SysPermissionService extends IService<SysPermission> {

    //查询树形结构的权限菜单
    List<SysPermission> listTree();

    //删除权限(级联删除)
    boolean removePermission(Long id);
}

