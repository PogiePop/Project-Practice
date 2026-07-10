package com.audit.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {
    Map<String, Object> login(@Param("username") String username, @Param("password") String password);
    Map<String, Object> findByUsername(@Param("username") String username);
    /** 查找用户（含id、password），用于JWT登录鉴权 */
    Map<String, Object> findById(@Param("id") Long id);
    /** 按用户名查找并返回密码字段，用于BCrypt校验 */
    Map<String, Object> findByUsernameWithPassword(@Param("username") String username);
    int update(@Param("username") String username, @Param("realName") String realName, @Param("phone") String phone, @Param("email") String email, @Param("department") String department, @Param("position") String position);
    int changePassword(@Param("username") String username, @Param("oldPassword") String oldPassword, @Param("newPassword") String newPassword);
    List<Map<String, Object>> findAll(@Param("keyword") String keyword);
    int insertUser(@Param("username") String username, @Param("password") String password, @Param("realName") String realName, @Param("staffId") String staffId, @Param("department") String department, @Param("position") String position, @Param("phone") String phone, @Param("email") String email, @Param("roleLevel") int roleLevel);
    int updateUser(@Param("username") String username, @Param("realName") String realName, @Param("staffId") String staffId, @Param("department") String department, @Param("position") String position, @Param("phone") String phone, @Param("email") String email, @Param("roleLevel") Integer roleLevel);
    int deleteUser(@Param("username") String username);
    int resetPassword(@Param("username") String username, @Param("password") String password);
    /** BCrypt密码升级专用：按userId更新密码 */
    int updatePassword(@Param("userId") Long userId, @Param("password") String password);
}
