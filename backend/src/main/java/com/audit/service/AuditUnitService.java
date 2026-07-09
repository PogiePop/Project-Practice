package com.audit.service;

import com.audit.common.PageResult;
import com.audit.entity.AuditUnit;
import java.util.List;

public interface AuditUnitService {
    PageResult<AuditUnit> getList(String keyword, Integer category, int pageNum, int pageSize);
    AuditUnit getByUnitId(String unitId);
    AuditUnit create(AuditUnit unit);
    void update(String unitId, AuditUnit unit);
    void delete(String unitId);
    List<String> getLeadersByUnitName(String unitName);
}
