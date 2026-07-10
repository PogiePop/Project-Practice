package com.audit.mapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface CareerHistoryMapper {
    List<Map<String, Object>> findByLeaderId(@Param("leaderId") String leaderId);
    int insert(@Param("recordId") String recordId, @Param("leaderId") String leaderId, @Param("unitName") String unitName, @Param("position") String position, @Param("startDate") String startDate, @Param("endDate") String endDate, @Param("dutyDescription") String dutyDescription, @Param("fundScope") BigDecimal fundScope);
    int update(@Param("recordId") String recordId, @Param("leaderId") String leaderId, @Param("unitName") String unitName, @Param("position") String position, @Param("startDate") String startDate, @Param("endDate") String endDate, @Param("dutyDescription") String dutyDescription, @Param("fundScope") BigDecimal fundScope);
    int delete(@Param("recordId") String recordId, @Param("leaderId") String leaderId);
}
