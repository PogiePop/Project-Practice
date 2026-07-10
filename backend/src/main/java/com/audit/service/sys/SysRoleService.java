package com.audit.service.sys;

import com.audit.entity.sys.SysRole;
import java.util.List;

/**
 * 角色管理Service接口
 */
public interface SysRoleService {

    List<SysRole> listAll(String keyword);

    SysRole getById(Long roleId);

    SysRole create(SysRole role);

    SysRole update(SysRole role);

    void delete(Long roleId);

    /** 查询角色已分配的权限ID列表 */
    List<Long> getPermIds(Long roleId);

    /** 为角色分配权限（先删后插） */
    void assignPerms(Long roleId, List<Long> permIds);
}
