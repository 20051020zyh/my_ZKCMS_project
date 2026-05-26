package com.heima.big_event.controller.system;

import com.heima.big_event.pojo.Result;
import com.heima.big_event.service.system.SysUserPermissionService;
import com.heima.big_event.utils.Permission.RequirePermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@Validated
@RequestMapping("/sysUserPermission")
public class SysUserPermissionController {
    @Autowired
    private SysUserPermissionService sysUserPermissionService;

    @PostMapping("/batchAssign")
    @RequirePermission("/sysUserPermission/batchAssign")
    public Result batchAssign(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        @SuppressWarnings("unchecked")
        List<Integer> permissionIds = (List<Integer>) params.get("permissionIds");
        if (userId == null || permissionIds == null) {
            return Result.error("参数不完整");
        }
        sysUserPermissionService.assignPermissionToUser(userId,
                permissionIds.stream().map(Long::valueOf).collect(java.util.stream.Collectors.toList()));
        return Result.success("权限分配成功");
    }

    @GetMapping("/getPermissionIds/{userId}")
    @RequirePermission("/sysUserPermission/getPermissionIds")
    public Result getPermissionIds(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            return Result.error("用户ID不合法");
        }
        List<Long> permissionIds = sysUserPermissionService.getPermissionIdsByUserId(userId);
        return Result.success(permissionIds);
    }

    @GetMapping("/getAllPermissionIds/{userId}")
    @RequirePermission("/sysUserPermission/getAllPermissionIds")
    public Result getAllPermissionIds(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            return Result.error("用户ID不合法");
        }
        List<Long> permissionIds = sysUserPermissionService.getAllPermissionIdsByUserId(userId);
        return Result.success(permissionIds);
    }
}
