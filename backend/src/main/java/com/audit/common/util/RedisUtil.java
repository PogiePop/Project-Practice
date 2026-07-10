package com.audit.common.util;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis字符串操作工具类
 * <p>封装常用String操作：set / setEx / get / del</p>
 */
@Component
public class RedisUtil {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisUtil(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 写入字符串（无过期时间） */
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /** 写入字符串并设置过期时间（秒） */
    public void setEx(String key, String value, long seconds) {
        redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
    }

    /** 读取字符串 */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /** 删除Key */
    public Boolean del(String key) {
        return redisTemplate.delete(key);
    }
}
