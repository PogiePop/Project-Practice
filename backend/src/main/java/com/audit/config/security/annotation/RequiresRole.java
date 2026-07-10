package com.audit.config.security.annotation;

import java.lang.annotation.*;

/**
 * 角色校验注解 — 标注在Controller方法上，校验当前用户是否拥有指定角色
 *
 * <pre>
 *   &#64;RequiresRole("super_admin")
 *   public Result<Void> manageUsers() { ... }
 * </pre>
 *
 * <p>由 {@link com.audit.config.security.aspect.PermissionAspect} 切面拦截校验</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {

    /** 角色标识，如 "super_admin" */
    String value();
}
