package com.audit.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface CareerHistoryMapper {

    @Select("SELECT record_id, leader_id, unit_name, position, start_date, end_date, duty_description, fund_scope, source FROM leader_career_history WHERE leader_id = #{leaderId} ORDER BY start_date DESC")
    List<Map<String, Object>> findByLeaderId(@Param("leaderId") String leaderId);

    @Insert("INSERT INTO leader_career_history (record_id, leader_id, unit_name, position, start_date, end_date, duty_description, fund_scope, source) VALUES (#{recordId}, #{leaderId}, #{unitName}, #{position}, #{startDate}, #{endDate}, #{dutyDescription}, #{fundScope}, '手动录入')")
    int insert(@Param("recordId") String recordId, @Param("leaderId") String leaderId,
               @Param("unitName") String unitName, @Param("position") String position,
               @Param("startDate") String startDate, @Param("endDate") String endDate,
               @Param("dutyDescription") String dutyDescription, @Param("fundScope") java.math.BigDecimal fundScope);

    @Update("UPDATE leader_career_history SET unit_name=#{unitName}, position=#{position}, start_date=#{startDate}, end_date=#{endDate}, duty_description=#{dutyDescription}, fund_scope=#{fundScope} WHERE record_id=#{recordId} AND leader_id=#{leaderId}")
    int update(@Param("recordId") String recordId, @Param("leaderId") String leaderId,
               @Param("unitName") String unitName, @Param("position") String position,
               @Param("startDate") String startDate, @Param("endDate") String endDate,
               @Param("dutyDescription") String dutyDescription, @Param("fundScope") java.math.BigDecimal fundScope);

    @Delete("DELETE FROM leader_career_history WHERE record_id = #{recordId} AND leader_id = #{leaderId}")
    int delete(@Param("recordId") String recordId, @Param("leaderId") String leaderId);
}
