package com.audit.mapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface MessageMapper {
    List<Map<String, Object>> findList(@Param("isRead") Integer isRead);
    int markRead(@Param("messageId") String messageId);
    int markAllRead();
    int countUnread();
    int insertAlert(@Param("messageId") String messageId, @Param("title") String title, @Param("content") String content, @Param("messageType") String messageType, @Param("related") String related);
}
