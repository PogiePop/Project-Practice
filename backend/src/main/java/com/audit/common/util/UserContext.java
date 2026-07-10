package com.audit.common.util;

import java.util.Collections;
import java.util.List;

/**
 * 用户上下文 — 基于ThreadLocal的当前请求用户信息持有者
 * <p>由JwtAuthenticationFilter在请求进入时设置，请求结束时清除</p>
 * <p>全局任意位置通过静态方法获取当前登录用户、角色、权限</p>
 *
 * <pre>
 *   String username = UserContext.getUsername();
 *   List&lt;String&gt; perms = UserContext.getPerms();
 * </pre>
 */
public final class UserContext {

    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    private UserContext() {}

    // ==================== 设置（仅供Filter调用） ====================

    /** 设置当前请求的用户信息 */
    public static void set(String userId, String username, List<String> roles, List<String> perms) {
        HOLDER.set(new Context(userId, username, roles, perms));
    }

    /** 清除（请求结束时调用，防止内存泄漏） */
    public static void clear() {
        HOLDER.remove();
    }

    // ==================== 读取（全局任意位置） ====================

    public static String getUserId() {
        Context ctx = HOLDER.get();
        return ctx != null ? ctx.userId : null;
    }

    public static String getUsername() {
        Context ctx = HOLDER.get();
        return ctx != null ? ctx.username : null;
    }

    public static List<String> getRoles() {
        Context ctx = HOLDER.get();
        return ctx != null ? ctx.roles : Collections.emptyList();
    }

    public static List<String> getPerms() {
        Context ctx = HOLDER.get();
        return ctx != null ? ctx.perms : Collections.emptyList();
    }

    /** 是否已登录 */
    public static boolean isLogin() {
        return HOLDER.get() != null;
    }

    /** 是否拥有指定角色 */
    public static boolean hasRole(String roleKey) {
        return getRoles().contains(roleKey);
    }

    /** 是否拥有指定权限（超管通配符*拥有全部权限） */
    public static boolean hasPerm(String perm) {
        List<String> perms = getPerms();
        return perms.contains("*") || perms.contains(perm);
    }

    // ==================== 内部数据类 ====================

    private record Context(String userId, String username, List<String> roles, List<String> perms) {}
}
