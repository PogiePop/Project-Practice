package com.audit.config.security.annotation;

import java.lang.annotation.*;

/**
 * 权限标识校验注解 — 标注在Controller方法上，校验当前用户是否拥有指定权限标识
 *
 * <pre>
 *   &#64;RequiresPerm("audit:plan:add")
 *   public Result<Void> addPlan() { ... }
 * </pre>
 *
 * <p>由 {@link com.audit.config.security.aspect.PermissionAspect} 切面拦截校验</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPerm {

    /** 权限标识符，如 "audit:plan:list" */
    String value();

    /** 是否逻辑与（预留）：多个权限时需要同时满足 */
    boolean logicalAnd() default true;
}
