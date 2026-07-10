package com.audit.mapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface AttachmentMapper {
    List<Map<String, Object>> findByBatchId(@Param("batchId") String batchId);
    Map<String, Object> findByAttachId(@Param("attachId") String attachId);
    int insert(@Param("attachId") String attachId, @Param("batchId") String batchId, @Param("fileName") String fileName, @Param("fileSize") long fileSize, @Param("fileType") String fileType, @Param("attachType") String attachType, @Param("attachTypeName") String attachTypeName, @Param("uploadBy") String uploadBy, @Param("filePath") String filePath);
    int delete(@Param("attachId") String attachId, @Param("batchId") String batchId);
}
