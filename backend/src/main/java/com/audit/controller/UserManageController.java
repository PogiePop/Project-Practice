package com.audit.controller;

import com.audit.common.Result;
import com.audit.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/audit/v1/admin/users")
public class UserManageController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserManageController(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(Map.of("list", userMapper.findAll(keyword)));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        if (userMapper.findByUsername(username) != null) {
            return Result.fail(400, "用户名已存在");
        }
        // 密码BCrypt加密存储
        String rawPassword = body.getOrDefault("password", "123456");
        String encoded = passwordEncoder.encode(rawPassword);
        userMapper.insertUser(username, encoded,
                body.getOrDefault("realName", ""), body.getOrDefault("staffId", ""),
                body.getOrDefault("department", ""), body.getOrDefault("position", ""),
                body.getOrDefault("phone", ""), body.getOrDefault("email", ""),
                1); // 新建默认为审计员
        return Result.ok(Map.of("username", username));
    }

    @PutMapping("/{username}")
    public Result<Void> update(@PathVariable String username, @RequestBody Map<String, String> body) {
        userMapper.updateUser(username,
                body.get("realName"), body.get("staffId"), body.get("department"),
                body.get("position"), body.get("phone"), body.get("email"),
                body.get("roleLevel") != null ? Integer.parseInt(body.get("roleLevel")) : null);
        return Result.ok();
    }

    @DeleteMapping("/{username}")
    public Result<Void> delete(@PathVariable String username) {
        // 不允许删除自己
        if ("admin".equals(username)) return Result.fail(400, "不能删除超级管理员");
        userMapper.deleteUser(username);
        return Result.ok();
    }

    @PutMapping("/{username}/password")
    public Result<Void> resetPassword(@PathVariable String username, @RequestBody Map<String, String> body) {
        String rawPassword = body.getOrDefault("password", "123456");
        String encoded = passwordEncoder.encode(rawPassword);
        userMapper.resetPassword(username, encoded);
        return Result.ok();
    }
}
