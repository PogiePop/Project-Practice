package com.audit.service.sys.impl;

import com.audit.entity.sys.SysPermission;
import com.audit.mapper.sys.SysPermissionMapper;
import com.audit.service.sys.SysPermissionService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 权限管理Service实现
 */
@Service
public class SysPermissionServiceImpl implements SysPermissionService {

    private final SysPermissionMapper sysPermissionMapper;

    public SysPermissionServiceImpl(SysPermissionMapper sysPermissionMapper) {
        this.sysPermissionMapper = sysPermissionMapper;
    }

    @Override
    public List<SysPermission> listAll() {
        return sysPermissionMapper.findAll();
    }

    @Override
    public List<SysPermission> listMenus() {
        // type=0目录, type=1菜单
        return sysPermissionMapper.findByType(1);
    }

    @Override
    public List<SysPermission> listMenusByRoleKeys(List<String> roleKeys) {
        if (roleKeys == null || roleKeys.isEmpty()) {
            return Collections.emptyList();
        }
        return sysPermissionMapper.findMenuByRoleKeys(roleKeys);
    }
}
