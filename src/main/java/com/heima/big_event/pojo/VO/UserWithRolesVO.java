package com.heima.big_event.pojo.VO;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserWithRolesVO {
    private Integer id;
    private String username;
    private String nickname;
    private String email;
    private String userPic;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer status;
    private List<String> roles;
}
