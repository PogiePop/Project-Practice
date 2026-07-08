package com.audit.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface SettingsMapper {

    @Select("SELECT setting_key, setting_value FROM sys_settings WHERE user_id = #{userId}")
    List<Map<String, String>> findByUserId(@Param("userId") String userId);

    @Insert("INSERT INTO sys_settings (user_id, setting_key, setting_value) VALUES (#{userId}, #{key}, #{value}) " +
            "ON DUPLICATE KEY UPDATE setting_value = #{value}, update_time = NOW()")
    int save(@Param("userId") String userId, @Param("key") String key, @Param("value") String value);
}
