# 审计信息管理系统 — JWT + RBAC 测试与部署文档

> **文档版本**: V1.0  
> **创建日期**: 2026-07-09  
> **关联文档**: [[jwt_rbac需求规格]], [[jwt_rbac系统设计]]

---

## 一、部署步骤

### 1.1 数据库初始化

```bash
# 步骤1: 如果已有数据库，先执行基础初始化
mysql -uroot -p < sql/init.sql

# 步骤2: 执行RBAC权限表初始化
mysql -uroot -p < sql/rbac_init.sql

# 步骤3: 验证
mysql -uroot -p project_practice -e "
  SELECT 'RBAC表创建完成' AS status;
  SELECT COUNT(*) AS 角色数 FROM sys_role;
  SELECT COUNT(*) AS 权限数 FROM sys_permission;
  SELECT COUNT(*) AS 角色权限关联 FROM sys_role_permission;
  SELECT COUNT(*) AS 用户角色关联 FROM sys_user_role;
"
```

### 1.2 后端启动

```bash
cd backend
mvn clean package -DskipTests
java -jar target/audit-system-1.0.0.jar
```

### 1.3 前端启动

```bash
cd front
npm install
npm run dev
```

---

## 二、Postman接口测试

### 2.1 登录测试

```
POST http://localhost:8080/audit/v1/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```

**预期响应 (200):**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInVzZXJJZCI6IjEiLCJyb2xlcyI6WyJzdXBlcl9hZG1pbiJdLCJwZXJtcyI6WyIqIl0sImlhdCI6MTc1MjAwMDAwMCwiZXhwIjoxNzUyMDA3MjAwfQ.xxx",
    "userInfo": {
      "id": 1,
      "username": "admin",
      "realName": "陈处长",
      "role": "DIRECTOR",
      "roleLevel": 0,
      ...
    },
    "roles": ["super_admin"],
    "perms": ["*"]
  },
  "timestamp": 1752000000000
}
```

### 2.2 无Token访问接口（返回1001）

```
GET http://localhost:8080/audit/v1/plan/batches
# 不携带 Authorization Header
```

**预期响应 (401):**
```json
{"code":1001, "message":"未登录，请先登录", "data":null, "timestamp":...}
```

### 2.3 过期Token访问（返回1002）

使用过期JWT访问任意业务接口，预期：
```json
{"code":1002, "message":"Token已过期，请重新登录", "data":null, "timestamp":...}
```

> 测试方法：修改`application.yml`中`jwt.expiration`为`1`（1秒），登录后等待2秒再访问接口。

### 2.4 伪造Token访问（返回1003）

```
GET http://localhost:8080/audit/v1/plan/batches
Authorization: Bearer fake-token-abc123
```

```json
{"code":1003, "message":"Token非法", "data":null, "timestamp":...}
```

### 2.5 角色管理（需admin登录）

```
# 获取角色列表
GET http://localhost:8080/audit/v1/sys/role
Authorization: Bearer {admin_token}

# 新增角色
POST http://localhost:8080/audit/v1/sys/role
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "roleName": "测试角色",
  "roleKey": "test_role",
  "status": 1,
  "remark": "测试用"
}

# 为角色分配权限
PUT http://localhost:8080/audit/v1/sys/role/5/perms
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "permIds": [2, 3, 10, 11, 15]
}
```

### 2.6 权限分配测试

```
# 为用户分配角色
PUT http://localhost:8080/audit/v1/sys/user/2/roles
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "roleIds": [3, 4]
}
```

### 2.7 无权限访问（返回1005）

用zhangsan（审计组长，无`audit:sys:role`权限）的Token访问角色管理：

```
GET http://localhost:8080/audit/v1/sys/role
Authorization: Bearer {zhangsan_token}
```

预期响应：
```json
{"code":1005, "message":"需要权限: audit:sys:role", "data":null, "timestamp":...}
```

### 2.8 获取当前用户菜单权限树

```
GET http://localhost:8080/audit/v1/sys/permission/menu
Authorization: Bearer {admin_token}
```

返回当前用户可见的菜单权限列表（type=0目录+type=1菜单）。

---

## 三、单元测试建议

### 3.1 JwtUtil测试

```java
@Test
void testCreateAndParseToken() {
    List<String> roles = List.of("super_admin");
    List<String> perms = List.of("*");
    String token = jwtUtil.createToken("1", "admin", roles, perms);
    
    assertNotNull(token);
    assertTrue(token.split("\\.").length == 3); // 三段式
    
    Claims claims = jwtUtil.parseToken(token);
    assertEquals("admin", claims.getSubject());
    assertEquals("1", claims.get("userId", String.class));
    assertEquals(List.of("super_admin"), claims.get("roles", List.class));
    assertEquals(List.of("*"), claims.get("perms", List.class));
}

@Test
void testExpiredToken() {
    // 设置expiration=1秒后测试
    String token = jwtUtil.createToken("1", "test", List.of(), List.of());
    Thread.sleep(2000);
    assertTrue(jwtUtil.isExpired(token));
}

@Test
void testInvalidToken() {
    assertFalse(jwtUtil.validateToken("invalid.token.here"));
}
```

### 3.2 PermissionAspect测试

```java
@Test
void testRequiresPermAnnotation() {
    // 模拟UserContext设置后调用标注了@RequiresPerm的方法
    UserContext.set("1", "test", List.of("auditor"), List.of("audit:plan:list"));
    
    // 有权限 → 正常执行
    // 无权限 → 返回Result(1005)
}
```

---

## 四、Vue3前端适配

### 4.1 Axios全局Token注入

在`front/src/api/request.js`中添加请求拦截器：

```javascript
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router/router'

const request = axios.create({
  baseURL: '/api/audit/v1',
  timeout: 15000
})

// ========== 请求拦截器：自动携带Token ==========
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
}, error => Promise.reject(error))

// ========== 响应拦截器：统一处理鉴权错误 ==========
request.interceptors.response.use(
  response => {
    const data = response.data
    if (data.code === 1001 || data.code === 1002 || data.code === 1003) {
      // Token失效 → 清除并跳转登录
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('perms')
      ElMessage.error(data.message || '登录已过期，请重新登录')
      router.push('/login')
      return Promise.reject(new Error(data.message))
    }
    if (data.code === 1005) {
      ElMessage.error('无此操作权限')
      return Promise.reject(new Error('无权限'))
    }
    return data
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.clear()
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default request
```

### 4.2 登录页面适配

```javascript
// Login.vue - 登录方法
const handleLogin = async () => {
  const res = await axios.post('/api/audit/v1/login', {
    username: form.username,
    password: form.password
  })
  if (res.data.code === 200) {
    const { token, userInfo, roles, perms } = res.data.data
    localStorage.setItem('token', token)
    localStorage.setItem('userInfo', JSON.stringify(userInfo))
    localStorage.setItem('roles', JSON.stringify(roles))
    localStorage.setItem('perms', JSON.stringify(perms))
    router.push('/')
  }
}
```

### 4.3 按钮权限控制指令

创建`front/src/utils/permission.js`：

```javascript
import { ref } from 'vue'

/**
 * 检查当前用户是否拥有指定权限
 * @param {string} perm 权限标识符，如 "audit:plan:add"
 * @returns {boolean}
 */
export function hasPerm(perm) {
  const perms = JSON.parse(localStorage.getItem('perms') || '[]')
  // 超管通配符拥有全部权限
  return perms.includes('*') || perms.includes(perm)
}

/**
 * 检查当前用户是否拥有指定角色
 * @param {string} role 角色标识，如 "super_admin"
 * @returns {boolean}
 */
export function hasRole(role) {
  const roles = JSON.parse(localStorage.getItem('roles') || '[]')
  return roles.includes(role)
}
```

Vue3组件中使用：

```vue
<template>
  <!-- 根据权限控制按钮显隐 -->
  <el-button v-if="hasPerm('audit:plan:add')" type="primary" @click="addPlan">
    新建计划
  </el-button>
  <el-button v-if="hasPerm('audit:plan:delete')" type="danger" @click="deletePlan">
    删除
  </el-button>
</template>

<script setup>
import { hasPerm, hasRole } from '@/utils/permission'
</script>
```

### 4.4 路由权限守卫

在`front/src/router/router.js`中添加：

```javascript
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  // 登录页：已登录则跳首页，未登录则放行
  if (to.path === '/login') {
    if (token) return next('/')
    return next()
  }

  // 其他页面：未登录跳登录页
  if (!token) {
    return next('/login')
  }

  // 超管专属路由检查
  if (to.meta.superAdmin) {
    const roles = JSON.parse(localStorage.getItem('roles') || '[]')
    if (!roles.includes('super_admin')) {
      return next('/plan/form')
    }
  }

  next()
})
```

---

## 五、生产环境安全配置

### 5.1 JWT密钥

生产环境**禁止**将`jwt.secret`明文写入`application.yml`，通过环境变量注入：

```bash
# Linux / macOS
export JWT_SECRET="$(openssl rand -base64 64)"
export JWT_EXPIRATION=7200

# 修改application.yml为占位符
# jwt:
#   secret: ${JWT_SECRET}
#   expiration: ${JWT_EXPIRATION:7200}
```

### 5.2 数据库密码

```bash
export SPRING_DATASOURCE_PASSWORD="your_secure_password"
```

### 5.3 权限数据初始化规范

1. 生产部署前在`sql/rbac_init.sql`中审查所有预设角色和权限
2. 根据实际组织架构调整角色定义（至少保留`super_admin`）
3. 执行`rbac_init.sql`后，通过管理界面而非SQL直接操作用户角色分配
4. 定期审计`sys_user_role`和`sys_role_permission`表的数据

### 5.4 HTTPS

生产环境启用HTTPS，在Spring Boot配置：

```yaml
server:
  port: 8443
  ssl:
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEY_PASSWORD}
    key-store-type: PKCS12
```

---

## 六、错误码速查表

| code | 说明 | 触发场景 |
|------|------|---------|
| 200 | 成功 | 正常响应 |
| 400 | 参数错误 | 请求参数校验失败 |
| 401 | 登录失败 | 用户名或密码错误 |
| 404 | 资源不存在 | 查询的数据不存在 |
| 500 | 服务器错误 | 未捕获的异常 |
| **1001** | 未登录 | 请求头无Authorization |
| **1002** | Token过期 | JWT超过有效期 |
| **1003** | Token非法 | 签名错误/格式错误/伪造 |
| **1004** | 角色不足 | @RequiresRole校验失败 |
| **1005** | 无权限 | @RequiresPerm校验失败 |

---

## 七、集成现有系统的检查清单

- [ ] 启动后端，确认无启动异常
- [ ] 用admin/123456登录，确认返回token+roles+perms
- [ ] 携带token访问`GET /audit/v1/plan/batches`，确认正常返回
- [ ] 访问`GET /audit/v1/plan/statistics/summary`，确认统计正常
- [ ] 访问`GET /audit/v1/objects/units`，确认单位列表正常
- [ ] 用zhangsan登录，确认权限正确（审计组长权限）
- [ ] 用lisi登录，确认只有查看权限
- [ ] 用lisi的token访问角色管理接口，确认返回1005
- [ ] 前端`npm run dev`启动，确认登录→业务页面完整流程
- [ ] 原有11个Controller的业务功能全部正常

---

## 八、文件变更清单

### 新增文件 (25个)

| 文件 | 说明 |
|------|------|
| `docs/jwt_rbac需求规格.md` | 阶段1：需求规格说明书 |
| `docs/jwt_rbac系统设计.md` | 阶段2：系统设计说明书 |
| `docs/jwt_rbac测试部署文档.md` | 阶段4：测试部署文档（本文档） |
| `sql/rbac_init.sql` | RBAC 4张表 + 种子数据 |
| `common/ResultCode.java` | 统一错误码常量 |
| `common/util/JwtUtil.java` | JWT签发/解析工具 |
| `common/util/UserContext.java` | ThreadLocal用户上下文 |
| `config/security/SecurityConfig.java` | Spring Security 6.x配置 |
| `config/security/JwtAuthenticationFilter.java` | JWT全局过滤器 |
| `config/security/CustomUserDetails.java` | UserDetails封装 |
| `config/security/annotation/RequiresPerm.java` | 权限校验注解 |
| `config/security/annotation/RequiresRole.java` | 角色校验注解 |
| `config/security/aspect/PermissionAspect.java` | 权限校验切面 |
| `entity/sys/SysUser.java` | 用户实体 |
| `entity/sys/SysRole.java` | 角色实体 |
| `entity/sys/SysPermission.java` | 权限实体 |
| `mapper/sys/SysRoleMapper.java` | 角色Mapper |
| `mapper/sys/SysPermissionMapper.java` | 权限Mapper |
| `mapper/sys/SysUserRoleMapper.java` | 用户-角色Mapper |
| `mapper/SysRoleMapper.xml` | 角色SQL映射 |
| `mapper/SysPermissionMapper.xml` | 权限SQL映射 |
| `mapper/SysUserRoleMapper.xml` | 用户-角色SQL映射 |
| `service/sys/SysRoleService.java` | 角色Service接口 |
| `service/sys/SysPermissionService.java` | 权限Service接口 |
| `service/sys/impl/SysRoleServiceImpl.java` | 角色Service实现 |
| `service/sys/impl/SysPermissionServiceImpl.java` | 权限Service实现 |
| `controller/LoginController.java` | JWT登录接口 |
| `controller/sys/SysRoleController.java` | 角色管理接口 |
| `controller/sys/SysPermissionController.java` | 权限管理接口 |

### 修改文件 (4个)

| 文件 | 改动 |
|------|------|
| `pom.xml` | 追加Spring Security、JJWT、AOP依赖 |
| `application.yml` | 新增jwt配置节点 |
| `common/Result.java` | 新增鉴权专用快捷方法(notLogin/tokenExpired等) |
| `mapper/UserMapper.java` | 新增findByUsernameWithPassword、findById、updatePassword方法 |
| `mapper/UserMapper.xml` | 相应SQL |
| `controller/AuthController.java` | 使用UserContext替代硬编码用户 |

### 未修改文件 (所有原有业务代码)

11个Controller、3个Service、11个Mapper（除UserMapper追加方法外）、所有Entity均未改动。

---

> **文档结束** — 如有问题请参考 `docs/jwt_rbac需求规格.md` 和 `docs/jwt_rbac系统设计.md`
