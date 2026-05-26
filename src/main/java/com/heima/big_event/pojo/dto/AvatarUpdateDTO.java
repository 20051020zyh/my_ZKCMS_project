package com.heima.big_event.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;


//头像地址的参数校验
@Data
public class AvatarUpdateDTO {
    @NotBlank(message = "头像URL地址不能为空")
    @Pattern(
            regexp = "^(https?|ftp)://.*$",
            message = "头像URL格式不正确,必须是有效的hhtp/hhtps/ftp地址"
    )
    private String avatarUrl;
}
