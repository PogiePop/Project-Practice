# 审计信息管理系统 — Redis图形验证码 测试部署文档

> **文档版本**: V1.0  
> **创建日期**: 2026-07-09

---

## 一、Redis环境准备

### 1.1 Windows安装Redis

```bash
# 方式1: 使用winget
winget install Redis.Redis

# 方式2: 下载msi安装包
# https://github.com/tporadowski/redis/releases

# 启动Redis（默认端口6379，无密码）
redis-server
```

### 1.2 验证Redis可用

```bash
redis-cli ping
# 预期: PONG
```

---

## 二、后端启动

### 2.1 确认配置

`application.yml` 中Redis连接参数：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:           # 本地开发通常留空
      database: 0
```

### 2.2 启动

```bash
cd backend
mvn clean package -DskipTests
java -jar target/audit-system-1.0.0.jar
```

启动日志应包含 `RedisConnectionFactory` 相关输出，无连接异常。

---

## 三、接口测试

### 3.1 获取验证码

```bash
curl -s "http://localhost:8080/audit/v1/captcha/generate?uuid=test-001" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['captchaImage'][:80]+'...')"
```

预期返回：
```json
{"code":200, "data": {"captchaImage": "data:image/png;base64,iVBOR..."}}
```

### 3.2 查看Redis中的验证码

```bash
redis-cli get "captcha:test-001"
# 预期返回4位大写字符串，如 "AB3K"
```

### 3.3 登录测试（正确验证码）

```bash
# 先用步骤3.2获取Redis中的验证码值
CAPTCHA=$(redis-cli get "captcha:test-001")
echo "验证码: $CAPTCHA"

curl -s -X POST http://localhost:8080/audit/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"123456\",\"captchaUuid\":\"test-001\",\"captchaCode\":\"$CAPTCHA\"}" | python3 -m json.tool
```

预期返回200 + token + userInfo + roles + perms。

### 3.4 登录测试（错误验证码）

```bash
curl -s -X POST http://localhost:8080/audit/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456","captchaUuid":"test-002","captchaCode":"XXXX"}'
```

预期：
```json
{"code":1007, "message":"验证码输入错误"}
```

### 3.5 登录测试（验证码已过期/不存在）

```bash
curl -s -X POST http://localhost:8080/audit/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456","captchaUuid":"nonexistent","captchaCode":"ABCD"}'
```

预期：
```json
{"code":1006, "message":"验证码不存在或已过期"}
```

### 3.6 验证码一次性消费验证

```bash
# 1. 生成验证码
curl -s "http://localhost:8080/audit/v1/captcha/generate?uuid=disposable-01"

# 2. 第一次登录（成功，验证码被删除）
CAPTCHA=$(redis-cli get "captcha:disposable-01")
curl -s -X POST http://localhost:8080/audit/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"123456\",\"captchaUuid\":\"disposable-01\",\"captchaCode\":\"$CAPTCHA\"}"
# → 200 成功

# 3. 第二次使用同一验证码（应失败）
curl -s -X POST http://localhost:8080/audit/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"123456\",\"captchaUuid\":\"disposable-01\",\"captchaCode\":\"$CAPTCHA\"}"
# → {"code":1006, "message":"验证码不存在或已过期"}
```

---

## 四、前端复测步骤

### 4.1 正常登录流程

1. 启动前端 `cd front && npm run dev`
2. 访问 `http://localhost:5173/login`
3. 确认页面显示验证码图片
4. 输入用户名 `admin`、密码 `123456`、验证码
5. 点击登录 → 成功跳转首页

### 4.2 验证码刷新测试

1. 在登录页点击验证码图片
2. 验证码图片刷新（显示新验证码）
3. 确认旧UUID对应Redis key仍有效（未过期）

### 4.3 错误验证码测试

1. 输入任意错误验证码
2. 点击登录 → 提示"验证码输入错误"
3. 验证码自动刷新，输入框清空

### 4.4 验证码过期测试（2分钟）

1. 等待2分钟后再点击登录
2. → 提示"验证码不存在或已过期"

---

## 五、文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/pom.xml` | 修改 | +spring-boot-starter-data-redis, +commons-pool2 |
| `backend/.../application.yml` | 修改 | +spring.data.redis节点 |
| `backend/.../common/ResultCode.java` | 修改 | +CAPTCHA_EXPIRED(1006), +CAPTCHA_ERROR(1007) |
| `backend/.../config/RedisConfig.java` | **新增** | RedisTemplate序列化配置 |
| `backend/.../common/util/RedisUtil.java` | **新增** | Redis字符串操作工具 |
| `backend/.../common/util/CaptchaUtil.java` | **新增** | 验证码文本+图片生成 |
| `backend/.../controller/CaptchaController.java` | **新增** | /captcha/generate接口 |
| `backend/.../controller/AuthController.java` | 修改 | login()新增验证码校验 |
| `backend/.../security/JwtAuthenticationFilter.java` | 修改 | 白名单+/captcha/generate |
| `backend/.../security/SecurityConfig.java` | 修改 | permitAll+/captcha/generate |
| `front/src/api/modules/common.js` | 修改 | +getCaptcha() |
| `front/src/views/login/Login.vue` | 修改 | +验证码输入框+图片+刷新逻辑 |
| `docs/redis验证码需求规格.md` | **新增** | 阶段1文档 |
| `docs/redis验证码系统设计.md` | **新增** | 阶段2文档 |
| `docs/redis验证码测试部署.md` | **新增** | 阶段4文档（本文档） |

---

## 六、常见问题

| 问题 | 解决方案 |
|------|---------|
| Redis连接失败 | 确认 `redis-server` 已启动，`redis-cli ping` 返回PONG |
| 验证码图片不显示 | 检查浏览器控制台 `/captcha/generate` 请求是否200 |
| 登录仍不带验证码 | 确认前端已重启 `npm run dev` |
| Redis key包含乱码前缀 | 确认 `RedisConfig` 中用了 `StringRedisSerializer` |
