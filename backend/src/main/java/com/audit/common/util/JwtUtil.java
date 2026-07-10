package com.audit.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT令牌工具类 — 签发、解析、校验
 * <p>使用JJWT 0.12.x API，HMAC-SHA256签名</p>
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expiration;

    /**
     * @param secret     application.yml中jwt.secret配置的Base64密钥
     * @param expiration application.yml中jwt.expiration（秒）
     */
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        // 确保密钥≥256位（32字节），不足则用HMAC-SHA256的最低要求
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // 不足32字节时用SHA-256散列扩展（生产环境建议直接配置≥32字符密钥）
            try {
                keyBytes = java.security.MessageDigest.getInstance("SHA-256").digest(keyBytes);
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new RuntimeException("SHA-256算法不可用", e);
            }
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expiration = expiration;
    }

    // ==================== 令牌签发 ====================

    /**
     * 创建JWT令牌
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param roles    角色标识集合
     * @param perms    权限标识符集合
     * @return JWT字符串（Header.Payload.Signature）
     */
    public String createToken(String userId, String username, List<String> roles, List<String> perms) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)                       // sub: 用户名
                .claim("userId", userId)                 // 用户ID
                .claim("roles", roles)                   // 角色列表
                .claim("perms", perms)                   // 权限标识列表
                .issuedAt(new Date(now))                 // iat: 签发时间
                .expiration(new Date(now + expiration * 1000)) // exp: 过期时间
                .signWith(secretKey)                     // HMAC-SHA256签名
                .compact();
    }

    // ==================== 令牌解析 ====================

    /**
     * 解析JWT令牌，返回Claims
     *
     * @param token JWT字符串
     * @return Claims对象
     * @throws JwtException 令牌非法/过期时抛出
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ==================== 令牌校验 ====================

    /**
     * 校验令牌是否合法（不抛出异常即合法）
     *
     * @param token JWT字符串
     * @return true=合法, false=非法/过期
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            return false; // 过期
        } catch (JwtException e) {
            return false; // 非法
        }
    }

    /**
     * 判断是否为Token过期异常
     */
    public boolean isExpired(String token) {
        try {
            parseToken(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * 不抛出异常地解析（过期/非法返回null）
     */
    public Claims parseTokenQuietly(String token) {
        try {
            return parseToken(token);
        } catch (JwtException e) {
            return null;
        }
    }
}
