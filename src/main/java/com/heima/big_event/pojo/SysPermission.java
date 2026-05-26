package com.heima.big_event.pojo;


import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import com.heima.big_event.anno.State;
import java.time.LocalDateTime;
import java.util.List;

/**
 * (SysPermission)表实体类
 *
 * @author makejava
 * @since 2026-05-12 17:14:42
 */
@Data
@TableName("sys_permission")
public class SysPermission {
        @TableId(type = IdType.AUTO)//主键自增
        private Long id;
        @NotBlank(message = "权限名称不能为空")
        private String name;//权限名称
        @NotBlank(message = "权限标识不能为空")
        private String permission;//权限标识 sys:user:list
        @NotBlank(message = "菜单路径不能为空")
        private String path;//菜单路由
        private Long parentId;//父菜单ID
        private Integer type;//1菜单 2按钮
        @TableField(exist = false)
        private List<SysPermission> children;//用于封装树形结构的子菜单
    }
