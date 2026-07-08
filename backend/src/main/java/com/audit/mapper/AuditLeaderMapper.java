package com.audit.mapper;

import com.audit.entity.AuditLeader;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AuditLeaderMapper {

    @Select("<script>" +
            "SELECT * FROM audit_object_leader WHERE 1=1" +
            "<if test='keyword != null'> AND (leader_name LIKE CONCAT('%',#{keyword},'%') OR staff_id LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "<if test='isActive != null'> AND is_active = #{isActive}</if>" +
            " ORDER BY create_time DESC" +
            "</script>")
    List<AuditLeader> findList(@Param("keyword") String keyword, @Param("isActive") Integer isActive);

    @Select("SELECT * FROM audit_object_leader WHERE leader_id = #{leaderId}")
    AuditLeader findByLeaderId(@Param("leaderId") String leaderId);

    @Insert("INSERT INTO audit_object_leader (leader_id, leader_code, leader_name, staff_id, current_unit_name, current_position, is_active, tenure_start_date, tenure_years, fund_scope) " +
            "VALUES (#{leaderId}, #{leaderCode}, #{leaderName}, #{staffId}, #{currentUnitName}, #{currentPosition}, #{isActive}, #{tenureStartDate}, #{tenureYears}, #{fundScope})")
    int insert(AuditLeader leader);

    @Update("UPDATE audit_object_leader SET leader_name=#{leaderName}, staff_id=#{staffId}, current_unit_name=#{currentUnitName}, current_position=#{currentPosition}, is_active=#{isActive}, tenure_start_date=#{tenureStartDate}, tenure_years=#{tenureYears}, fund_scope=#{fundScope} WHERE leader_id=#{leaderId}")
    int update(AuditLeader leader);

    @Delete("DELETE FROM audit_object_leader WHERE leader_id = #{leaderId}")
    int deleteByLeaderId(@Param("leaderId") String leaderId);

    @Select("SELECT * FROM audit_object_leader WHERE is_active = 1 AND tenure_years >= 3 ORDER BY tenure_years DESC")
    List<AuditLeader> findRecommendList();
}
