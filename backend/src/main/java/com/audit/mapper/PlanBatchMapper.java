package com.audit.mapper;

import com.audit.entity.PlanBatch;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface PlanBatchMapper {

    @Select("<script>" +
            "SELECT b.*, u.unit_name as unit_name FROM audit_plan_batch b " +
            "LEFT JOIN audit_object_unit u ON b.unit_id = u.unit_id WHERE 1=1" +
            "<if test='keyword != null'> AND (b.batch_id LIKE CONCAT('%',#{keyword},'%') OR b.batch_name LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "<if test='planType != null'> AND b.plan_type = #{planType}</if>" +
            "<if test='year != null'> AND b.year = #{year}</if>" +
            "<if test='approvalStatus != null'> AND b.approval_status = #{approvalStatus}</if>" +
            " ORDER BY b.create_time DESC" +
            "</script>")
    @Results({
        @Result(column = "unit_name", property = "unitName")
    })
    List<PlanBatch> findList(@Param("keyword") String keyword, @Param("planType") Integer planType,
                              @Param("year") Integer year, @Param("approvalStatus") Integer approvalStatus);

    @Select("SELECT * FROM audit_plan_batch WHERE batch_id = #{batchId}")
    PlanBatch findByBatchId(@Param("batchId") String batchId);

    @Insert("INSERT INTO audit_plan_batch (batch_id, batch_name, plan_type, year, project_count, start_date, end_date, approval_status, progress, audit_leader, audit_leader_id, remark) " +
            "VALUES (#{batchId}, #{batchName}, #{planType}, #{year}, #{projectCount}, #{startDate}, #{endDate}, #{approvalStatus}, #{progress}, #{auditLeader}, #{auditLeaderId}, #{remark})")
    int insert(PlanBatch batch);

    @Update("UPDATE audit_plan_batch SET batch_name=#{batchName}, plan_type=#{planType}, year=#{year}, start_date=#{startDate}, end_date=#{endDate}, audit_leader=#{auditLeader}, remark=#{remark}, unit_id=#{unitId}, leader_id=#{leaderId}, update_time=NOW() WHERE batch_id=#{batchId}")
    int update(PlanBatch batch);

    @Delete("DELETE FROM audit_plan_batch WHERE batch_id = #{batchId}")
    int deleteByBatchId(@Param("batchId") String batchId);

    @Update("UPDATE audit_plan_batch SET approval_status = #{status}, update_time = NOW() WHERE batch_id = #{batchId}")
    int updateApprovalStatus(@Param("batchId") String batchId, @Param("status") int status);

    @Update("UPDATE audit_plan_batch SET progress = #{progress}, update_time = NOW() WHERE batch_id = #{batchId}")
    int updateProgress(@Param("batchId") String batchId, @Param("progress") int progress);

    @Update("UPDATE audit_plan_batch SET project_count = #{count}, update_time = NOW() WHERE batch_id = #{batchId}")
    int updateProjectCount(@Param("batchId") String batchId, @Param("count") int count);

    @Select("SELECT COUNT(*) as totalPlanCount, " +
            "SUM(CASE WHEN approval_status = 0 THEN 1 ELSE 0 END) as approvedCount, " +
            "SUM(CASE WHEN approval_status = 1 THEN 1 ELSE 0 END) as approvingCount, " +
            "SUM(CASE WHEN progress > 0 AND progress < 100 AND end_date < CURDATE() THEN 1 ELSE 0 END) as alertCount " +
            "FROM audit_plan_batch")
    @Results({
        @Result(column = "totalPlanCount", property = "totalPlanCount"),
        @Result(column = "approvedCount", property = "approvedCount"),
        @Result(column = "approvingCount", property = "approvingCount"),
        @Result(column = "alertCount", property = "alertCount")
    })
    java.util.Map<String, Object> getSummary();

    @Select("<script>" +
            "SELECT b.batch_id as projectId, b.batch_name as projectName, " +
            "CONCAT(DATE_FORMAT(b.start_date,'%Y-%m'),' ~ ',DATE_FORMAT(b.end_date,'%Y-%m')) as period, " +
            "CASE b.approval_status WHEN 0 THEN '已审批' WHEN 1 THEN '审批中' WHEN 2 THEN '已归档' WHEN 3 THEN '草稿' WHEN 4 THEN '已驳回' ELSE '未知' END as status, " +
            "IFNULL(b.audit_conclusion, '') as conclusion, " +
            "(SELECT COUNT(*) FROM audit_rectify_ledger r WHERE r.batch_id = b.batch_id) as issueCount, " +
            "(SELECT COUNT(*) FROM audit_rectify_ledger r WHERE r.batch_id = b.batch_id AND r.rectify_status = 2) as rectifiedCount " +
            "FROM audit_plan_batch b WHERE (b.leader_id = #{leaderId} " +
            "<if test='unitName != null'> OR b.unit_id IN (SELECT unit_id FROM audit_object_unit WHERE unit_name = #{unitName})</if>)" +
            " ORDER BY b.create_time DESC" +
            "</script>")
    List<Map<String, Object>> findProjectsByLeader(@Param("leaderId") String leaderId, @Param("unitName") String unitName);
}
