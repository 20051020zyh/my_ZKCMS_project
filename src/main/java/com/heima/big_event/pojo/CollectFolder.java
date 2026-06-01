package com.heima.big_event.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

//收藏分类文件夹实体类
@Data
@TableName("collect_folder")
public class CollectFolder {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;//所属用户ID
    private String name;//文件夹名称
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;//创建时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;//更新时间
}
