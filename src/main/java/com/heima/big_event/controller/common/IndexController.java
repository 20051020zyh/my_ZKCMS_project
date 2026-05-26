package com.heima.big_event.controller.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {
    //访问项目根路径,自动跳转到登录页面
    @GetMapping("/")
    public String toLogin(){
        //直接指向 static 下面的login.html
        return "forward:/login.html";
    }
}
