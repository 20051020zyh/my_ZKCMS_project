package com.heima.big_event.pojo.VO;

import lombok.Data;

import java.util.List;

@Data
public class UserRoleAssignVO {
    private Long userId;
    private List<Long> roleIds;//角色ID列表(如果是空的,那么就是清空原有的角色)
}
