package com.heima.big_event.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

//用户关注关系表实体类
@Data
@TableName("user_follow")
public class UserFollow {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;//关注者ID
    private Integer followedUserId;//被关注者ID
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;//关注时间
}
