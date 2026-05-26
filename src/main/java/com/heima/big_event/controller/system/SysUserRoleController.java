package com.heima.big_event.controller.system;



import com.heima.big_event.pojo.Result;
import com.heima.big_event.pojo.VO.UserRoleAssignVO;
import com.heima.big_event.service.system.SysUserRoleService;
import com.heima.big_event.utils.Permission.RequirePermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * (SysUserRole)表控制层
 *
 * @author makejava
 * @since 2026-05-12 17:10:18
 */
@Slf4j
@RestController
@Validated
@RequestMapping("/sysUserRole")
public class SysUserRoleController {
    @Autowired
    private SysUserRoleService sysUserRoleService;

    //为用户批量分配角色
    @PostMapping("/batchAssign")
    @RequirePermission("/sysUserRole/batchAssign")
    public Result batchAssign(@RequestBody UserRoleAssignVO userRoleAssignVO) {
        if (userRoleAssignVO.getUserId() == null) {
            return Result.error("用户ID不能为空");
        }
        sysUserRoleService.UserAssignRole(userRoleAssignVO);
        return Result.success("角色分配成功");
    }

    //获取用户当前的角色ID列表
    @GetMapping("/getRoleIds/{userId}")
    @RequirePermission("/sysUserRole/getRoleIds")
    public Result getRoleIds(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            return Result.error("用户ID不合法");
        }
        List<Long> roleIds = sysUserRoleService.getRoleIdsByUserId(userId);
        return Result.success(roleIds);
    }
}
