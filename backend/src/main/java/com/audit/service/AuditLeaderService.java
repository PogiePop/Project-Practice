package com.audit.service;

import cn.hutool.core.util.IdUtil;
import com.audit.common.PageResult;
import com.audit.entity.AuditLeader;
import com.audit.mapper.AuditLeaderMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AuditLeaderService {

    private final AuditLeaderMapper mapper;

    public AuditLeaderService(AuditLeaderMapper mapper) { this.mapper = mapper; }

    /**
     * 根据任职起始日期自动计算任职年限
     * 在职：从任职起始到今天的年数（精确到1位小数）
     * 离任：从任职起始到今天的年数
     */
    private BigDecimal calcTenureYears(AuditLeader leader) {
        if (leader.getTenureStartDate() == null) return BigDecimal.ZERO;
        long days = ChronoUnit.DAYS.between(leader.getTenureStartDate(), LocalDate.now());
        return BigDecimal.valueOf(Math.max(days, 0) / 365.0).setScale(1, RoundingMode.HALF_UP);
    }

    public PageResult<AuditLeader> getList(String keyword, Integer isActive, int pageNum, int pageSize) {
        List<AuditLeader> all = mapper.findList(keyword, isActive);
        // 动态计算在职干部的任职年限
        for (AuditLeader l : all) {
            if (l.getIsActive() != null && l.getIsActive() == 1) {
                l.setTenureYears(calcTenureYears(l));
            }
        }
        int total = all.size();
        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        return new PageResult<>(total, pageNum, pageSize, from < total ? all.subList(from, to) : List.of());
    }

    public AuditLeader getByLeaderId(String leaderId) {
        AuditLeader l = mapper.findByLeaderId(leaderId);
        if (l != null && l.getIsActive() != null && l.getIsActive() == 1) {
            l.setTenureYears(calcTenureYears(l));
        }
        return l;
    }

    public AuditLeader create(AuditLeader leader) {
        if (leader.getLeaderId() == null || leader.getLeaderId().isEmpty()) {
            leader.setLeaderId(IdUtil.fastSimpleUUID().substring(0, 10).toUpperCase());
        }
        leader.setLeaderCode("LD-" + LocalDate.now().getYear() + "-" + IdUtil.fastSimpleUUID().substring(0, 3));
        // 自动计算任职年限
        leader.setTenureYears(calcTenureYears(leader));
        mapper.insert(leader);
        return leader;
    }

    public void update(String leaderId, AuditLeader leader) {
        leader.setLeaderId(leaderId);
        // 更新时重新计算任职年限
        leader.setTenureYears(calcTenureYears(leader));
        mapper.update(leader);
    }
    public void delete(String leaderId) { mapper.deleteByLeaderId(leaderId); }

    public List<AuditLeader> getRecommendList() {
        List<AuditLeader> list = mapper.findRecommendList();
        for (AuditLeader l : list) {
            if (l.getIsActive() != null && l.getIsActive() == 1) {
                l.setTenureYears(calcTenureYears(l));
            }
        }
        return list;
    }
}
