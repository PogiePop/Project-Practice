package com.audit.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface RectifyMapper {

    @Select("SELECT rectify_id, batch_id, unit_id, leader_id, issue_description, issue_category, rectify_status, responsible_person, deadline, rectify_progress FROM audit_rectify_ledger WHERE batch_id = #{batchId}")
    List<Map<String, Object>> findByBatchId(@Param("batchId") String batchId);

    @Select("SELECT rectify_id, batch_id, unit_id, leader_id, issue_description, issue_category, rectify_status, responsible_person, deadline, rectify_progress FROM audit_rectify_ledger WHERE unit_id = #{unitId}")
    List<Map<String, Object>> findByUnitId(@Param("unitId") String unitId);

    @Select("SELECT COUNT(*) FROM audit_rectify_ledger WHERE unit_id = #{unitId} AND rectify_status != 2")
    int countPendingByUnitId(@Param("unitId") String unitId);

    @Insert("INSERT INTO audit_rectify_ledger (rectify_id, batch_id, unit_id, issue_description, issue_category, rectify_status, responsible_person, deadline) " +
            "VALUES (#{rectifyId}, #{batchId}, #{unitId}, #{issue}, '审批驳回', 0, #{responsible}, DATE_ADD(NOW(), INTERVAL 30 DAY))")
    int insertRejectionRectify(@Param("rectifyId") String rectifyId, @Param("batchId") String batchId,
                                @Param("unitId") String unitId, @Param("issue") String issue,
                                @Param("responsible") String responsible);

    @Delete("DELETE FROM audit_rectify_ledger WHERE batch_id = #{batchId} AND issue_category = '审批驳回'")
    int deleteRejectionRectifies(@Param("batchId") String batchId);
}
