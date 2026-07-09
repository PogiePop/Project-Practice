package com.audit.mapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface RectifyMapper {
    List<Map<String, Object>> findByBatchId(@Param("batchId") String batchId);
    List<Map<String, Object>> findByUnitId(@Param("unitId") String unitId);
    int countPendingByUnitId(@Param("unitId") String unitId);
    int insertRejectionRectify(@Param("rectifyId") String rectifyId, @Param("batchId") String batchId, @Param("unitId") String unitId, @Param("issue") String issue, @Param("responsible") String responsible);
    int deleteRejectionRectifies(@Param("batchId") String batchId);
    int markRejectionResolved(@Param("batchId") String batchId);
}
