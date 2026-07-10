package com.audit.mapper;

import com.audit.entity.PlanBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface PlanBatchMapper {

    List<PlanBatch> findList(@Param("keyword") String keyword, @Param("planType") Integer planType,
                              @Param("year") Integer year, @Param("approvalStatus") Integer approvalStatus);

    PlanBatch findByBatchId(@Param("batchId") String batchId);

    int insert(PlanBatch batch);

    int update(PlanBatch batch);

    int deleteByBatchId(@Param("batchId") String batchId);

    int updateApprovalStatus(@Param("batchId") String batchId, @Param("status") int status);

    int updateProgress(@Param("batchId") String batchId, @Param("progress") int progress);

    int updateProjectCount(@Param("batchId") String batchId, @Param("count") int count);

    int updateAmounts(PlanBatch batch);

    Map<String, Object> getSummary();

    List<Map<String, Object>> findProjectsByLeader(@Param("leaderId") String leaderId, @Param("unitName") String unitName);
}
