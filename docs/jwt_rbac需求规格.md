# 审计信息管理系统 — JWT鉴权 + RBAC权限控制 需求规格说明书

> **文档版本**: V1.0  
> **创建日期**: 2026-07-09  
> **关联文档**: [[jwt_rbac系统设计]], [[jwt_rbac测试部署文档]]

---

## 一、引言

### 1.1 背景

当前审计信息管理系统存在以下安全缺陷：

| 问题 | 现状 | 风险等级 |
|------|------|---------|
| 密码明文存储 | `sys_user`表`password`字段存明文，登录SQL直接比对 | 严重 |
| 伪Token | 登录返回`"jwt-token-admin"`字符串拼接，无签名验证 | 严重 |
| 无全局鉴权 | `AuthController.userinfo()`硬编码`username="admin"` | 严重 |
| 无权限控制 | 所有接口无访问控制，任意登录用户可操作全部功能 | 高 |
| 无角色体系 | 仅`sys_user.role`字段存角色名，无法细粒度控权 | 高 |

### 1.2 目标

1. 实现JWT标准令牌认证，替换伪Token
2. 用户密码BCrypt加密存储与校验
3. 全局过滤器拦截所有`/audit/v1/**`请求，白名单放行登录接口
4. 建立用户-角色-权限三级RBAC模型，实现接口级、按钮级细粒度权限控制
5. 不修改原有审计计划、审批、单位库等11个Controller的业务代码

---

## 二、功能需求

### 2.1 基础JWT认证

| 编号 | 需求描述 | 优先级 |
|------|---------|--------|
| AUTH-01 | 使用JJWT库实现Token签发与解析，支持HMAC-SHA256签名 | P0 |
| AUTH-02 | 登录接口`POST /audit/v1/login`校验BCrypt密码，签发JWT | P0 |
| AUTH-03 | JWT载荷存储：userId、username、角色标识集合、权限标识符集合 | P0 |
| AUTH-04 | Token默认有效期2小时，通过`application.yml`可配置 | P0 |
| AUTH-05 | 请求头规范：`Authorization: Bearer {jwt_token}` | P0 |
| AUTH-06 | 全局`OncePerRequestFilter`拦截所有`/audit/v1/**`请求 | P0 |
| AUTH-07 | 白名单：`/audit/v1/login`放行（不拦截） | P0 |
| AUTH-08 | `ThreadLocal`封装`UserContext`，全局获取当前用户、角色、权限 | P1 |
| AUTH-09 | SpringBoot 3.x `SecurityFilterChain`配置，不使用废弃的`WebSecurityConfigurerAdapter` | P0 |

### 2.2 RBAC细粒度权限控制

| 编号 | 需求描述 | 优先级 |
|------|---------|--------|
| RBAC-01 | 建立用户-角色-权限三级模型，多对多关联 | P0 |
| RBAC-02 | 自定义注解`@RequiresPerm("audit:plan:list")`实现接口级权限校验 | P0 |
| RBAC-03 | 自定义注解`@RequiresRole("super_admin")`实现角色级校验 | P1 |
| RBAC-04 | AOP切面拦截上述注解，从`UserContext`取权限/角色匹配 | P0 |
| RBAC-05 | 权限标识符统一规范：`模块:功能:操作`（如`audit:plan:add`） | P0 |
| RBAC-06 | 用户登录后根据userId→角色→权限链路加载完整权限标识集合 | P0 |
| RBAC-07 | 权限分为三类：菜单权限、按钮权限、接口权限 | P1 |
| RBAC-08 | 提供角色CRUD接口（超管专用） | P0 |
| RBAC-09 | 提供权限查询/分配接口（超管专用） | P0 |
| RBAC-10 | 提供用户-角色绑定/解绑接口（超管专用） | P0 |

### 2.3 统一鉴权错误码

| 编号 | 错误码 | 说明 |
|------|--------|------|
| ERR-01 | 1001 | 未登录（Token缺失） |
| ERR-02 | 1002 | Token已过期 |
| ERR-03 | 1003 | Token非法（签名/格式错误） |
| ERR-04 | 1004 | 角色不足（无所需角色） |
| ERR-05 | 1005 | 无接口访问权限（无所需权限标识） |
| ERR-06 | 401 | 用户名或密码错误（登录） |

---

## 三、数据库表需求

### 3.1 新增表

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| `sys_role` | 角色表 | role_id, role_name, role_key, status, remark |
| `sys_user_role` | 用户-角色关联 | user_id, role_id（联合主键） |
| `sys_permission` | 权限表 | perm_id, parent_id, perm_name, perms(标识符), type(菜单/按钮/接口), path, sort_order |
| `sys_role_permission` | 角色-权限关联 | role_id, perm_id（联合主键） |

### 3.2 原有表适配

| 表名 | 改动 | 说明 |
|------|------|------|
| `sys_user` | 新增字段或通过关联表 | 不修改原有表结构，通过`sys_user_role`中间表关联角色 |
| `sys_user` | `password`字段 | 存储BCrypt密文，历史种子数据需更新 |

---

## 四、接口清单

### 4.1 认证接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/audit/v1/login` | 用户登录，返回JWT+用户信息+权限列表+角色列表 | 白名单 |

### 4.2 角色管理（超管）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/audit/v1/sys/role` | 角色列表（分页） |
| GET | `/audit/v1/sys/role/{roleId}` | 角色详情 |
| POST | `/audit/v1/sys/role` | 新增角色 |
| PUT | `/audit/v1/sys/role/{roleId}` | 编辑角色 |
| DELETE | `/audit/v1/sys/role/{roleId}` | 删除角色 |
| GET | `/audit/v1/sys/role/{roleId}/perms` | 查询角色已有权限 |
| PUT | `/audit/v1/sys/role/{roleId}/perms` | 分配角色权限 |

### 4.3 权限管理（超管）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/audit/v1/sys/permission` | 权限树（全部权限） |
| GET | `/audit/v1/sys/permission/menu` | 菜单权限树（前端动态路由用） |

### 4.4 用户-角色绑定（超管）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/audit/v1/sys/user/{userId}/roles` | 查询用户已有角色 |
| PUT | `/audit/v1/sys/user/{userId}/roles` | 分配用户角色 |

---

## 五、安全非功能需求

| 编号 | 需求 | 说明 |
|------|------|------|
| SEC-01 | 密码BCrypt加密 | 强度因子10，存储60字符密文 |
| SEC-02 | JWT签名密钥外置 | `application.yml`可配置，生产环境通过环境变量覆盖 |
| SEC-03 | Token过期自动失效 | 前端拦截401/1002，跳转登录页 |
| SEC-04 | 权限缓存 | 登录时一次性加载权限，会话期间不变 |
| SEC-05 | 密码不可逆 | 修改密码时BCrypt匹配旧密码，新密码加密存入 |

---

## 六、验收标准

### 6.1 基础认证验收

- [ ] 访问任意`/audit/v1/**`接口无Token返回`{code:1001, message:"未登录"}`
- [ ] 登录成功返回JWT（Header.Payload.Signature三段式）和用户信息
- [ ] JWT载荷中包含userId、username、roles数组、perms数组
- [ ] 过期Token访问返回`{code:1002, message:"Token已过期"}`
- [ ] 伪造Token访问返回`{code:1003, message:"Token非法"}`
- [ ] 通过`UserContext`可在任意Service层获取当前用户信息

### 6.2 RBAC验收

- [ ] 管理员可创建角色、给角色分配权限标识
- [ ] 管理员可将角色分配给用户
- [ ] 方法标注`@RequiresPerm("audit:plan:add")`后，无该权限用户访问返回`{code:1005}`
- [ ] 方法标注`@RequiresRole("super_admin")`后，无该角色用户访问返回`{code:1004}`
- [ ] 登录返回`permissions`和`roles`数组，前端可据此控制按钮显隐

### 6.3 兼容性验收

- [ ] 原有11个Controller全部功能不受影响
- [ ] 原有审计计划/审批/对象/统计/附件/消息业务全部正常
- [ ] 默认admin用户登录后拥有全部权限（超管）

---

## 七、权限标识符设计（预设）

按`模块:功能:操作`命名，预置以下权限标识：

```
# 计划管理
audit:plan:list      — 计划列表查看
audit:plan:add       — 新建计划
audit:plan:edit      — 编辑计划
audit:plan:delete    — 删除计划
audit:plan:approve   — 审批操作
audit:plan:change    — 计划变更
audit:plan:export    — 导出计划

# 审计对象
audit:object:list    — 查看审计对象
audit:object:add     — 新增审计对象
audit:object:edit    — 编辑审计对象
audit:object:delete  — 删除审计对象

# 统计分析
audit:statistic:view — 查看统计看板

# 系统管理
audit:sys:role       — 角色管理
audit:sys:perm       — 权限分配
audit:sys:user       — 用户管理
audit:sys:settings   — 系统设置

# 附件
audit:attach:upload  — 上传附件
audit:attach:delete  — 删除附件
```

---

> **下一阶段**: 阶段2 — 系统设计文档（`docs/jwt_rbac系统设计.md`）
