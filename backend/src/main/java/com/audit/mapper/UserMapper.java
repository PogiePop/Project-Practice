package com.audit.mapper;

import org.apache.ibatis.annotations.*;

import java.util.Map;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM sys_user WHERE username = #{username} AND password = #{password}")
    Map<String, Object> login(@Param("username") String username, @Param("password") String password);

    @Select("SELECT username, real_name as realName, staff_id as staffId, department, position, phone, email, role, avatar, last_login as lastLogin FROM sys_user WHERE username = #{username}")
    Map<String, Object> findByUsername(@Param("username") String username);

    @Update("<script>" +
            "UPDATE sys_user SET" +
            "<if test='realName != null'> real_name = #{realName},</if>" +
            "<if test='phone != null'> phone = #{phone},</if>" +
            "<if test='email != null'> email = #{email},</if>" +
            "<if test='department != null'> department = #{department},</if>" +
            "<if test='position != null'> position = #{position},</if>" +
            " real_name = real_name WHERE username = #{username}" +
            "</script>")
    int update(@Param("username") String username, @Param("realName") String realName,
               @Param("phone") String phone, @Param("email") String email,
               @Param("department") String department, @Param("position") String position);

    @Update("UPDATE sys_user SET password = #{newPassword} WHERE username = #{username} AND password = #{oldPassword}")
    int changePassword(@Param("username") String username, @Param("oldPassword") String oldPassword, @Param("newPassword") String newPassword);
}
