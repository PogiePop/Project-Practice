package com.audit.config.security;

import java.util.Collection;
import java.util.List;

/**
 * 用户详情封装 — 供Spring Security使用
 * <p>存储当前登录用户的认证与授权信息</p>
 */
public class CustomUserDetails implements org.springframework.security.core.userdetails.UserDetails {

    private final String userId;
    private final String username;
    private final String password;
    private final List<String> roles;
    private final List<String> perms;

    public CustomUserDetails(String userId, String username, String password,
                             List<String> roles, List<String> perms) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.roles = roles;
        this.perms = perms;
    }

    public String getUserId() { return userId; }
    public List<String> getRoles() { return roles; }
    public List<String> getPerms() { return perms; }

    @Override
    public Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(r -> (org.springframework.security.core.GrantedAuthority) () -> "ROLE_" + r)
                .toList();
    }

    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
