package com.heima.big_event.pojo;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * (SysUserPermission)表实体类
 *
 * @author makejava
 * @since 2026-05-23
 */
@Data
@TableName("sys_user_permission")
public class SysUserPermission {
        @TableId(type = IdType.AUTO)
        private Long id;
        private Long userId;
        private Long permissionId;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createTime;
    }
