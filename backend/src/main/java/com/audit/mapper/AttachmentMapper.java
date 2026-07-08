package com.audit.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface AttachmentMapper {

    @Select("SELECT attach_id, file_name, file_size, file_type, attach_type, attach_type_name, upload_time, upload_by FROM audit_attachment WHERE batch_id = #{batchId} ORDER BY upload_time DESC")
    List<Map<String, Object>> findByBatchId(@Param("batchId") String batchId);

    @Select("SELECT * FROM audit_attachment WHERE attach_id = #{attachId}")
    Map<String, Object> findByAttachId(@Param("attachId") String attachId);

    @Insert("INSERT INTO audit_attachment (attach_id, batch_id, file_name, file_size, file_type, attach_type, attach_type_name, upload_time, upload_by, file_path) VALUES (#{attachId}, #{batchId}, #{fileName}, #{fileSize}, #{fileType}, #{attachType}, #{attachTypeName}, NOW(), #{uploadBy}, #{filePath})")
    int insert(@Param("attachId") String attachId, @Param("batchId") String batchId,
               @Param("fileName") String fileName, @Param("fileSize") long fileSize,
               @Param("fileType") String fileType, @Param("attachType") String attachType,
               @Param("attachTypeName") String attachTypeName, @Param("uploadBy") String uploadBy,
               @Param("filePath") String filePath);

    @Delete("DELETE FROM audit_attachment WHERE attach_id = #{attachId} AND batch_id = #{batchId}")
    int delete(@Param("attachId") String attachId, @Param("batchId") String batchId);
}
