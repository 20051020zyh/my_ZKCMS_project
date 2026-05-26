package com.heima.big_event.pojo.dto;  // 根据你的实际包路径调整

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;


//参数校验
@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^\\S{2,16}$", message = "用户名必须为2~16位非空白字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^\\S{5,16}$", message = "密码必须为5~16位非空白字符")
    private String password;

}
