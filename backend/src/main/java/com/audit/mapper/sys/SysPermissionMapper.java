package com.audit.mapper.sys;

import com.audit.entity.sys.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 权限Mapper — 权限树查询 + 全量权限
 */
@Mapper
public interface SysPermissionMapper {

    /** 全部权限列表（用于管理端展示） */
    List<SysPermission> findAll();

    /** 按类型查询（0目录/1菜单/2按钮/3接口） */
    List<SysPermission> findByType(@Param("type") Integer type);

    /** 根据角色key列表查询所有权限标识符（用于登录时加载） */
    List<String> findPermsByRoleKeys(@Param("roleKeys") List<String> roleKeys);

    /** 根据角色key列表查询所有菜单权限（用于前端动态路由） */
    List<SysPermission> findMenuByRoleKeys(@Param("roleKeys") List<String> roleKeys);
}
