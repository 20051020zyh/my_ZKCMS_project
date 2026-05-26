package com.heima.big_event.pojo;



import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)//告诉MP这是一个自增主键
    private Integer id;//主键ID
    private String username;//用户名
    @JsonIgnore//让springMVC把当前对象转换成json字符串的时候,忽略password,
    //最终的json字符串就没有password这个属性了
    private String password;//密码
    @NotBlank(message = "昵称不能为空")
    @Pattern(regexp = "^\\S{1,10}$",message = "昵称必须为1~10位非白字符")
    private String nickname;//昵称
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    //@Email邮箱的专门注释
    private String email;//邮箱
    private String userPic;//用户头像地址
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;//创建时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;//更新时间
    private Integer status;//用户状态
}
