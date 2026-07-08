package com.audit.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ApprovalMapper {

    @Select("SELECT step_order as stepOrder, step_name as stepName, status, approver_name as approverName, comment, operate_time as operateTime FROM approval_step WHERE batch_id = #{batchId} ORDER BY step_order")
    List<Map<String, Object>> getSteps(@Param("batchId") String batchId);

    @Update("UPDATE approval_step SET status = #{status}, approver_name = #{approverName}, comment = #{comment}, operate_time = NOW() WHERE batch_id = #{batchId} AND step_order = #{stepOrder}")
    int updateStep(@Param("batchId") String batchId, @Param("stepOrder") int stepOrder,
                   @Param("status") String status, @Param("approverName") String approverName, @Param("comment") String comment);

    @Select("SELECT * FROM approval_history WHERE batch_id = #{batchId} ORDER BY submit_time DESC")
    List<Map<String, Object>> getHistory(@Param("batchId") String batchId);

    @Select("SELECT * FROM plan_change WHERE batch_id = #{batchId} ORDER BY apply_time DESC")
    List<Map<String, Object>> getChanges(@Param("batchId") String batchId);

    @Insert("INSERT INTO plan_change (change_id, batch_id, change_type, change_type_name, reason, change_data, approval_status, approval_status_name, apply_time) " +
            "VALUES (#{changeId}, #{batchId}, #{changeType}, #{changeTypeName}, #{reason}, #{changeData}, 1, '待确认', NOW())")
    int insertChange(@Param("changeId") String changeId, @Param("batchId") String batchId,
                     @Param("changeType") int changeType, @Param("changeTypeName") String changeTypeName,
                     @Param("reason") String reason, @Param("changeData") String changeData);

    @Update("UPDATE plan_change SET approval_status = 0, approval_status_name = '已确认' WHERE change_id = #{changeId}")
    int approveChange(@Param("changeId") String changeId);

    @Insert("INSERT INTO approval_step (batch_id, step_order, step_name, status, approver_name, comment, operate_time) " +
            "VALUES (#{batchId}, #{stepOrder}, #{stepName}, #{status}, #{approverName}, #{comment}, NOW())")
    int insertStep(@Param("batchId") String batchId, @Param("stepOrder") int stepOrder,
                   @Param("stepName") String stepName, @Param("status") String status,
                   @Param("approverName") String approverName, @Param("comment") String comment);

    @Insert("INSERT INTO approval_history (approval_id, batch_id, flow_type, status, submit_by, submit_time, result) " +
            "VALUES (#{approvalId}, #{batchId}, 'NEW_PLAN', '进行中', #{submitBy}, NOW(), #{result})")
    int insertHistory(@Param("approvalId") String approvalId, @Param("batchId") String batchId,
                      @Param("submitBy") String submitBy, @Param("result") String result);
}
