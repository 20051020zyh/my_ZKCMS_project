package com.heima.big_event.utils.Others;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 存入数据（永久）
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    // 存入数据（带过期时间，单位秒）
    public void set(String key, Object value, long timeout) {
        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }

    // 获取数据
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // 删除数据(只支持key)
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    //更新缓存删除(支持通配符删除)
    public void deleteByPattern(String pattern){
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()){
            redisTemplate.delete(keys);
        }

    }

    // 设置过期时间
    public Boolean expire(String key, long timeout) {
        return redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }




    //Hash
    public void hput(String key , String hashKey , Object value){
        redisTemplate.opsForHash().put(key , hashKey , value);
    }
    // 往 Hash 里存一个字段（带过期时间）
    public void hput(String key, String hashKey, Object value, long timeout) {
        redisTemplate.opsForHash().put(key, hashKey, value);
        expire(key, timeout);
    }

    // 从 Hash 里获取一个字段
    public Object hget(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    // 删除 Hash 里的某个字段
    public void hdelete(String key, String... hashKeys) {
        redisTemplate.opsForHash().delete(key, hashKeys);
    }

    // ======================== 分布式锁=========================
    // 加锁
    public boolean lock(String key, long timeout) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "locked", timeout, TimeUnit.SECONDS));
    }

    // 解锁
    public void unlock(String key) {
        redisTemplate.delete(key);
    }
}
