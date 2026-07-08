package com.audit.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface TemplateMapper {

    @Select("<script>" +
            "SELECT template_id, template_name, plan_type, version, description FROM audit_template WHERE 1=1" +
            "<if test='planType != null'> AND plan_type = #{planType}</if>" +
            "<if test='keyword != null'> AND template_name LIKE CONCAT('%',#{keyword},'%')</if>" +
            " ORDER BY create_time DESC" +
            "</script>")
    List<Map<String, Object>> findList(@Param("planType") Integer planType, @Param("keyword") String keyword);

    @Select("SELECT * FROM audit_template WHERE template_id = #{templateId}")
    Map<String, Object> findById(@Param("templateId") String templateId);

    @Select("SELECT t.template_id, t.template_name, t.plan_type, t.version, t.description FROM audit_template t INNER JOIN batch_template bt ON t.template_id = bt.template_id WHERE bt.batch_id = #{batchId}")
    List<Map<String, Object>> findByBatchId(@Param("batchId") String batchId);

    @Insert("INSERT IGNORE INTO batch_template (batch_id, template_id) VALUES (#{batchId}, #{templateId})")
    int bindToBatch(@Param("batchId") String batchId, @Param("templateId") String templateId);

    @Delete("DELETE FROM batch_template WHERE batch_id = #{batchId} AND template_id = #{templateId}")
    int unbindFromBatch(@Param("batchId") String batchId, @Param("templateId") String templateId);
}
