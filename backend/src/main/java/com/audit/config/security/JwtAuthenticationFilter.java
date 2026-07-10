package com.audit.config.security;

import com.audit.common.util.JwtUtil;
import com.audit.common.util.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT全局认证过滤器 — 拦截所有 /audit/v1/** 请求
 * <p>白名单放行登录接口，其余请求必须携带合法JWT</p>
 * <p>令牌合法后：1)构造UserContext 2)设置Spring Security SecurityContext</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 白名单路径（不需要Token） */
    private static final List<String> WHITE_LIST = List.of(
            "/audit/v1/login",
            "/audit/v1/auth/login",
            "/audit/v1/captcha/generate"
    );

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // 1. 白名单放行
        if (WHITE_LIST.stream().anyMatch(uri::equals)) {
            chain.doFilter(request, response);
            return;
        }

        // 2. 提取Token: Authorization: Bearer xxx
        String token = extractToken(request);
        if (token == null || token.isEmpty()) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "{\"code\":1001,\"message\":\"未登录，请先登录\",\"data\":null,\"timestamp\":" + System.currentTimeMillis() + "}");
            return;
        }

        // 3. 解析JWT
        Claims claims;
        try {
            claims = jwtUtil.parseToken(token);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "{\"code\":1002,\"message\":\"Token已过期，请重新登录\",\"data\":null,\"timestamp\":" + System.currentTimeMillis() + "}");
            return;
        } catch (Exception e) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "{\"code\":1003,\"message\":\"Token非法\",\"data\":null,\"timestamp\":" + System.currentTimeMillis() + "}");
            return;
        }

        // 4. 提取载荷数据
        String userId = claims.get("userId", String.class);
        String username = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        @SuppressWarnings("unchecked")
        List<String> perms = claims.get("perms", List.class);

        if (roles == null) roles = List.of();
        if (perms == null) perms = List.of();

        // 5. 构造UserContext（业务层使用）
        UserContext.set(userId, username, roles, perms);

        // 6. 设置Spring Security SecurityContext（SecurityFilterChain使用）
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList());
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authToken);

        try {
            chain.doFilter(request, response);
        } finally {
            // 7. 请求结束清除，防止内存泄漏
            SecurityContextHolder.clearContext();
            UserContext.clear();
        }
    }

    /** 从请求头提取Bearer Token */
    private String extractToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }

    /** 写入JSON响应 */
    private void writeJson(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(json);
    }
}
