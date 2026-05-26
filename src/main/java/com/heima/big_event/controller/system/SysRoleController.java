package com.heima.big_event.controller.system;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.big_event.pojo.SysRole;
import com.heima.big_event.pojo.Result;
import com.heima.big_event.pojo.SysRolePermission;
import com.heima.big_event.pojo.SysUserRole;
import com.heima.big_event.service.system.SysRolePermissionService;
import com.heima.big_event.service.system.SysRoleService;
import com.heima.big_event.service.system.SysUserRoleService;
import com.heima.big_event.utils.Permission.RequirePermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

//sysrole角色
@Slf4j
@RestController
@Validated
@RequestMapping("/sysRole")
public class SysRoleController {
    @Autowired
    private SysRoleService sysRoleService;
    @Autowired
    private SysUserRoleService sysUserRoleService;
    @Autowired
    private SysRolePermissionService sysRolePermissionService;
    //新增角色
    @PostMapping("/add")
    @RequirePermission("/sysRole/add")
    public Result add(@RequestBody SysRole sysRole) {
        if (!StringUtils.hasText(sysRole.getRoleName())|| !StringUtils.hasText(sysRole.getRoleCode())){
            return Result.error("角色名称和编码不能为空");
        }
        //校验角色编码是否重复
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleCode , sysRole.getRoleCode());
        if (sysRoleService.count(wrapper) > 0){
            return Result.error("角色编码已存在,无法新增");
        }
        boolean save = sysRoleService.save(sysRole);
        return save ? Result.success() : Result.error("角色新增失败");
    }

   //根据角色id查询对应的数据
    @GetMapping("/select")
    @RequirePermission("/sysRole/select")
    public Result getById(@RequestParam Long id) {
        if (id == null || id <= 0 ){
            return Result.error("角色ID不合法");
        }
        SysRole sysRole = sysRoleService.getById(id);
        return Result.success(sysRole);

    }

    //更新角色数据
    @PutMapping("/update")
    @RequirePermission("/sysRole/update")
    public Result update(@RequestBody SysRole sysRole) {
        if (sysRole.getId()  == null ||  !StringUtils.hasText(sysRole.getRoleName())  ||
                !StringUtils.hasText(sysRole.getRoleCode())) {
            return Result.error("角色ID,名称,编码不能为空");
        }
        //校验角色编码是否重复(排除自身)
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleCode , sysRole.getRoleCode())
                .ne(SysRole::getId , sysRole.getId());//ne = not equal , 排除自己
        if (sysRoleService.count(wrapper) > 0){
            return Result.error("角色编码已存在,无法更新");
        }
        boolean update = sysRoleService.updateById(sysRole);
        return update ? Result.success() : Result.error("更新角色失败");
    }

    //根据id删除相应的角色(支持批量)
    @Transactional
    @DeleteMapping("/delete")
    @RequirePermission("/sysRole/delete")
    public Result delete(@RequestParam String ids) {
        if (ids == null || ids.trim().isEmpty()){
            return Result.error("请选择要删除的角色");
        }
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(Collectors.toList());
        //删除用户角色关联表的相关数据
        LambdaQueryWrapper<SysUserRole> sysUserRoleLambdaQueryWrapper = new LambdaQueryWrapper<>();
        sysUserRoleLambdaQueryWrapper.in(SysUserRole::getRoleId , idList);
        if (sysUserRoleService.count(sysUserRoleLambdaQueryWrapper) > 0){
            return Result.error("部分角色已被用户关联,无法删除");
        }
        //如果要删除的还要把权限角色表的数据也删除
        LambdaQueryWrapper<SysRolePermission> sysRolePermissionLambdaQueryWrapper = new LambdaQueryWrapper<>();
        sysRolePermissionLambdaQueryWrapper.in(SysRolePermission::getRoleId , idList);
        sysRolePermissionService.remove(sysRolePermissionLambdaQueryWrapper);

        boolean removeByIds = sysRoleService.removeByIds(idList);
        return removeByIds ? Result.success() : Result.error("删除失败");
    }

    //查询所有角色列表(分页+条件)
    @GetMapping("/list")
    @RequirePermission("/sysRole/list")
    public Result<Page<SysRole>> list(
            //分页参数
            @RequestParam(defaultValue = "1") Integer pageNum ,
            @RequestParam(defaultValue = "10") Integer pageSize,
            //条件参数:支持按角色名称/角色编码模糊查询
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleCode
            ) {
        Page<SysRole> page = new Page<>(pageNum , pageSize);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
//        return Result.success(sysRoleService.list());
        //如果传了角色名称的话
        if (StringUtils.hasText(roleName)){
            wrapper.like(SysRole::getRoleName , roleName);
        }
        //如果传了角色编码的话
        if (StringUtils.hasText(roleCode)){
            wrapper.like(SysRole::getRoleCode , roleCode);
        }
        //调用分页查询
        Page<SysRole> sysRolePage = sysRoleService.page(page, wrapper);
        //返回结果
        return Result.success(sysRolePage);
    }
}
