package com.audit.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface MessageMapper {

    @Select("<script>" +
            "SELECT * FROM sys_message WHERE 1=1" +
            "<if test='isRead != null'> AND is_read = #{isRead}</if>" +
            " ORDER BY create_time DESC" +
            "</script>")
    List<Map<String, Object>> findList(@Param("isRead") Integer isRead);

    @Update("UPDATE sys_message SET is_read = 1 WHERE message_id = #{messageId}")
    int markRead(@Param("messageId") String messageId);

    @Update("UPDATE sys_message SET is_read = 1 WHERE is_read = 0")
    int markAllRead();

    @Select("SELECT COUNT(*) FROM sys_message WHERE is_read = 0")
    int countUnread();
}
