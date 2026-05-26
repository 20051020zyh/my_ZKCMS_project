package com.heima.big_event.pojo;


import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import com.heima.big_event.anno.State;
import java.time.LocalDateTime;

/**
 * (SysRole)表实体类
 *
 * @author makejava
 * @since 2026-05-12 17:14:42
 */
@Data
@TableName("sys_role") // 匹配数据库表名，比如article
public class SysRole {
    @TableId(type = IdType.AUTO)
        private Long id;
    @NotBlank(message = "角色名不能为空")
        private String roleName;//角色名
        private String roleCode;//角色标识 ADMIN/USER
        private String remark; //备注
    }
