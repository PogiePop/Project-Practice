package com.audit.config.security.aspect;

import com.audit.common.Result;
import com.audit.common.ResultCode;
import com.audit.common.util.UserContext;
import com.audit.config.security.annotation.RequiresPerm;
import com.audit.config.security.annotation.RequiresRole;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 权限校验切面 — 拦截 @RequiresPerm 和 @RequiresRole 注解
 * <p>从 UserContext 取出当前登录用户的权限/角色进行匹配</p>
 * <p>不匹配时直接返回Result错误响应，不执行业务方法</p>
 */
@Aspect
@Component
public class PermissionAspect {

    /**
     * 拦截标注了权限/角色注解的方法
     */
    @Around("@annotation(com.audit.config.security.annotation.RequiresPerm) || " +
            "@annotation(com.audit.config.security.annotation.RequiresRole)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 1. 检查 @RequiresRole
        RequiresRole requiresRole = method.getAnnotation(RequiresRole.class);
        if (requiresRole != null) {
            if (!UserContext.hasRole(requiresRole.value())) {
                return Result.fail(ResultCode.ROLE_DENIED, "需要角色: " + requiresRole.value());
            }
        }

        // 2. 检查 @RequiresPerm
        RequiresPerm requiresPerm = method.getAnnotation(RequiresPerm.class);
        if (requiresPerm != null) {
            if (!UserContext.hasPerm(requiresPerm.value())) {
                return Result.fail(ResultCode.PERM_DENIED, "需要权限: " + requiresPerm.value());
            }
        }

        // 3. 全部通过 → 执行业务方法
        return joinPoint.proceed();
    }
}
