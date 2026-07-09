package com.audit.mapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface TemplateMapper {
    List<Map<String, Object>> findList(@Param("planType") Integer planType, @Param("keyword") String keyword);
    Map<String, Object> findById(@Param("templateId") String templateId);
    List<Map<String, Object>> findByBatchId(@Param("batchId") String batchId);
    int bindToBatch(@Param("batchId") String batchId, @Param("templateId") String templateId);
    int unbindFromBatch(@Param("batchId") String batchId, @Param("templateId") String templateId);
}
