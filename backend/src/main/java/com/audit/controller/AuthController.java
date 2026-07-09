package com.audit.controller;

import com.audit.common.Result;
import com.audit.mapper.UserMapper;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/audit/v1/auth")
public class AuthController {

    private final UserMapper userMapper;

    public AuthController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        Map<String, Object> user = userMapper.login(username, password);
        if (user != null) {
            user.remove("password");
            return Result.ok(Map.of("token", "jwt-token-" + username, "userInfo", user));
        }
        return Result.fail(401, "用户名或密码错误");
    }

    @GetMapping("/userinfo")
    public Result<Map<String, Object>> userinfo(@RequestHeader(value = "Authorization", defaultValue = "") String auth) {
        String username = "admin";
        if (auth.contains("jwt-token-")) {
            username = auth.replace("Bearer jwt-token-", "").trim();
        }
        Map<String, Object> user = userMapper.findByUsername(username);
        if (user != null) return Result.ok(user);
        return Result.ok(userMapper.findByUsername("admin"));
    }

    @PutMapping("/userinfo")
    public Result<Void> updateUserinfo(@RequestBody Map<String, String> body) {
        String username = "admin";
        userMapper.update(username,
                body.get("realName"), body.get("phone"), body.get("email"),
                body.get("department"), body.get("position"));
        return Result.ok();
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        String username = "admin";
        int rows = userMapper.changePassword(username, body.get("oldPassword"), body.get("newPassword"));
        if (rows > 0) return Result.ok();
        return Result.fail(400, "当前密码错误");
    }

    @PostMapping("/logout")
    public Result<Void> logout() { return Result.ok(); }
}
