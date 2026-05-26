package com.heima.big_event.pojo;


import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import com.heima.big_event.anno.State;
import java.time.LocalDateTime;

/**
 * 系统全局配置表(SysConfig)表实体类
 *
 * @author makejava
 * @since 2026-05-14 16:09:00
 */
@Data
@TableName("sys_config") // 匹配数据库表名，比如article
public class SysConfig {
        private Long id;//id
        private String configKey;//配置键
        private String configValue;//配置值
        private String remark;//备注:全站维护模式开关,1是开启,0是关闭
    
    
    }
