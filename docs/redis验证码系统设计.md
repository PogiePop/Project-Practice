# 审计信息管理系统 — Redis图形验证码 系统设计说明书

> **文档版本**: V1.0  
> **创建日期**: 2026-07-09

---

## 一、整体时序

```
前端                                后端                              Redis
 │                                   │                                 │
 │ 1. 页面加载，生成uuid              │                                 │
 │ 2. GET /captcha/generate?uuid=X   │                                 │
 │──────────────────────────────────►│                                 │
 │                                   │ 3. CaptchaUtil生成4位验证码      │
 │                                   │ 4. RedisUtil.setEx(captcha:X, code, 120s)
 │                                   │────────────────────────────────►│
 │                                   │                                 │
 │ 5. Base64 PNG图片                 │                                 │
 │◄──────────────────────────────────│                                 │
 │                                   │                                 │
 │ 6. 用户填写验证码+账号密码          │                                 │
 │ 7. POST /auth/login               │                                 │
 │    {captchaUuid, captchaCode, ...} │                                 │
 │──────────────────────────────────►│                                 │
 │                                   │ 8. get(captcha:X)              │
 │                                   │────────────────────────────────►│
 │                                   │ ◄──── code or null ────────────│
 │                                   │                                 │
 │                                   │ 9. null → 1006 验证码已失效     │
 │                                   │ 10. 不匹配 → 1007 验证码错误    │
 │                                   │ 11. 匹配 → del(captcha:X)      │
 │                                   │────────────────────────────────►│
 │                                   │ 12. 执行原有JWT登录逻辑         │
 │                                   │                                 │
 │ 13. token + 用户信息               │                                 │
 │◄──────────────────────────────────│                                 │
```

## 二、Redis键设计

| Key | Value | TTL | 操作 |
|-----|-------|-----|------|
| `captcha:{uuid}` | 4位验证码文本，大写 | 120s | generate→setEx, login→get→校验→del |

销毁时机：
1. 校验通过 → 立即 `del`
2. 120秒过期 → Redis 自动删除
3. 不区分大小写：value存大写，比对时输入也转大写

## 三、新增类设计

### 3.1 RedisConfig
- 路径: `com.audit.config`
- 职责: 配置RedisTemplate序列化（StringRedisSerializer），避免Key乱码

### 3.2 RedisUtil
- 路径: `com.audit.common.util`
- 方法: `set(key, value)` / `setEx(key, value, seconds)` / `get(key)` / `del(key)`

### 3.3 CaptchaUtil
- 路径: `com.audit.common.util`
- 方法: `generateCode()` 生成4位随机码 / `generateImage(code)` 生成BufferedImage含干扰线+噪点 → Base64

### 3.4 CaptchaController
- 路径: `com.audit.controller`
- `GET /audit/v1/captcha/generate?uuid=xxx` → 返回 `{code:200, data:{captchaImage:"data:image/png;base64,..."}}`

## 四、AuthController登录改造

在原有 `login()` 方法的 **第一步之前** 插入验证码校验：

```java
@PostMapping("/login")
public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
    // === 新增：验证码校验 ===
    String captchaUuid = body.get("captchaUuid");
    String captchaCode = body.get("captchaCode");
    if (captchaUuid == null || captchaCode == null) {
        return Result.fail(1006, "验证码不能为空");
    }
    String cachedCode = redisUtil.get("captcha:" + captchaUuid);
    if (cachedCode == null) {
        return Result.fail(1006, "验证码不存在或已过期");
    }
    if (!cachedCode.equalsIgnoreCase(captchaCode)) {
        return Result.fail(1007, "验证码输入错误");
    }
    redisUtil.del("captcha:" + captchaUuid); // 一次性销毁
    // === 原有逻辑不变 ===
    ...
}
```

注入新增依赖: `private final RedisUtil redisUtil;`

## 五、Maven新增依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

## 六、application.yml新增配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:           # 无密码则留空
      database: 0
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

## 七、前端Login.vue改造点

1. 新增 `captchaUuid` 状态（页面加载时随机生成）
2. 新增 `captchaCode` 输入框（el-input）
3. 新增验证码图片 `<img :src="captchaImage" @click="refreshCaptcha" />`
4. `refreshCaptcha()` 方法：重新生成uuid → 调 `GET /captcha/generate` → 更新captchaImage
5. `handleLogin()` 中携带 `captchaUuid`、`captchaCode`

## 八、白名单追加

JwtAuthenticationFilter + SecurityConfig 白名单新增：
- `/audit/v1/captcha/generate`

---

> 下一阶段：编码落地
