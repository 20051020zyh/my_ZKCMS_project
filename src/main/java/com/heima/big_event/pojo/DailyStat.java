package com.heima.big_event.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
public class DailyStat implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private Integer pv;
    private Integer uv;
    private Integer ipCount;
    private Long publishCount;
    private Integer userCount;
}
