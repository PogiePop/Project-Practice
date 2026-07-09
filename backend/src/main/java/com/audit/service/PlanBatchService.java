package com.audit.service;

import com.audit.common.PageResult;
import com.audit.entity.PlanBatch;
import java.util.Map;

public interface PlanBatchService {
    PageResult<PlanBatch> getList(String keyword, Integer planType, Integer year, Integer approvalStatus, int pageNum, int pageSize);
    PlanBatch getByBatchId(String batchId);
    PlanBatch create(PlanBatch batch);
    void update(String batchId, PlanBatch batch);
    void delete(String batchId);
    Map<String, Object> getSummary();
    void updateLeaderFunds(String leaderId);
}
