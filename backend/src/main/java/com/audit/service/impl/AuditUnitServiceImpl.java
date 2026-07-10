package com.audit.service.impl;

import cn.hutool.core.util.IdUtil;
import com.audit.common.PageResult;
import com.audit.entity.AuditUnit;
import com.audit.mapper.AuditUnitMapper;
import com.audit.mapper.RectifyMapper;
import com.audit.service.AuditUnitService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditUnitServiceImpl implements AuditUnitService {

    private final AuditUnitMapper mapper;
    private final RectifyMapper rectifyMapper;

    public AuditUnitServiceImpl(AuditUnitMapper mapper, RectifyMapper rectifyMapper) {
        this.mapper = mapper;
        this.rectifyMapper = rectifyMapper;
    }

    @Override
    public PageResult<AuditUnit> getList(String keyword, Integer category, int pageNum, int pageSize) {
        List<AuditUnit> all = mapper.findList(keyword, category);
        for (AuditUnit u : all) {
            u.setPendingRectifyCount(rectifyMapper.countPendingByUnitId(u.getUnitId()));
            resolvePhones(u);
        }
        int total = all.size();
        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        return new PageResult<>(total, pageNum, pageSize, from < total ? all.subList(from, to) : List.of());
    }

    @Override
    public AuditUnit getByUnitId(String unitId) {
        AuditUnit u = mapper.findByUnitId(unitId);
        if (u != null) {
            u.setPendingRectifyCount(rectifyMapper.countPendingByUnitId(unitId));
            resolvePhones(u);
        }
        return u;
    }

    @Override
    public AuditUnit create(AuditUnit unit) {
        if ((unit.getUnitId() != null && !unit.getUnitId().isEmpty()) || (unit.getUnitCode() != null && !unit.getUnitCode().isEmpty())) {
            List<AuditUnit> all = mapper.findList(null, null);
            for (AuditUnit u : all) {
                if ((unit.getUnitId() != null && unit.getUnitId().equals(u.getUnitId()))
                    || (unit.getUnitCode() != null && unit.getUnitCode().equals(u.getUnitCode()))) {
                    unit.setUnitId(u.getUnitId());
                    update(u.getUnitId(), unit);
                    return unit;
                }
            }
        }
        if (unit.getUnitId() == null || unit.getUnitId().isEmpty()) {
            unit.setUnitId(IdUtil.fastSimpleUUID().substring(0, 10).toUpperCase());
        }
        unit.setUnitCode(unit.getUnitCode() != null ? unit.getUnitCode() : "DW-" + java.time.LocalDate.now().getYear() + "-" + IdUtil.fastSimpleUUID().substring(0, 3));
        String[] cats = {"校内职能部门","二级学院","直属后勤单位","校办企业","基建项目部","附属医院"};
        unit.setCategoryName(unit.getCategory() != null && unit.getCategory() < cats.length ? cats[unit.getCategory()] : "未知");
        mapper.insert(unit);
        return unit;
    }

    @Override
    public void update(String unitId, AuditUnit unit) { unit.setUnitId(unitId); mapper.update(unit); }

    @Override
    public void delete(String unitId) { mapper.deleteByUnitId(unitId); }

    @Override
    public List<String> getLeadersByUnitName(String unitName) { return mapper.findLeaderNamesByUnitName(unitName); }

    private void resolvePhones(AuditUnit u) {
        var leaders = mapper.findAllLeaders();
        for (var l : leaders) {
            String name = (String) l.get("leader_name");
            String phone = (String) l.get("phone");
            if (name == null || phone == null) continue;
            if (name.equals(u.getLeaderInCharge())) u.setLeaderInChargePhone(phone);
            if (name.equals(u.getFinanceContact())) u.setFinanceContactPhone(phone);
        }
    }
}
