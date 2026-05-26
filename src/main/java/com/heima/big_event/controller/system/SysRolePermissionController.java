package com.heima.big_event.controller.system;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heima.big_event.pojo.SysRolePermission;
import com.heima.big_event.pojo.Result;
import com.heima.big_event.service.system.SysRolePermissionService;
import com.heima.big_event.utils.Permission.RequirePermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * (SysRolePermission)表控制层
 *
 * @author makejava
 * @since 2026-05-12 17:10:18
 */
@Slf4j
@RestController
@Validated
@RequestMapping("/sysRolePermission")
public class SysRolePermissionController {
    /**
     * 服务对象（Autowired注入，和你的示例一致）
     */
    @Autowired
    private SysRolePermissionService sysRolePermissionService;

    /**
     * 新增数据
     * @param entity 实体对象
     * @return 新增结果
     */
    @PostMapping("/add")
    @RequirePermission("/sysRolePermission/add")
    public Result add(@RequestBody SysRolePermission entity) {
        boolean save = sysRolePermissionService.save(entity);
        return save ? Result.success() : Result.error("新增失败");
    }

    // 批量分配权限：先删除该角色所有权限关联，再批量新增
    @PostMapping("/batchAssign")
    @RequirePermission("/sysRolePermission/add")
    @Transactional
    public Result batchAssign(@RequestBody Map<String, Object> params) {
        Long roleId = Long.valueOf(params.get("roleId").toString());
        @SuppressWarnings("unchecked")
        List<Integer> permissionIds = (List<Integer>) params.get("permissionIds");

        if (roleId == null || permissionIds == null) {
            return Result.error("参数不完整");
        }

        // 删除该角色所有现有的权限关联
        LambdaQueryWrapper<SysRolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRolePermission::getRoleId, roleId);
        sysRolePermissionService.remove(wrapper);

        // 批量新增
        List<SysRolePermission> list = permissionIds.stream().map(pid -> {
            SysRolePermission rp = new SysRolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(pid.longValue());
            return rp;
        }).collect(Collectors.toList());

        sysRolePermissionService.saveBatch(list);
        return Result.success("权限分配成功");
    }


    /**
     * 根据ID查询单条数据
     * @param id 主键ID
     * @return 查询结果
     */
    @GetMapping("/select")
    @RequirePermission("/sysRolePermission/select")
    public Result getById(@PathVariable Integer id) {
        SysRolePermission entity = sysRolePermissionService.getById(id);
        return Result.success(entity);
    }

    /**
     * 修改数据
     * @param entity 实体对象
     * @return 修改结果
     */
    @PutMapping("/update")
    @RequirePermission("/sysRolePermission/update")
    public Result update(@RequestBody SysRolePermission entity) {
        boolean update = sysRolePermissionService.updateById(entity);
        return update ? Result.success() : Result.error("修改失败");
    }

    /**
     * 根据ID删除数据
     * @param id 主键ID
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    @RequirePermission("/sysRolePermission/delete")
    public Result delete(@PathVariable Integer id) {
        boolean remove = sysRolePermissionService.removeById(id);
        return remove ? Result.success() : Result.error("删除失败");
    }

    /**
     * 查询所有数据
     * @return 所有数据
     */
    @GetMapping("list")
    @RequirePermission("/sysRolePermission/list")
    public Result list() {
        return Result.success(sysRolePermissionService.list());
    }
}
