package com.audit.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.audit.common.util.JwtUtil;

/**
 * Spring Security 6.x 安全配置（使用新版SecurityFilterChain，无废弃API）
 * <p>核心策略：无状态会话 + JWT过滤器前置 + BCrypt密码编码</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    public SecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Security过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（前后端分离 + JWT无状态）
            .csrf(csrf -> csrf.disable())
            // 无状态会话（不创建HttpSession）
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 请求授权规则
            .authorizeHttpRequests(auth -> auth
                // 登录接口 + 验证码接口 允许匿名访问
                .requestMatchers("/audit/v1/login", "/audit/v1/auth/login", "/audit/v1/captcha/generate").permitAll()
                // 其余所有 /audit/v1/** 需要认证
                .requestMatchers("/audit/v1/**").authenticated()
                // 其他路径放行（静态资源等）
                .anyRequest().permitAll()
            )
            // 在UsernamePasswordAuthenticationFilter之前插入JWT过滤器
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt密码编码器（强度10）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
