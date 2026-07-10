# 审计信息管理系统 — JWT鉴权 + RBAC权限控制 系统设计说明书

> **文档版本**: V1.0  
> **创建日期**: 2026-07-09  
> **关联文档**: [[jwt_rbac需求规格]], [[jwt_rbac测试部署文档]]

---

## 一、总体架构设计

### 1.1 请求鉴权完整时序

```
┌─────────┐     ┌──────────────┐     ┌───────────────┐     ┌───────────┐     ┌──────────────┐
│  Vue前端  │     │ JwtAuthFilter │     │ LoginController │     │ JwtUtil    │     │ UserContext  │
└────┬─────┘     └──────┬───────┘     └───────┬───────┘     └─────┬─────┘     └──────┬───────┘
     │                  │                     │                   │                    │
     │ 1.POST /login    │                     │                   │                    │
     │  {user,password} │                     │                   │                    │
     │─────────────────►│                     │                   │                    │
     │                  │ 白名单放行           │                   │                    │
     │                  │────────────────────►│                   │                    │
     │                  │                     │ 2.查用户(含角色)  │                    │
     │                  │                     │ 3.BCrypt密码比对  │                    │
     │                  │                     │ 4.查角色→权限集合 │                    │
     │                  │                     │ 5.createToken()   │                    │
     │                  │                     │──────────────────►│                    │
     │                  │                     │ ◄───JWT Token─────│                    │
     │ 6.{token,user,   │                     │                   │                    │
     │   roles,perms}   │                     │                   │                    │
     │◄─────────────────│◄────────────────────│                   │                    │
     │                  │                     │                   │                    │
     │ 7.请求业务API    │                     │                   │                    │
     │ Authorization:   │                     │                   │                    │
     │ Bearer {jwt}     │                     │                   │                    │
     │─────────────────►│                     │                   │                    │
     │                  │ 8.解析Token         │                   │                    │
     │                  │────────────────────────────────────────►│                    │
     │                  │ ◄────claims────────────────────────────│                    │
     │                  │ 9.构造UserContext   │                   │                    │
     │                  │────────────────────────────────────────────────────────────►│
     │                  │ 10.放行到Controller │                   │                    │
     │                  │                     │                   │                    │
     │                  │         11.PermissionAspect切面          │                    │
     │                  │         检查@RequiresPerm/@RequiresRole  │                    │
     │                  │         从UserContext取权限比对          │                    │
     │                  │         无权限→返回1005                  │                    │
     │                  │         有权限→执行方法                  │                    │
```

### 1.2 过滤器与切面执行链

```
HTTP Request
  │
  ▼
CorsFilter          ← 跨域处理（已有）
  │
  ▼
JwtAuthenticationFilter  ← 新增：Token解析 + 用户信息存入UserContext
  │                        白名单放行 /audit/v1/login
  │
  ▼
SecurityFilterChain ← 新增：Spring Security配置
  │
  ▼
DispatcherServlet
  │
  ▼
PermissionAspect    ← 新增：拦截@RequiresPerm/@RequiresRole注解
  │
  ▼
Controller Method   ← 业务处理
```

---

## 二、RBAC实体关系模型

### 2.1 ER文字模型

```
sys_user (用户)          sys_role (角色)          sys_permission (权限)
┌──────────────┐        ┌──────────────┐        ┌──────────────────┐
│ user_id (PK) │ 1    M │ role_id (PK) │ 1    M │ perm_id (PK)     │
│ username     │───────│ role_name    │───────│ parent_id (FK)   │──┐
│ password     │   │    │ role_key     │   │    │ perm_name         │  │ 自引用
│ real_name    │   │    │ status       │   │    │ perms (标识符)    │◄─┘
│ role_level   │   │    │ remark       │   │    │ type (菜单/按钮/接口)│
└──────────────┘   │    └──────────────┘   │    │ path (路由地址)    │
                   │                       │    │ sort_order        │
        ┌──────────┘        ┌──────────────┘    └──────────────────┘
        │                   │
        ▼                   ▼
sys_user_role          sys_role_permission
(用户-角色中间表)       (角色-权限中间表)
┌──────────────┐        ┌──────────────┐
│ user_id (FK) │        │ role_id (FK) │
│ role_id (FK) │        │ perm_id (FK) │
└──────────────┘        └──────────────┘

关联链路：
  用户 ──多对多──► 角色 ──多对多──► 权限
  登录时：查询用户所有角色 → 查询角色所有权限标识符 → 存入JWT载荷
```

### 2.2 权限类型说明

| type值 | 说明 | 用途 |
|--------|------|------|
| 0 | 目录 | 一级菜单分组（如"计划管理"） |
| 1 | 菜单 | 可访问的页面路由（如`/plan/form`） |
| 2 | 按钮 | 页面内操作按钮（如新增、删除） |
| 3 | 接口 | API级别控制（`audit:plan:add`） |

---

## 三、项目分包设计

### 3.1 新增包结构

```
com.audit
├── common/
│   ├── Result.java              # [修改] 新增鉴权错误码静态方法
│   ├── ResultCode.java          # [新增] 统一错误码常量类
│   └── util/
│       ├── JwtUtil.java         # [新增] JWT令牌签发/解析工具
│       └── UserContext.java     # [新增] ThreadLocal用户上下文
├── config/
│   ├── CorsConfig.java          # [保留] 原有跨域配置
│   └── security/
│       ├── SecurityConfig.java          # [新增] Spring Security 6.x 配置
│       ├── JwtAuthenticationFilter.java # [新增] 全局JWT过滤器
│       ├── CustomUserDetails.java       # [新增] 用户详情封装
│       ├── annotation/
│       │   ├── RequiresPerm.java        # [新增] 权限标识注解
│       │   └── RequiresRole.java        # [新增] 角色校验注解
│       └── aspect/
│           └── PermissionAspect.java    # [新增] 权限校验切面
├── entity/sys/
│   ├── SysUser.java             # [新增] 用户实体
│   ├── SysRole.java             # [新增] 角色实体
│   └── SysPermission.java       # [新增] 权限实体
├── mapper/sys/
│   ├── SysRoleMapper.java       # [新增] 角色Mapper
│   ├── SysPermissionMapper.java # [新增] 权限Mapper
│   └── SysUserRoleMapper.java   # [新增] 用户-角色关联Mapper
├── service/sys/
│   ├── SysRoleService.java      # [新增] 角色Service接口
│   ├── SysPermissionService.java# [新增] 权限Service接口
│   └── impl/
│       ├── SysRoleServiceImpl.java
│       └── SysPermissionServiceImpl.java
├── controller/
│   ├── AuthController.java      # [重构] 替换为JWT+BCrypt认证
│   └── sys/
│       ├── SysRoleController.java       # [新增] 角色管理接口
│       └── SysPermissionController.java # [新增] 权限管理接口
```

### 3.2 原有代码零修改清单

以下代码**完全不修改**：
- AuditObjectController, AttachmentController, DictController
- MessageController, PlanBatchController, RecommendController
- SettingsController, StatisticsController, TemplateController
- UserManageController (保留但不增加鉴权注解，后续可按需添加)
- 所有Service/ServiceImpl/Mapper/Entity（除UserMapper需追加方法）

---

## 四、Maven依赖清单

在现有`pom.xml`基础上追加：

| 依赖 | 版本 | 说明 |
|------|------|------|
| `spring-boot-starter-security` | 3.4.1 (父POM管理) | Spring Security核心 |
| `jjwt-api` | 0.12.6 | JJWT API |
| `jjwt-impl` | 0.12.6 | JJWT实现 |
| `jjwt-jackson` | 0.12.6 | JJWT JSON序列化 |
| `spring-boot-starter-aop` | 3.4.1 (父POM管理) | AOP切面支持 |

所有版本兼容Spring Boot 3.4.1 + Java 21。

---

## 五、JWT载荷结构设计

### 5.1 Payload Claims

```json
{
  "sub": "admin",                    // 用户名 (Subject)
  "userId": "admin",                 // 用户ID（自定义claim）
  "roles": ["super_admin", "auditor"], // 角色标识集合
  "perms": [                         // 权限标识符集合
    "audit:plan:list",
    "audit:plan:add",
    "audit:plan:edit",
    "audit:plan:delete",
    "audit:object:list",
    "audit:sys:role",
    "audit:sys:user",
    "audit:sys:perm",
    "*"                               // 超管通配符（拥有全部权限）
  ],
  "iat": 1752000000,                 // 签发时间
  "exp": 1752007200                  // 过期时间（iat+7200秒=2小时）
}
```

### 5.2 配置项

```yaml
jwt:
  secret: "your-256-bit-secret-key-min-32-chars!!"  # HMAC-SHA256密钥，至少256位
  expiration: 7200                                   # 过期时间（秒），默认7200=2小时
```

---

## 六、数据库表详细设计

### 6.1 sys_role（角色表）

| 字段 | 类型 | 说明 |
|------|------|------|
| role_id | BIGINT PK AUTO_INCREMENT | 主键 |
| role_name | VARCHAR(50) NOT NULL | 角色名称（如"超级管理员"） |
| role_key | VARCHAR(50) NOT NULL UNIQUE | 角色标识（如"super_admin"） |
| status | TINYINT DEFAULT 1 | 状态 0-禁用 1-启用 |
| remark | VARCHAR(200) | 备注 |
| create_time | DATETIME DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

### 6.2 sys_user_role（用户-角色中间表）

| 字段 | 类型 | 说明 |
|------|------|------|
| user_id | BIGINT NOT NULL | 用户ID（关联sys_user.id） |
| role_id | BIGINT NOT NULL | 角色ID（关联sys_role.role_id） |

联合主键：`PRIMARY KEY (user_id, role_id)`

### 6.3 sys_permission（权限表）

| 字段 | 类型 | 说明 |
|------|------|------|
| perm_id | BIGINT PK AUTO_INCREMENT | 主键 |
| parent_id | BIGINT DEFAULT 0 | 父权限ID，0=顶级 |
| perm_name | VARCHAR(100) NOT NULL | 权限名称（如"计划列表"） |
| perms | VARCHAR(100) | 权限标识符（如"audit:plan:list"） |
| type | TINYINT DEFAULT 1 | 类型：0-目录 1-菜单 2-按钮 3-接口 |
| path | VARCHAR(200) | 路由地址（菜单类型） |
| icon | VARCHAR(100) | 图标（菜单类型） |
| sort_order | INT DEFAULT 0 | 排序 |
| status | TINYINT DEFAULT 1 | 状态 |
| create_time | DATETIME DEFAULT CURRENT_TIMESTAMP | 创建时间 |

### 6.4 sys_role_permission（角色-权限中间表）

| 字段 | 类型 | 说明 |
|------|------|------|
| role_id | BIGINT NOT NULL | 角色ID |
| perm_id | BIGINT NOT NULL | 权限ID |

联合主键：`PRIMARY KEY (role_id, perm_id)`

### 6.5 sys_user现有表适配说明

`sys_user`表**不在本次新增DDL中修改结构**。通过以下方式关联：
- `sys_user_role.user_id` ↔ `sys_user.id`（通过id关联，注意现有sys_user主键为`id BIGINT`）
- 角色和权限完全通过新增的4张表管理
- 原有`sys_user.role`和`sys_user.role_level`字段保留不删，但不再作为鉴权依据

---

## 七、建表SQL

```sql
-- =====================================================
-- RBAC权限管理系统 - 数据库初始化（追加）
-- =====================================================
USE project_practice;

-- 1. 角色表
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    role_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_key VARCHAR(50) NOT NULL UNIQUE COMMENT '角色标识',
    status TINYINT DEFAULT 1 COMMENT '状态:0-禁用 1-启用',
    remark VARCHAR(200) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 用户-角色中间表
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 权限表
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
    perm_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0 COMMENT '父权限ID',
    perm_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    perms VARCHAR(100) COMMENT '权限标识符(如audit:plan:list)',
    type TINYINT DEFAULT 1 COMMENT '类型:0-目录 1-菜单 2-按钮 3-接口',
    path VARCHAR(200) COMMENT '路由地址',
    icon VARCHAR(100) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 角色-权限中间表
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    perm_id BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (role_id, perm_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 种子数据 ====================

-- 角色
INSERT INTO sys_role (role_id, role_name, role_key, remark) VALUES
(1, '超级管理员', 'super_admin', '系统最高权限，拥有全部功能'),
(2, '审计处长', 'director', '审批+查看全部数据'),
(3, '审计组长', 'audit_leader', '管理本人负责的审计计划'),
(4, '普通审计员', 'auditor', '查看和录入权限');

-- 权限（接口级）
INSERT INTO sys_permission (perm_id, parent_id, perm_name, perms, type, sort_order) VALUES
-- 计划管理
(1,  0,  '计划管理',     NULL,                 0, 1),
(2,  1,  '计划列表',     'audit:plan:list',    1, 1),
(3,  1,  '新建计划',     'audit:plan:add',     2, 2),
(4,  1,  '编辑计划',     'audit:plan:edit',    2, 3),
(5,  1,  '删除计划',     'audit:plan:delete',  2, 4),
(6,  1,  '审批操作',     'audit:plan:approve', 2, 5),
(7,  1,  '计划变更',     'audit:plan:change',  2, 6),
(8,  1,  '导出计划',     'audit:plan:export',  2, 7),
-- 审计对象
(9,  0,  '审计对象',     NULL,                   0, 2),
(10, 9,  '查看对象',     'audit:object:list',    1, 1),
(11, 9,  '新增对象',     'audit:object:add',     2, 2),
(12, 9,  '编辑对象',     'audit:object:edit',    2, 3),
(13, 9,  '删除对象',     'audit:object:delete',  2, 4),
-- 统计分析
(14, 0,  '统计分析',     NULL,                    0, 3),
(15, 14, '查看统计',     'audit:statistic:view',  1, 1),
-- 系统管理
(16, 0,  '系统管理',     NULL,                    0, 4),
(17, 16, '角色管理',     'audit:sys:role',        1, 1),
(18, 16, '权限分配',     'audit:sys:perm',        1, 2),
(19, 16, '用户管理',     'audit:sys:user',        1, 3),
(20, 16, '系统设置',     'audit:sys:settings',    1, 4),
-- 附件
(21, 0,  '附件管理',     NULL,                   0, 5),
(22, 21, '上传附件',     'audit:attach:upload',  2, 1),
(23, 21, '删除附件',     'audit:attach:delete',  2, 2);

-- 角色-权限分配
-- 超级管理员：全部权限
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT 1, perm_id FROM sys_permission WHERE perms IS NOT NULL;

-- 审计处长：查看+审批+系统管理（不含删除计划）
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT 2, perm_id FROM sys_permission WHERE perms IN (
    'audit:plan:list','audit:plan:add','audit:plan:edit','audit:plan:approve',
    'audit:plan:change','audit:plan:export',
    'audit:object:list','audit:statistic:view',
    'audit:sys:settings','audit:attach:upload','audit:attach:delete'
);

-- 审计组长：查看+部分操作
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT 3, perm_id FROM sys_permission WHERE perms IN (
    'audit:plan:list','audit:plan:add','audit:plan:edit','audit:plan:export',
    'audit:object:list','audit:statistic:view',
    'audit:attach:upload'
);

-- 普通审计员：查看
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT 4, perm_id FROM sys_permission WHERE perms IN (
    'audit:plan:list','audit:object:list','audit:statistic:view'
);

-- 用户-角色分配（sys_user.id: admin=1, zhangsan=2, lisi=3）
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1), -- admin → 超级管理员
(2, 3), -- zhangsan → 审计组长
(3, 4); -- lisi → 普通审计员

-- 更新现有用户密码为BCrypt密文（123456的BCrypt值）
-- 注意：实际由LoginController首次登录时处理，此处为兜底
-- BCrypt "$2a$10$..." 格式 60字符密文
```

---

## 八、关键设计决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| JWT存储角色/权限 | 存入载荷(claims) | 避免每次请求查库，权限集合较小（<50项） |
| ThreadLocal方式 | 手动UserContext | 轻量，无第三方依赖，Filter设置+请求结束清除 |
| 密码加密 | BCrypt(强度10) | Spring Security默认，60字符输出，安全且普遍 |
| 权限注解 vs URL拦截 | 注解 `@RequiresPerm` | 精确到方法级，灵活，不依赖URL规则 |
| AOP实现 | Spring AOP + AspectJ | Spring Boot原生支持，无需额外依赖 |
| Security配置 | SecurityFilterChain Bean | Spring Boot 3.x标准，不使用废弃Adapter |
| 超级管理员权限 | 通配符`*`标识 | 判断简单，`perms.contains("*")`即放行 |

---

> **下一阶段**: 阶段3 — 编码落地（直接操作项目文件）
