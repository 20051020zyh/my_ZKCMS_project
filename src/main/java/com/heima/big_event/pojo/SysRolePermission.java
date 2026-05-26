package com.heima.big_event.pojo;


import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import com.heima.big_event.anno.State;
import java.time.LocalDateTime;

/**
 * (SysRolePermission)表实体类
 *
 * @author makejava
 * @since 2026-05-12 17:14:42
 */
@Data
@TableName("sys_role_permission") // 匹配数据库表名，比如article
public class SysRolePermission {
        private Long id;
        private Long roleId;
        private Long permissionId;
    }
