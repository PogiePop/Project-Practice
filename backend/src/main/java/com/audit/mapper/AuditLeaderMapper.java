package com.audit.mapper;

import com.audit.entity.AuditLeader;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface AuditLeaderMapper {
    List<AuditLeader> findList(@Param("keyword") String keyword, @Param("isActive") Integer isActive);
    AuditLeader findByLeaderId(@Param("leaderId") String leaderId);
    int insert(AuditLeader leader);
    int update(AuditLeader leader);
    int deleteByLeaderId(@Param("leaderId") String leaderId);
    List<AuditLeader> findRecommendList();
    int updateFundScope(@Param("leaderId") String leaderId, @Param("fundScope") BigDecimal fundScope);
    int updateLatestAuditDate(@Param("leaderId") String leaderId);
    int incrementAuditCount(@Param("leaderId") String leaderId);
    int updateAuditCount(@Param("leaderId") String leaderId, @Param("count") int count);
}
