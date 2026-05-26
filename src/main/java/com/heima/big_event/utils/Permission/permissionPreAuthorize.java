package com.heima.big_event.utils.Permission;

import com.alibaba.fastjson.JSON;
import com.heima.big_event.utils.Others.RedisUtil;
import com.heima.big_event.utils.Others.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class permissionPreAuthorize {
    @Autowired
    private RedisUtil redisUtil;
    /**
     * 权限校验核心方法
     * 从 Redis 获取当前用户权限，判断是否包含传入的权限标识
     */
    public boolean hasPermission(String permission) {
        Integer userId = ThreadLocalUtil.getUserId();
        if (userId == null){
            return false;
        }

        String key = "login:user:" + userId;
        Object permissionsObj = redisUtil.hget(key, "permissions");
        if (permissionsObj == null){
            return false;
        }
        String permissionsJson = permissionsObj.toString();
        if (permissionsJson.isEmpty()){
            return false;
        }
        //转成集合
        List<String> list = JSON.parseArray(permissionsJson, String.class);
        //判断是否包含该权限
        return list != null && list.contains(permission);
    }
}
