package com.audit.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface ApprovalMapper {
    List<Map<String, Object>> getSteps(@Param("batchId") String batchId);
    int updateStep(@Param("batchId") String batchId, @Param("stepOrder") int stepOrder, @Param("status") String status, @Param("approverName") String approverName, @Param("comment") String comment);
    List<Map<String, Object>> getHistory(@Param("batchId") String batchId);
    List<Map<String, Object>> getChanges(@Param("batchId") String batchId);
    List<Map<String, Object>> getAllHistory();
    int insertChange(@Param("changeId") String changeId, @Param("batchId") String batchId, @Param("changeType") int changeType, @Param("changeTypeName") String changeTypeName, @Param("reason") String reason, @Param("changeData") String changeData);
    int insertStep(@Param("batchId") String batchId, @Param("stepOrder") int stepOrder, @Param("stepName") String stepName, @Param("status") String status, @Param("approverName") String approverName, @Param("comment") String comment);
    int insertHistory(@Param("approvalId") String approvalId, @Param("batchId") String batchId, @Param("submitBy") String submitBy, @Param("result") String result);
    int approveChange(@Param("changeId") String changeId);
}
