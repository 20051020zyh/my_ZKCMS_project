package com.heima.big_event.utils.Others;

import java.util.HashMap;
import java.util.Map;

//ThreadLocal 工具类，用于在同一线程中存储和获取用户信息
public class ThreadLocalUtil {
    private static final ThreadLocal<Map<String, Object>> threadLocal = ThreadLocal.withInitial(HashMap::new);

    //存储键值对
    public static void set(String key, Object value) {
        threadLocal.get().put(key, value);
    }

    //根据键获取值
    public static Object get(String key) {
        return threadLocal.get().get(key);
    }

    //取用户id
    public static Integer getUserId(){
        return (Integer) get("userId");
    }

    //获取用户的名字
    public static String getUserName(){
        return (String) get("username");
    }

    //清除当前线程的所有数据
    public static void clear() {
        threadLocal.get().clear();
        threadLocal.remove();
    }

}
