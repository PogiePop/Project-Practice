package com.audit.controller.sys;

import com.audit.common.Result;
import com.audit.config.security.annotation.RequiresPerm;
import com.audit.entity.sys.SysRole;
import com.audit.service.sys.SysRoleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统角色管理控制器（超管专用）
 * <p>GET    /audit/v1/sys/role          — 角色列表</p>
 * <p>GET    /audit/v1/sys/role/{id}     — 角色详情</p>
 * <p>POST   /audit/v1/sys/role          — 新增角色</p>
 * <p>PUT    /audit/v1/sys/role/{id}     — 编辑角色</p>
 * <p>DELETE /audit/v1/sys/role/{id}     — 删除角色</p>
 * <p>GET    /audit/v1/sys/role/{id}/perms — 角色已有权限ID列表</p>
 * <p>PUT    /audit/v1/sys/role/{id}/perms — 分配角色权限</p>
 */
@RestController
@RequestMapping("/audit/v1/sys/role")
public class SysRoleController {

    private final SysRoleService sysRoleService;

    public SysRoleController(SysRoleService sysRoleService) {
        this.sysRoleService = sysRoleService;
    }

    /** 角色列表 */
    @GetMapping
    @RequiresPerm("audit:sys:role")
    public Result<List<SysRole>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(sysRoleService.listAll(keyword));
    }

    /** 角色详情 */
    @GetMapping("/{roleId}")
    @RequiresPerm("audit:sys:role")
    public Result<SysRole> detail(@PathVariable Long roleId) {
        SysRole role = sysRoleService.getById(roleId);
        if (role == null) return Result.fail(404, "角色不存在");
        return Result.ok(role);
    }

    /** 新增角色 */
    @PostMapping
    @RequiresPerm("audit:sys:role")
    public Result<SysRole> create(@RequestBody SysRole role) {
        return Result.ok(sysRoleService.create(role));
    }

    /** 编辑角色 */
    @PutMapping("/{roleId}")
    @RequiresPerm("audit:sys:role")
    public Result<SysRole> update(@PathVariable Long roleId, @RequestBody SysRole role) {
        role.setRoleId(roleId);
        return Result.ok(sysRoleService.update(role));
    }

    /** 删除角色 */
    @DeleteMapping("/{roleId}")
    @RequiresPerm("audit:sys:role")
    public Result<Void> delete(@PathVariable Long roleId) {
        sysRoleService.delete(roleId);
        return Result.ok();
    }

    /** 查询角色已有权限ID列表 */
    @GetMapping("/{roleId}/perms")
    @RequiresPerm("audit:sys:perm")
    public Result<List<Long>> getPerms(@PathVariable Long roleId) {
        return Result.ok(sysRoleService.getPermIds(roleId));
    }

    /** 分配角色权限 */
    @PutMapping("/{roleId}/perms")
    @RequiresPerm("audit:sys:perm")
    public Result<Void> assignPerms(@PathVariable Long roleId, @RequestBody Map<String, List<Long>> body) {
        List<Long> permIds = body.get("permIds");
        sysRoleService.assignPerms(roleId, permIds);
        return Result.ok();
    }
}
