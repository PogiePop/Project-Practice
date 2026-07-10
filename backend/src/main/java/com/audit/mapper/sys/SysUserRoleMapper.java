package com.audit.mapper.sys;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 用户-角色关联Mapper — 中间表 sys_user_role CRUD
 */
@Mapper
public interface SysUserRoleMapper {

    /** 查询用户已有的角色ID列表 */
    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);

    /** 删除用户所有角色关联 */
    int deleteByUserId(@Param("userId") Long userId);

    /** 批量插入用户角色关联（分配角色） */
    int insertBatch(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);
}
