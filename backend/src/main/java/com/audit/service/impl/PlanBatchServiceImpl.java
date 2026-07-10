package com.audit.service.impl;

import cn.hutool.core.util.IdUtil;
import com.audit.common.PageResult;
import com.audit.entity.AuditLeader;
import com.audit.entity.AuditUnit;
import com.audit.entity.PlanBatch;
import com.audit.mapper.AuditLeaderMapper;
import com.audit.mapper.AuditUnitMapper;
import com.audit.mapper.PlanBatchMapper;
import com.audit.service.PlanBatchService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class PlanBatchServiceImpl implements PlanBatchService {

    private final PlanBatchMapper mapper;
    private final AuditUnitMapper unitMapper;
    private final AuditLeaderMapper leaderMapper;

    public PlanBatchServiceImpl(PlanBatchMapper mapper, AuditUnitMapper unitMapper, AuditLeaderMapper leaderMapper) {
        this.mapper = mapper;
        this.unitMapper = unitMapper;
        this.leaderMapper = leaderMapper;
    }

    @Override
    public PageResult<PlanBatch> getList(String keyword, Integer planType, Integer year, Integer approvalStatus, int pageNum, int pageSize) {
        List<PlanBatch> all = mapper.findList(keyword, planType, year, approvalStatus);
        int total = all.size();
        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        return new PageResult<>(total, pageNum, pageSize, from < total ? all.subList(from, to) : List.of());
    }

    @Override
    public PlanBatch getByBatchId(String batchId) { return mapper.findByBatchId(batchId); }

    @Override
    public PlanBatch create(PlanBatch batch) {
        if (batch.getBatchId() == null || batch.getBatchId().isEmpty()) {
            batch.setBatchId("JH" + java.time.LocalDate.now().getYear() + IdUtil.fastSimpleUUID().substring(0, 4));
        }
        if (batch.getApprovalStatus() == null) batch.setApprovalStatus(3);
        if (batch.getProgress() == null) batch.setProgress(0);
        if (batch.getProjectCount() == null) batch.setProjectCount(1);
        if (batch.getProjectAmount() != null && batch.getProjectAmount().compareTo(BigDecimal.ZERO) > 0 && batch.getUnitId() != null) {
            AuditUnit unit = unitMapper.findByUnitId(batch.getUnitId());
            if (unit != null && unit.getFundScale() != null && unit.getFundScale().compareTo(BigDecimal.ZERO) > 0) {
                if (batch.getProjectAmount().compareTo(unit.getFundScale()) > 0) {
                    throw new RuntimeException("项目金额超出单位可用资金");
                }
            }
        }
        mapper.insert(batch);
        cascadeAmounts(batch);
        return batch;
    }

    @Override
    public void update(String batchId, PlanBatch batch) {
        PlanBatch old = mapper.findByBatchId(batchId);
        String oldLeaderId = old != null ? old.getLeaderId() : null;
        batch.setBatchId(batchId);
        mapper.update(batch);
        if (batch.getApprovalStatus() != null) mapper.updateApprovalStatus(batchId, batch.getApprovalStatus());
        if (batch.getProgress() != null) mapper.updateProgress(batchId, batch.getProgress());
        if (oldLeaderId != null && !oldLeaderId.equals(batch.getLeaderId())) {
            updateLeaderFunds(oldLeaderId);
        }
        if (batch.getLeaderId() != null) {
            updateLeaderFunds(batch.getLeaderId());
        }
    }

    @Override
    public void delete(String batchId) { mapper.deleteByBatchId(batchId); }

    @Override
    public Map<String, Object> getSummary() { return mapper.getSummary(); }

    private void cascadeAmounts(PlanBatch batch) {
        if (batch.getProjectAmount() != null && batch.getProjectAmount().compareTo(BigDecimal.ZERO) > 0 && batch.getUnitId() != null) {
            AuditUnit unit = unitMapper.findByUnitId(batch.getUnitId());
            if (unit != null) {
                BigDecimal current = unit.getFundScale() != null ? unit.getFundScale() : BigDecimal.ZERO;
                unit.setFundScale(current.subtract(batch.getProjectAmount()));
                unitMapper.update(unit);
            }
        }
        if (batch.getLeaderId() != null) {
            leaderMapper.updateFundScope(batch.getLeaderId(), recalcLeaderFundScope(batch.getLeaderId()));
        }
    }

    private BigDecimal recalcLeaderFundScope(String leaderId) {
        BigDecimal total = BigDecimal.ZERO;
        for (PlanBatch b : mapper.findList(null, null, null, null)) {
            if (leaderId.equals(b.getLeaderId()) && b.getProjectAmount() != null) {
                total = total.add(b.getProjectAmount());
            }
        }
        return total;
    }

    @Override
    public void updateLeaderFunds(String leaderId) {
        if (leaderId == null) return;
        leaderMapper.updateFundScope(leaderId, recalcLeaderFundScope(leaderId));
    }
}
