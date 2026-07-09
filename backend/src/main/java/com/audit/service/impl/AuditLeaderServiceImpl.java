package com.audit.service.impl;

import cn.hutool.core.util.IdUtil;
import com.audit.common.PageResult;
import com.audit.entity.AuditLeader;
import com.audit.entity.PlanBatch;
import com.audit.mapper.AuditLeaderMapper;
import com.audit.mapper.PlanBatchMapper;
import com.audit.service.AuditLeaderService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AuditLeaderServiceImpl implements AuditLeaderService {

    private final AuditLeaderMapper mapper;
    private final PlanBatchMapper batchMapper;

    public AuditLeaderServiceImpl(AuditLeaderMapper mapper, PlanBatchMapper batchMapper) {
        this.mapper = mapper;
        this.batchMapper = batchMapper;
    }

    @Override
    public PageResult<AuditLeader> getList(String keyword, Integer isActive, int pageNum, int pageSize) {
        List<AuditLeader> all = mapper.findList(keyword, isActive);
        for (AuditLeader l : all) {
            if (l.getIsActive() != null && l.getIsActive() == 1) {
                l.setTenureYears(calcTenureYears(l));
            }
            l.setFundScope(calcFundScope(l.getLeaderId()));
        }
        int total = all.size();
        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        return new PageResult<>(total, pageNum, pageSize, from < total ? all.subList(from, to) : List.of());
    }

    @Override
    public AuditLeader getByLeaderId(String leaderId) {
        AuditLeader l = mapper.findByLeaderId(leaderId);
        if (l != null && l.getIsActive() != null && l.getIsActive() == 1) {
            l.setTenureYears(calcTenureYears(l));
            l.setFundScope(calcFundScope(leaderId));
        }
        return l;
    }

    @Override
    public AuditLeader create(AuditLeader leader) {
        if (leader.getStaffId() != null && !leader.getStaffId().isEmpty()) {
            List<AuditLeader> all = mapper.findList(null, null);
            for (AuditLeader l : all) {
                if (leader.getStaffId().equals(l.getStaffId()) || (leader.getLeaderId() != null && leader.getLeaderId().equals(l.getLeaderId()))) {
                    leader.setLeaderId(l.getLeaderId());
                    update(l.getLeaderId(), leader);
                    return leader;
                }
            }
        }
        if (leader.getLeaderId() == null || leader.getLeaderId().isEmpty()) {
            leader.setLeaderId(IdUtil.fastSimpleUUID().substring(0, 10).toUpperCase());
        }
        leader.setLeaderCode("LD-" + LocalDate.now().getYear() + "-" + IdUtil.fastSimpleUUID().substring(0, 3));
        leader.setTenureYears(calcTenureYears(leader));
        leader.setFundScope(BigDecimal.ZERO);
        mapper.insert(leader);
        return leader;
    }

    @Override
    public void update(String leaderId, AuditLeader leader) {
        leader.setLeaderId(leaderId);
        leader.setTenureYears(calcTenureYears(leader));
        mapper.update(leader);
        if (leader.getAuditCount() != null) mapper.updateAuditCount(leaderId, leader.getAuditCount());
        if (leader.getLatestAuditDate() != null) mapper.updateLatestAuditDate(leaderId);
    }

    @Override
    public void delete(String leaderId) { mapper.deleteByLeaderId(leaderId); }

    @Override
    public List<AuditLeader> getRecommendList() {
        List<AuditLeader> list = mapper.findRecommendList();
        for (AuditLeader l : list) {
            if (l.getIsActive() != null && l.getIsActive() == 1) l.setTenureYears(calcTenureYears(l));
        }
        return list;
    }

    private BigDecimal calcTenureYears(AuditLeader leader) {
        if (leader.getTenureStartDate() == null) return BigDecimal.ZERO;
        long days = ChronoUnit.DAYS.between(leader.getTenureStartDate(), LocalDate.now());
        return BigDecimal.valueOf(Math.max(days, 0) / 365.0).setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal calcFundScope(String leaderId) {
        if (leaderId == null) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (PlanBatch b : batchMapper.findList(null, null, null, null)) {
            if (leaderId.equals(b.getLeaderId()) && b.getProjectAmount() != null) {
                total = total.add(b.getProjectAmount());
            }
        }
        return total;
    }
}
