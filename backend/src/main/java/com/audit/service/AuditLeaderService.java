package com.audit.service;

import com.audit.common.PageResult;
import com.audit.entity.AuditLeader;
import java.util.List;

public interface AuditLeaderService {
    PageResult<AuditLeader> getList(String keyword, Integer isActive, int pageNum, int pageSize);
    AuditLeader getByLeaderId(String leaderId);
    AuditLeader create(AuditLeader leader);
    void update(String leaderId, AuditLeader leader);
    void delete(String leaderId);
    List<AuditLeader> getRecommendList();
}
