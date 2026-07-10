package com.audit.service.sys;

import com.audit.entity.sys.SysPermission;
import java.util.List;

/**
 * 权限管理Service接口
 */
public interface SysPermissionService {

    /** 全部权限列表 */
    List<SysPermission> listAll();

    /** 菜单权限树（目录+菜单，用于前端动态路由） */
    List<SysPermission> listMenus();

    /** 根据角色key列表查询菜单权限树 */
    List<SysPermission> listMenusByRoleKeys(List<String> roleKeys);
}
