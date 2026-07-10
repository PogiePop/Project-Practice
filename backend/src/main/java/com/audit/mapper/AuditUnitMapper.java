package com.audit.mapper;

import com.audit.entity.AuditUnit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface AuditUnitMapper {
    List<AuditUnit> findList(@Param("keyword") String keyword, @Param("category") Integer category);
    AuditUnit findByUnitId(@Param("unitId") String unitId);
    int insert(AuditUnit unit);
    int update(AuditUnit unit);
    int deleteByUnitId(@Param("unitId") String unitId);
    int incrementAuditStats(@Param("unitId") String unitId, @Param("pending") int pending);
    int incrementAuditCount(@Param("unitId") String unitId);
    int updateLatestAuditDate(@Param("unitId") String unitId);
    List<String> findLeaderNamesByUnitName(@Param("unitName") String unitName);
    List<Map<String, Object>> findAllLeaders();
}
