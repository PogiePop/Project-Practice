package com.audit.controller;

import com.audit.common.Result;
import com.audit.common.util.JwtUtil;
import com.audit.common.util.RedisUtil;
import com.audit.common.util.UserContext;
import com.audit.mapper.UserMapper;
import com.audit.mapper.sys.SysPermissionMapper;
import com.audit.mapper.sys.SysRoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 认证控制器 — 登录、用户信息、修改密码、退出登录
 * <p>POST /audit/v1/auth/login - 验证码校验 + JWT+BCrypt登录</p>
 */
@RestController
@RequestMapping("/audit/v1/auth")
public class AuthController {

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";

    private final UserMapper userMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    public AuthController(UserMapper userMapper,
                          SysRoleMapper sysRoleMapper,
                          SysPermissionMapper sysPermissionMapper,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          RedisUtil redisUtil) {
        this.userMapper = userMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisUtil = redisUtil;
    }

    // ==================== 登录 ====================

    /**
     * 用户登录（验证码 + JWT + BCrypt + RBAC权限加载）
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        // === 0. 验证码校验（新增，前置拦截） ===
        String captchaUuid = body.get("captchaUuid");
        String captchaCode = body.get("captchaCode");
        if (captchaUuid == null || captchaUuid.isEmpty() || captchaCode == null || captchaCode.isEmpty()) {
            return Result.fail(1006, "验证码不能为空");
        }
        String cachedCode = redisUtil.get(CAPTCHA_KEY_PREFIX + captchaUuid);
        if (cachedCode == null) {
            return Result.fail(1006, "验证码不存在或已过期");
        }
        if (!cachedCode.equalsIgnoreCase(captchaCode)) {
            return Result.fail(1007, "验证码输入错误");
        }
        // 验证码一次性消费，校验通过立刻删除
        redisUtil.del(CAPTCHA_KEY_PREFIX + captchaUuid);

        String username = body.get("username");
        String password = body.get("password");

        // 1. 查询用户（含密码）
        Map<String, Object> user = userMapper.findByUsernameWithPassword(username);
        if (user == null) {
            return Result.loginFailed();
        }

        String storedPassword = (String) user.get("password");
        Object idObj = user.get("id");
        Long userId = idObj instanceof Long ? (Long) idObj : Long.valueOf(idObj.toString());

        // 2. 密码校验（兼容明文 → 自动升级BCrypt）
        boolean passwordOk;
        if (storedPassword != null && storedPassword.startsWith("$2a$")) {
            passwordOk = passwordEncoder.matches(password, storedPassword);
        } else {
            passwordOk = password.equals(storedPassword);
            if (passwordOk) {
                // 自动升级明文密码为BCrypt
                userMapper.updatePassword(userId, passwordEncoder.encode(password));
            }
        }

        if (!passwordOk) {
            return Result.loginFailed();
        }

        // 3. 查询角色
        List<String> roles = sysRoleMapper.findRoleKeysByUserId(userId);
        if (roles == null) roles = List.of();

        // 4. 查询权限标识符（超管通配符*）
        List<String> perms;
        if (roles.contains("super_admin")) {
            perms = List.of("*");
        } else {
            perms = sysPermissionMapper.findPermsByRoleKeys(roles);
            if (perms == null) perms = List.of();
        }

        // 5. 签发JWT
        String token = jwtUtil.createToken(userId.toString(), username, roles, perms);

        // 6. 构造返回（不含密码）
        user.remove("password");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("userInfo", user);
        result.put("roles", roles);
        result.put("perms", perms);

        return Result.ok(result);
    }

    // ==================== 用户信息 ====================

    @GetMapping("/userinfo")
    public Result<Map<String, Object>> userinfo() {
        String username = UserContext.getUsername();
        if (username == null) {
            return Result.notLogin();
        }
        Map<String, Object> user = userMapper.findByUsername(username);
        if (user != null) return Result.ok(user);
        return Result.fail(404, "用户不存在");
    }

    @PutMapping("/userinfo")
    public Result<Void> updateUserinfo(@RequestBody Map<String, String> body) {
        String username = UserContext.getUsername();
        if (username == null) return Result.notLogin();
        userMapper.update(username,
                body.get("realName"), body.get("phone"), body.get("email"),
                body.get("department"), body.get("position"));
        return Result.ok();
    }

    // ==================== 修改密码 ====================

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        String username = UserContext.getUsername();
        if (username == null) return Result.notLogin();

        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        Map<String, Object> userWithPwd = userMapper.findByUsernameWithPassword(username);
        if (userWithPwd == null) return Result.fail(404, "用户不存在");

        String storedPassword = (String) userWithPwd.get("password");

        boolean oldPwdOk;
        if (storedPassword != null && storedPassword.startsWith("$2a$")) {
            oldPwdOk = passwordEncoder.matches(oldPassword, storedPassword);
        } else {
            oldPwdOk = oldPassword.equals(storedPassword);
        }

        if (!oldPwdOk) return Result.fail(400, "当前密码错误");

        Object idObj = userWithPwd.get("id");
        Long userId = idObj instanceof Long ? (Long) idObj : Long.valueOf(idObj.toString());
        userMapper.updatePassword(userId, passwordEncoder.encode(newPassword));

        return Result.ok();
    }

    // ==================== 退出登录 ====================

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }
}
