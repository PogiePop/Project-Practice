package com.audit.service.sys.impl;

import com.audit.entity.sys.SysRole;
import com.audit.mapper.sys.SysRoleMapper;
import com.audit.service.sys.SysRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色管理Service实现
 */
@Service
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;

    public SysRoleServiceImpl(SysRoleMapper sysRoleMapper) {
        this.sysRoleMapper = sysRoleMapper;
    }

    @Override
    public List<SysRole> listAll(String keyword) {
        return sysRoleMapper.findAll(keyword);
    }

    @Override
    public SysRole getById(Long roleId) {
        return sysRoleMapper.findById(roleId);
    }

    @Override
    public SysRole create(SysRole role) {
        sysRoleMapper.insert(role);
        return role;
    }

    @Override
    public SysRole update(SysRole role) {
        sysRoleMapper.update(role);
        return sysRoleMapper.findById(role.getRoleId());
    }

    @Override
    public void delete(Long roleId) {
        // 先删除角色-权限关联
        sysRoleMapper.deleteRolePerms(roleId);
        sysRoleMapper.delete(roleId);
    }

    @Override
    public List<Long> getPermIds(Long roleId) {
        return sysRoleMapper.findPermIdsByRoleId(roleId);
    }

    @Override
    @Transactional
    public void assignPerms(Long roleId, List<Long> permIds) {
        // 先删后插，保证幂等
        sysRoleMapper.deleteRolePerms(roleId);
        if (permIds != null && !permIds.isEmpty()) {
            sysRoleMapper.insertRolePerms(roleId, permIds);
        }
    }
}
