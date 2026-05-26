package com.heima.big_event.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heima.big_event.pojo.SysPermission;
import com.heima.big_event.pojo.Result;
import com.heima.big_event.service.system.SysPermissionService;
import com.heima.big_event.utils.Permission.RequirePermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@Validated
@RequestMapping("/sysPermission")
public class SysPermissionController {
    @Autowired
    private SysPermissionService sysPermissionService;

    //新增权限菜单
    @PostMapping("/add")
    @RequirePermission("/sysPermission/add")
    public Result add(@RequestBody SysPermission sysPermission) {
        boolean save = sysPermissionService.save(sysPermission);
        return save ? Result.success() : Result.error("新增失败");
    }

    //修改菜单
    @PutMapping("/update")
    @RequirePermission("/sysPermission/update")
    public Result update(@RequestBody SysPermission sysPermission) {
        try {
            // 步骤1：校验ID是否为空
            if (sysPermission.getId() == null) {
                log.warn("修改权限失败：ID为空，请求体：{}", sysPermission);
                return Result.error("菜单ID不能为空");
            }
            // 步骤2：校验ID是否存在
            LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysPermission::getId , sysPermission.getId());
            boolean exists = sysPermissionService.exists(wrapper);
            if (!exists) {
                log.warn("修改权限失败：ID{}不存在，请求体：{}", sysPermission.getId(), sysPermission);
                return Result.error("菜单ID不存在，修改失败");
            }
            // 步骤3：校验其他参数
            if (!StringUtils.hasText(sysPermission.getName())
                    || !StringUtils.hasText(sysPermission.getPermission())
                    || !StringUtils.hasText(sysPermission.getPath())) {
                log.warn("修改权限失败：参数为空，ID：{}，请求体：{}", sysPermission.getId(), sysPermission);
                return Result.error("菜单名称、标识、路径不能为空");
            }
            // 步骤4：执行修改
            boolean update = sysPermissionService.updateById(sysPermission);
            if (update) {
                log.info("修改权限成功：ID{}，内容：{}", sysPermission.getId(), sysPermission);
                return Result.success("修改成功");
            } else {
                log.error("修改权限失败：ID{}数据库操作失败，请求体：{}", sysPermission.getId(), sysPermission);
                return Result.error("修改失败（数据库操作异常）");
            }
        } catch (Exception e) {
            log.error("修改权限异常：ID{}，", sysPermission.getId(), e);
            return Result.error("修改失败：" + e.getMessage());
        }
    }

    //删除菜单
    @DeleteMapping("/delete")
    @RequirePermission("/sysPermission/delete")
    public Result delete(@RequestParam Long id) {
        try {
            if (id == null){
                log.warn("删除权限失败:ID为空");
                return Result.error("菜单ID不能为空");
            }
            //校验id是否存在
            LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysPermission::getId , id);
            boolean exists = sysPermissionService.exists(wrapper);
            if (!exists){
                log.warn("删除权限失败:ID{}不存在" , id);
                return Result.error("菜单ID不存在,删除失败");
            }
            //执行删除
            boolean remove = sysPermissionService.removePermission(id);
            if (remove){
                log.info("删除权限成功:ID{}" , id);
                return Result.success("删除成功");
            }else {
                log.error("删除权限失败:ID{}数据库操作失败" , id);
                return Result.error("删除失败(数据库操作异常)");
            }
        } catch (Exception e){
            log.error("删除权限异常: ID{}" , id , e);
            return Result.error("删除失败:" + e.getMessage());
        }
    }


    //查询单个权限菜单
    @GetMapping("/selectOne")
    @RequirePermission("/sysPermission/selectOne")
    public Result getById(@RequestParam Long id) {
        if (id == null){
            return Result.error("菜单ID为空");
        }
        SysPermission sysPermission = sysPermissionService.getById(id);
        return Result.success(sysPermission);
    }

    //查询菜单树(给角色分配权限时用)
    @GetMapping("/tree")
    @RequirePermission("/sysPermission/tree")
    public Result listTree() {
        return Result.success(sysPermissionService.listTree());
    }

    //查询所有的菜单权限(平级列表,方便前端下拉选择)
    @GetMapping("/list")
    @RequirePermission("/sysPermission/list")
    public Result listAll(){
        return Result.success(sysPermissionService.list());
    }
}
