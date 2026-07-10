package com.audit.mapper.sys;

import com.audit.entity.sys.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 角色Mapper — 角色CRUD + 权限关联操作
 */
@Mapper
public interface SysRoleMapper {

    // ==================== 角色CRUD ====================

    List<SysRole> findAll(@Param("keyword") String keyword);

    SysRole findById(@Param("roleId") Long roleId);

    SysRole findByKey(@Param("roleKey") String roleKey);

    int insert(SysRole role);

    int update(SysRole role);

    int delete(@Param("roleId") Long roleId);

    // ==================== 角色-权限关联 ====================

    /** 查询角色已分配的权限ID列表 */
    List<Long> findPermIdsByRoleId(@Param("roleId") Long roleId);

    /** 删除角色的所有权限关联 */
    int deleteRolePerms(@Param("roleId") Long roleId);

    /** 批量插入角色权限关联 */
    int insertRolePerms(@Param("roleId") Long roleId, @Param("permIds") List<Long> permIds);

    // ==================== 用户-角色查询 ====================

    /** 根据用户ID查询其角色key列表（用于登录时加载权限） */
    List<String> findRoleKeysByUserId(@Param("userId") Long userId);
}
