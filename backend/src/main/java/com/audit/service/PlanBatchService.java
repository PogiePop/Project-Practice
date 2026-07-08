package com.audit.service;

import cn.hutool.core.util.IdUtil;
import com.audit.common.PageResult;
import com.audit.entity.PlanBatch;
import com.audit.mapper.PlanBatchMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PlanBatchService {

    private final PlanBatchMapper mapper;

    public PlanBatchService(PlanBatchMapper mapper) { this.mapper = mapper; }

    public PageResult<PlanBatch> getList(String keyword, Integer planType, Integer year, Integer approvalStatus, int pageNum, int pageSize) {
        List<PlanBatch> all = mapper.findList(keyword, planType, year, approvalStatus);
        int total = all.size();
        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<PlanBatch> page = from < total ? all.subList(from, to) : List.of();
        return new PageResult<>(total, pageNum, pageSize, page);
    }

    public PlanBatch getByBatchId(String batchId) { return mapper.findByBatchId(batchId); }

    public PlanBatch create(PlanBatch batch) {
        if (batch.getBatchId() == null || batch.getBatchId().isEmpty()) {
            batch.setBatchId("JH" + java.time.LocalDate.now().getYear() + IdUtil.fastSimpleUUID().substring(0, 4));
        }
        if (batch.getApprovalStatus() == null) batch.setApprovalStatus(3);
        if (batch.getProgress() == null) batch.setProgress(0);
        if (batch.getProjectCount() == null) batch.setProjectCount(0);
        mapper.insert(batch);
        return batch;
    }

    public void update(String batchId, PlanBatch batch) {
        batch.setBatchId(batchId);
        mapper.update(batch);
    }

    public void delete(String batchId) { mapper.deleteByBatchId(batchId); }

    public Map<String, Object> getSummary() { return mapper.getSummary(); }
}
