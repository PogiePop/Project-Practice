package com.audit.mapper;

import com.audit.entity.AuditUnit;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AuditUnitMapper {

    @Select("<script>" +
            "SELECT * FROM audit_object_unit WHERE 1=1" +
            "<if test='keyword != null'> AND (unit_name LIKE CONCAT('%',#{keyword},'%') OR unit_id LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "<if test='category != null'> AND category = #{category}</if>" +
            " ORDER BY create_time DESC" +
            "</script>")
    List<AuditUnit> findList(@Param("keyword") String keyword, @Param("category") Integer category);

    @Select("SELECT * FROM audit_object_unit WHERE unit_id = #{unitId}")
    AuditUnit findByUnitId(@Param("unitId") String unitId);

    @Insert("INSERT INTO audit_object_unit (unit_id, unit_code, unit_name, category, category_name, establishment_count, fund_scale, leader_in_charge, finance_contact, finance_contact_phone, address, setup_date, total_audit_count, latest_audit_date, pending_rectify_count) " +
            "VALUES (#{unitId}, #{unitCode}, #{unitName}, #{category}, #{categoryName}, #{establishmentCount}, #{fundScale}, #{leaderInCharge}, #{financeContact}, #{financeContactPhone}, #{address}, #{setupDate}, #{totalAuditCount}, #{latestAuditDate}, #{pendingRectifyCount})")
    int insert(AuditUnit unit);

    @Update("<script>" +
            "UPDATE audit_object_unit SET" +
            "<if test='unitName != null'> unit_name = #{unitName},</if>" +
            "<if test='category != null'> category = #{category},</if>" +
            "<if test='establishmentCount != null'> establishment_count = #{establishmentCount},</if>" +
            "<if test='fundScale != null'> fund_scale = #{fundScale},</if>" +
            "<if test='leaderInCharge != null'> leader_in_charge = #{leaderInCharge},</if>" +
            "<if test='financeContact != null'> finance_contact = #{financeContact},</if>" +
            "<if test='financeContactPhone != null'> finance_contact_phone = #{financeContactPhone},</if>" +
            " unit_name = #{unitName} WHERE unit_id = #{unitId}" +
            "</script>")
    int update(AuditUnit unit);

    @Delete("DELETE FROM audit_object_unit WHERE unit_id = #{unitId}")
    int deleteByUnitId(@Param("unitId") String unitId);

    @Select("SELECT leader_name FROM audit_object_leader WHERE current_unit_name = #{unitName} AND is_active = 1")
    List<String> findLeaderNamesByUnitName(@Param("unitName") String unitName);
}
