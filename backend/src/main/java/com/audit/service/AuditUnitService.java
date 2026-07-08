package com.audit.service;

import cn.hutool.core.util.IdUtil;
import com.audit.common.PageResult;
import com.audit.entity.AuditUnit;
import com.audit.mapper.AuditUnitMapper;
import com.audit.mapper.RectifyMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditUnitService {

    private final AuditUnitMapper mapper;
    private final RectifyMapper rectifyMapper;

    public AuditUnitService(AuditUnitMapper mapper, RectifyMapper rectifyMapper) {
        this.mapper = mapper;
        this.rectifyMapper = rectifyMapper;
    }

    public PageResult<AuditUnit> getList(String keyword, Integer category, int pageNum, int pageSize) {
        List<AuditUnit> all = mapper.findList(keyword, category);
        // 动态计算待整改数 + 关联领导干部
        for (AuditUnit u : all) {
            u.setPendingRectifyCount(rectifyMapper.countPendingByUnitId(u.getUnitId()));
        }
        int total = all.size();
        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        return new PageResult<>(total, pageNum, pageSize, from < total ? all.subList(from, to) : List.of());
    }

    public List<String> getLeadersByUnitName(String unitName) {
        return mapper.findLeaderNamesByUnitName(unitName);
    }

    public AuditUnit getByUnitId(String unitId) {
        AuditUnit u = mapper.findByUnitId(unitId);
        if (u != null) u.setPendingRectifyCount(rectifyMapper.countPendingByUnitId(unitId));
        return u;
    }

    public AuditUnit create(AuditUnit unit) {
        if (unit.getUnitId() == null || unit.getUnitId().isEmpty()) {
            unit.setUnitId(IdUtil.fastSimpleUUID().substring(0, 10).toUpperCase());
        }
        unit.setUnitCode(unit.getUnitCode() != null ? unit.getUnitCode() : "DW-" + java.time.LocalDate.now().getYear() + "-" + IdUtil.fastSimpleUUID().substring(0, 3));
        String[] cats = {"校内职能部门", "二级学院", "直属后勤单位", "校办企业", "基建项目部", "附属医院"};
        unit.setCategoryName(unit.getCategory() != null && unit.getCategory() < cats.length ? cats[unit.getCategory()] : "未知");
        mapper.insert(unit);
        return unit;
    }

    public void update(String unitId, AuditUnit unit) { unit.setUnitId(unitId); mapper.update(unit); }
    public void delete(String unitId) { mapper.deleteByUnitId(unitId); }
}
