package com.audit.controller.sys;

import com.audit.common.Result;
import com.audit.common.util.UserContext;
import com.audit.config.security.annotation.RequiresPerm;
import com.audit.entity.sys.SysPermission;
import com.audit.mapper.sys.SysPermissionMapper;
import com.audit.mapper.sys.SysUserRoleMapper;
import com.audit.service.sys.SysPermissionService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 系统权限管理控制器（超管专用）
 * <p>GET  /audit/v1/sys/permission       — 全部权限列表（管理端）</p>
 * <p>GET  /audit/v1/sys/permission/menu  — 当前用户菜单权限树（前端动态路由）</p>
 * <p>GET  /audit/v1/sys/user/{userId}/roles — 用户已有角色ID列表</p>
 * <p>PUT  /audit/v1/sys/user/{userId}/roles — 分配用户角色</p>
 */
@RestController
@RequestMapping("/audit/v1/sys")
public class SysPermissionController {

    private final SysPermissionService sysPermissionService;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    public SysPermissionController(SysPermissionService sysPermissionService,
                                   SysPermissionMapper sysPermissionMapper,
                                   SysUserRoleMapper sysUserRoleMapper) {
        this.sysPermissionService = sysPermissionService;
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
    }

    /** 全部权限列表（管理端展示） */
    @GetMapping("/permission")
    @RequiresPerm("audit:sys:perm")
    public Result<List<SysPermission>> listAll() {
        return Result.ok(sysPermissionService.listAll());
    }

    /** 当前用户菜单权限树（前端根据此数据动态生成路由和菜单） */
    @GetMapping("/permission/menu")
    public Result<List<SysPermission>> menuTree() {
        // 超管看全部菜单
        List<String> roles = UserContext.getRoles();
        if (roles.contains("super_admin")) {
            // 返回所有菜单（type=0目录 + type=1菜单）
            List<SysPermission> all = sysPermissionMapper.findByType(1);
            return Result.ok(all);
        }
        List<SysPermission> menus = sysPermissionService.listMenusByRoleKeys(roles);
        return Result.ok(menus);
    }

    /** 查询用户已有角色ID列表 */
    @GetMapping("/user/{userId}/roles")
    @RequiresPerm("audit:sys:user")
    public Result<List<Long>> userRoles(@PathVariable Long userId) {
        return Result.ok(sysUserRoleMapper.findRoleIdsByUserId(userId));
    }

    /** 分配用户角色（全量替换：先删后插） */
    @PutMapping("/user/{userId}/roles")
    @RequiresPerm("audit:sys:user")
    public Result<Void> assignUserRoles(@PathVariable Long userId, @RequestBody Map<String, List<Long>> body) {
        List<Long> roleIds = body.get("roleIds");
        sysUserRoleMapper.deleteByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            sysUserRoleMapper.insertBatch(userId, roleIds);
        }
        return Result.ok();
    }
}
