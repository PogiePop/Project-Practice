package com.audit.mapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface SettingsMapper {
    List<Map<String, String>> findByUserId(@Param("userId") String userId);
    int save(@Param("userId") String userId, @Param("key") String key, @Param("value") String value);
}
