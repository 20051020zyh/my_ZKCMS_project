package com.heima.big_event.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@TableName("browser_device_stat")
public class BrowserDeviceStat implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private String browser;
    private String deviceType;
    private String os;
    private Integer count;
}
