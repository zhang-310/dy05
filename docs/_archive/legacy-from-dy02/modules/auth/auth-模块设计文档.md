> ⚠️ **本文档已废弃** — 内容已拆分为 9 文件标准结构（00-大纲.md ~ 08-测试与验收.md），请以拆分文件为准。本文件仅保留作为历史参考。

# auth 模块设计文档

> 版本：3.1 | 更新日期：2026-02-25 | 阶段：P0

---

## 一、需求分析（Requirements Analysis）

### 1.1 模块定位

auth 模块是整个系统的基础支撑，负责用户身份认证（Authentication）和访问授权（Authorization）。所有其他模块都依赖 auth 模块提供的登录态和权限校验。

### 1.2 用户故事（User Stories）

| 编号 | 角色 | 故事 | 验收条件 |
|------|------|------|----------|
| AUTH-01 | 达人/主播 | 我要能登录系统（密码/短信/邮箱） | 用户名+密码登录、短信验证码登录、邮箱验证码登录，成功后获得 Token |
| AUTH-02 | 达人/主播 | 我要能修改密码和个人资料 | 修改密码需验证原密码、修改昵称/头像即时生效 |
| AUTH-03 | 达人/主播 | 我登录过期后要能重新登录 | Token 过期返回 2001、前端跳转登录页 |
| AUTH-04 | 达人/主播 | 我忘记密码时能通过验证码重置 | 通过手机/邮箱验证码重置密码 |
| AUTH-05 | 达人/主播 | 我要能用第三方账号登录/绑定 | 微信/QQ/抖音/火山 OAuth 登录、绑定/解绑 |
| AUTH-06 | 平台管理员 | 我要能管理所有用户 | 用户列表、搜索、查看详情、封禁/解封、在线用户 |
| AUTH-07 | 平台管理员 | 我要能管理角色和权限 | 角色 CRUD、角色绑定资源（菜单/API/按钮） |
| AUTH-08 | 平台管理员 | 我要能管理系统资源（菜单/接口/按钮） | 资源树形展示、CRUD、按类型区分图标 |
| AUTH-09 | 平台管理员 | 我要能看到用户的登录记录 | 登录日志列表、按用户/时间筛选 |
| AUTH-10 | 系统 | 未登录用户访问受保护接口要拦截 | 返回 status=2001 |
| AUTH-11 | 系统 | 无权限用户访问受限接口要拦截 | 返回 status=2002 |

### 1.3 功能清单

```
auth 模块
├── 用户认证
│   ├── 登录（用户名+密码，支持图片验证码）
│   ├── 短信/邮箱验证码登录
│   ├── 忘记密码（验证码重置）
│   ├── 图片验证码（登录失败后触发）
│   ├── 获取当前用户信息
│   ├── 修改密码
│   └── 修改个人资料
│
├── 第三方登录
│   ├── OAuth 授权（微信/QQ/抖音/火山）
│   ├── OAuth 回调处理
│   ├── 第三方绑定/解绑
│   └── 已绑定平台查询
│
├── 用户管理（管理员）
│   ├── 用户列表（分页、搜索）
│   ├── 用户详情
│   ├── 新增/编辑用户
│   ├── 封禁 / 解封用户
│   ├── 登录日志查询
│   └── 在线用户列表
│
├── 角色管理（管理员）
│   ├── 角色列表（分页）
│   ├── 角色全量列表（无分页，下拉选用）
│   ├── 角色 CRUD
│   └── 角色授权（绑定资源）
│
├── 资源管理（管理员）
│   ├── 资源分页列表
│   ├── 菜单树
│   ├── 资源 CRUD
│   └── 资源删除（逻辑）
│
└── 鉴权机制
    ├── Token 生成与验证（AuthTokenFilter）
    ├── RBAC 权限校验（角色→资源）
    └── 当前用户可访问的菜单/API/按钮列表
```

> **说明：** 系统不设独立注册端点。新用户由管理员通过 `POST /api/v1/auth/user/save` 创建，或通过 OAuth 首次登录时自动创建。
> **说明：** 短信/邮箱验证码首版仅落库不真实发送，便于开发联调；正式环境需接入短信/邮件服务。

### 1.4 业务规则

| 编号 | 规则 | 说明 |
|------|------|------|
| BR-01 | 用户名唯一 | 管理员创建用户时检查用户名是否已存在 |
| BR-02 | 密码加密存储 | BCrypt 加密，不可逆，最低 6 位 |
| BR-03 | Token 有效期 | 24 小时（可配置），过期需重新登录 |
| BR-04 | 一个用户一个角色 | 用户通过 role_code 关联角色（简化模型） |
| BR-05 | 逻辑删除 | 用户/角色/资源删除为 deleted=1，不物理删除 |
| BR-06 | 封禁用户不可登录 | status=1（封禁）的用户登录返回 2006（0=正常, 1=封禁） |
| BR-07 | 管理员接口保护 | 管理端接口需 role_code=admin 校验 |
| BR-08 | 默认角色 | 新用户默认分配 `user` 角色 |
| BR-09 | 超级管理员 | role_code=admin 的用户拥有所有资源权限，不做接口级校验 |
| BR-10 | 登录防暴力 | 密码错误后 `recordLoginFailure(IP)`，同 IP 15 分钟内需图片验证码 |
| BR-11 | 图片验证码 | 4 位字母数字混合，5 分钟有效，校验后立即失效 |
| BR-12 | 短信/邮箱验证码 | 6 位纯数字，5 分钟有效，落库存储 |
| BR-13 | OAuth 首次登录 | 自动创建用户并绑定第三方，分配 user 角色 |
| BR-14 | Token 存储 | 内存 ConcurrentHashMap（单机部署），集群环境需改 Redis 实现 |
| BR-15 | 验证码发送限流 | 同 target 60 秒内仅可发送 1 次，防短信轰炸 |

### 1.5 模块依赖

```
auth 模块不依赖任何其他业务模块（它是被依赖的基础）

依赖关系：
common (config/vo/util/constant) ← auth
                                    ↑
                    所有其他业务模块都依赖 auth
```

### 1.6 安全设计

**认证方式：**
- Token-based（Bearer），通过 `Authorization: Bearer <token>` Header 传递
- Token 为 UUID 随机字符串，存储在 `AuthTokenStore`（内存实现），24h 过期
- 前端存 localStorage，每次请求携带

**防暴力破解：**
- 密码登录失败 → `CaptchaService.recordLoginFailure(clientIp)`
- 同 IP 15 分钟内（`getRequireCaptchaMinutes()`）后续登录需图片验证码
- 验证码 4 位字母数字混合，base64 图片，5 分钟有效

**密码安全：**
- BCrypt 加密存储，不可逆
- 修改密码需验证原密码
- 忘记密码通过验证码重置，最低 6 位

**CSRF：**
- 已禁用（Token 认证模式不需要 CSRF 保护）

**白名单（AuthTokenFilter.WHITELIST）：**
1. `/api/v1/auth/login` -- 登录
2. `/api/v1/auth/captcha` -- 获取图片验证码
3. `/api/v1/auth/sms/send` -- 发送短信/邮箱验证码
4. `/api/v1/auth/forgot-password` -- 忘记密码
5. `/api/v1/auth/oauth/authorize` -- OAuth 授权 URL
6. `/api/v1/auth/oauth/callback` -- OAuth 回调

**权限模型（RBAC）：**
- 角色→资源多对多关联，通过 `auth_role_resource` 关联表
- `admin` 角色超级权限，`AuthPermissionService.hasPermission()` 跳过校验
- 非 admin 用户按角色已授权的资源编码（API 路径）校验

**角色权限边界：**
- `admin`：全局管理，可访问所有管理端接口
- `institution`：机构管理员，管理机构下的达人、内容和数据，可访问 /org/* 路由下的功能
- `talent`：达人/主播，管理个人账号、内容创作和直播，可访问 /talent/* 路由下的功能
- `user`：普通用户，仅个人数据操作（profile、change-password、menu/search、resource/search）

**数据安全：**
- 手机号/邮箱展示脱敏（如 138\*\*\*\*0001、z\*\*\*@example.com）

---

## 二、数据库设计（Database Design）

### 2.1 ER 关系

```
auth_user ──(role_code)──→ auth_role
                              │
                     auth_role_resource (多对多)
                              │
                        auth_resource (树形: parent_id)

auth_user ──→ auth_login_log (一对多)
auth_user ──→ auth_verify_code (一对多)
auth_user ──→ auth_third_party_bind (一对多)
```

### 2.2 表结构

#### auth_user -- 用户表

> 字段对齐 `AuthUser.java` Entity

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| username | VARCHAR(64) | NOT NULL | -- | 用户名（唯一） |
| password_hash | VARCHAR(128) | NOT NULL | -- | 密码哈希（BCrypt） |
| mobile | VARCHAR(20) | | -- | 手机号 |
| email | VARCHAR(128) | | -- | 邮箱 |
| nickname | VARCHAR(64) | | -- | 昵称 |
| avatar_url | VARCHAR(256) | | -- | 头像 URL |
| role_code | VARCHAR(32) | NOT NULL | 'user' | 角色编码，关联 auth_role.role_code |
| status | INTEGER | NOT NULL | 0 | 0=正常 1=封禁 |
| banned_at | TIMESTAMP | | -- | 封禁时间 |
| banned_reason | VARCHAR(256) | | -- | 封禁原因 |
| last_login_at | TIMESTAMP | | -- | 最后登录时间 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | 更新时间 |

索引：`UNIQUE(username) WHERE deleted=0`

Entity 注解：`@SQLRestriction("deleted = 0")`

#### auth_role -- 角色表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| role_code | VARCHAR(64) | NOT NULL | -- | 角色编码（唯一标识）：admin, institution, talent, user |
| role_name | VARCHAR(64) | NOT NULL | -- | 角色名称：平台管理员, 机构管理员, 达人, 普通用户 |
| status | INTEGER | NOT NULL | 1 | 状态：1=正常 0=禁用 |
| sort_order | INTEGER | | 0 | 排序 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | 更新时间 |

索引：`UNIQUE(role_code) WHERE deleted=0`

#### auth_resource -- 资源表（菜单/API/按钮）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| resource_type | VARCHAR(16) | NOT NULL | -- | 类型：menu / api / button |
| resource_code | VARCHAR(256) | NOT NULL | -- | 资源编码（菜单路径/API路径/按钮标识） |
| resource_name | VARCHAR(128) | | -- | 资源名称 |
| module | VARCHAR(64) | | -- | 所属模块：auth, douyin, live... |
| request_method | VARCHAR(16) | | -- | 请求方式：GET/POST/PUT/DELETE（api 类型用） |
| parent_id | BIGINT | | 0 | 父级 ID（menu 类型用于构建树） |
| sort_order | INTEGER | | 0 | 排序 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

#### auth_role_resource -- 角色-资源关联表

> 此为关联表，不使用逻辑删除（无 deleted 字段）。角色授权更新采用"事务内全删重建"策略。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| role_id | BIGINT | NOT NULL | -- | 角色 ID |
| resource_id | BIGINT | NOT NULL | -- | 资源 ID |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | 创建时间 |

索引：`UNIQUE(role_id, resource_id)`

Entity 注解：无 `@SQLRestriction`（无 deleted 字段）

#### auth_login_log -- 登录日志表

> 字段对齐 `AuthLoginLog.java` Entity 及 `sql/auth/schema.sql`

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| user_id | BIGINT | | -- | 用户 ID（失败时可为空，如用户不存在） |
| username | VARCHAR(64) | | -- | 用户名（冗余，方便查询；失败时记录尝试的用户名） |
| login_type | VARCHAR(32) | NOT NULL | 'password' | 登录方式：password / sms / email / oauth |
| device_type | VARCHAR(16) | NOT NULL | 'web' | 设备类型：web / ios / android |
| ip | VARCHAR(64) | | -- | 登录 IP |
| user_agent | VARCHAR(256) | | -- | 浏览器 User-Agent |
| status | INTEGER | NOT NULL | 1 | 1=成功 0=失败 |
| fail_reason | VARCHAR(256) | | -- | 失败原因（如：密码错误、用户不存在） |
| login_time | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | 登录时间 |

索引：`(user_id, login_time DESC)`（user_id 为空时仍可插入）

#### auth_verify_code -- 验证码表

> 字段对齐 `AuthVerifyCode.java` Entity 及 `sql/auth/schema.sql`

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| target | VARCHAR(128) | NOT NULL | -- | 手机号或邮箱 |
| code | VARCHAR(16) | NOT NULL | -- | 验证码（6 位纯数字） |
| type | VARCHAR(32) | NOT NULL | -- | 用途：login / forgot_password |
| expire_at | TIMESTAMP | NOT NULL | -- | 过期时间（创建时间 + 5 分钟） |
| used | INTEGER | NOT NULL | 0 | 是否已使用：0=未用 1=已用 |
| try_count | INTEGER | NOT NULL | 0 | 尝试次数（校验失败时递增，防暴力） |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | 创建时间 |

#### auth_third_party_bind -- 第三方绑定表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| user_id | BIGINT | NOT NULL | -- | 用户 ID |
| provider | VARCHAR(32) | NOT NULL | -- | 第三方平台：wechat / qq / douyin / volcano |
| open_id | VARCHAR(256) | NOT NULL | -- | 第三方平台用户 ID |
| union_id | VARCHAR(256) | | -- | 联合 ID |
| nickname | VARCHAR(64) | | -- | 第三方昵称 |
| avatar | VARCHAR(512) | | -- | 第三方头像 |
| deleted | INTEGER | NOT NULL | 0 | |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`UNIQUE(provider, open_id) WHERE deleted=0`

### 2.3 初始数据

```sql
-- 默认角色
INSERT INTO auth_role (role_code, role_name, status, sort_order) VALUES
('admin', '平台管理员', 1, 1),
('institution', '机构管理员', 1, 5),
('talent', '达人', 1, 8),
('user', '普通用户', 1, 10);

-- 超级管理员账号（密码: admin123，BCrypt 加密）
INSERT INTO auth_user (username, password_hash, nickname, role_code, status) VALUES
('admin', '$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx', '管理员', 'admin', 0);
```

> **注意：** auth_user.status 默认值为 0（正常），0=正常 1=封禁。

---

## 三、接口设计（API Design）

### 3.1 接口总表

#### AuthController（/api/v1/auth）

| # | 方法 | 路径 | 权限 | 说明 |
|---|------|------|------|------|
| 1 | GET | /api/v1/auth/captcha | 公开 | 获取图片验证码 |
| 2 | POST | /api/v1/auth/login | 公开 | 用户登录（密码/短信/邮箱） |
| 3 | POST | /api/v1/auth/sms/send | 公开 | 发送验证码（短信/邮箱） |
| 4 | POST | /api/v1/auth/forgot-password | 公开 | 忘记密码（验证码重置） |
| 5 | POST | /api/v1/auth/profile | 登录 | 获取当前用户信息 |
| 6 | POST | /api/v1/auth/profile/update | 登录 | 修改个人资料 |
| 7 | POST | /api/v1/auth/profile/change-password | 登录 | 修改密码 |
| 8 | POST | /api/v1/auth/menu/search | 登录 | 获取当前用户菜单树 |
| 9 | POST | /api/v1/auth/resource/search | 登录 | 获取当前用户可访问资源编码 |
| 10 | GET | /api/v1/auth/oauth/authorize | 公开 | 获取 OAuth 授权 URL |
| 11 | GET | /api/v1/auth/oauth/callback | 公开 | OAuth 回调处理（重定向） |
| 12 | POST | /api/v1/auth/oauth/bindings | 登录 | 查询已绑定的第三方列表 |
| 13 | POST | /api/v1/auth/oauth/bind | 登录 | 绑定第三方账号 |
| 14 | POST | /api/v1/auth/oauth/unbind | 登录 | 解绑第三方账号 |

> **GET 方法说明：** 接口 1 使用 GET 是因为验证码图片通常以 `<img src="...">` 方式加载；接口 10/11 使用 GET 是因为浏览器 OAuth 重定向流程要求 GET 请求。

#### AuthUserController（/api/v1/auth/user）

| # | 方法 | 路径 | 权限 | 说明 |
|---|------|------|------|------|
| 15 | POST | /api/v1/auth/user/search | 管理员 | 用户列表（分页） |
| 16 | POST | /api/v1/auth/user/get | 管理员 | 用户详情 |
| 17 | POST | /api/v1/auth/user/save | 管理员 | 新增/编辑用户 |
| 18 | POST | /api/v1/auth/user/ban | 管理员 | 封禁/解封用户 |
| 19 | POST | /api/v1/auth/user/login-logs | 管理员 | 用户登录日志 |
| 20 | POST | /api/v1/auth/user/online | 管理员 | 在线用户列表 |

#### AuthRoleController（/api/v1/auth/role）

| # | 方法 | 路径 | 权限 | 说明 |
|---|------|------|------|------|
| 21 | POST | /api/v1/auth/role/search | 管理员 | 角色列表（分页） |
| 22 | POST | /api/v1/auth/role/list | 管理员 | 角色全量列表（无分页） |
| 23 | POST | /api/v1/auth/role/get | 管理员 | 角色详情 |
| 24 | POST | /api/v1/auth/role/save | 管理员 | 新增/编辑角色 |
| 25 | POST | /api/v1/auth/role/delete | 管理员 | 删除角色（逻辑） |
| 26 | POST | /api/v1/auth/role/resources | 管理员 | 获取角色已授权资源 ID 列表 |
| 27 | POST | /api/v1/auth/role/resources/save | 管理员 | 保存角色授权 |

#### AuthResourceController（/api/v1/auth/resource）

| # | 方法 | 路径 | 权限 | 说明 |
|---|------|------|------|------|
| 28 | POST | /api/v1/auth/resource/list | 管理员 | 资源分页列表 |
| 29 | POST | /api/v1/auth/resource/tree | 管理员 | 菜单树 |
| 30 | POST | /api/v1/auth/resource/get | 管理员 | 资源详情 |
| 31 | POST | /api/v1/auth/resource/save | 管理员 | 新增/编辑资源 |
| 32 | POST | /api/v1/auth/resource/delete | 管理员 | 删除资源（逻辑） |

共计 **32 个端点**。

### 3.2 关键接口详情

#### 接口 1：获取图片验证码

```
GET /api/v1/auth/captcha
权限：公开（白名单）
```

**响应：**

```json
{
  "status": 200,
  "data": {
    "captchaId": "uuid-string",
    "image": "data:image/png;base64,..."
  }
}
```

**业务逻辑：**
1. `CaptchaService.generate()` 生成 4 位字母数字混合验证码
2. 验证码存入内存缓存，5 分钟有效
3. 返回 captchaId + base64 编码图片

---

#### 接口 2：用户登录

```
POST /api/v1/auth/login
权限：公开（白名单）
```

**请求参数（LoginVO）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| loginType | String | 否 | 登录方式：password(默认) / sms / email |
| username | String | 条件必填 | loginType=password 时必填 |
| password | String | 条件必填 | loginType=password 时必填 |
| target | String | 条件必填 | loginType=sms/email 时必填（手机号或邮箱） |
| code | String | 条件必填 | loginType=sms/email 时必填（6 位验证码） |
| captchaId | String | 条件必填 | 该 IP 需要验证码时必填 |
| captchaCode | String | 条件必填 | 该 IP 需要验证码时必填 |

**响应（LoginResultVO）：**

```json
{
  "status": 200,
  "message": "登录成功",
  "data": {
    "token": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "userId": 1,
    "username": "zhangsan",
    "nickname": "张三",
    "avatarUrl": "https://...",
    "roleCode": "user"
  }
}
```

**业务逻辑（password 方式）：**
1. 检查 `CaptchaService.requireCaptcha(clientIp)` → 若需要则校验 captchaId + captchaCode
2. 查询用户（username + deleted=0）
3. 用户不存在 → 2005（LOGIN_FAILED）
4. 用户被封禁（status=1）→ 2006（USER_DISABLED）
5. 验证密码（BCrypt）→ 密码错误 → `recordLoginFailure(clientIp)` → 2005
6. 生成 Token（UUID），存入 AuthTokenStore（createToken）
7. 发布 `LoginSuccessEvent`（记录登录日志）
8. 更新 last_login_at
9. 返回 Token + 用户信息

**业务逻辑（sms/email 方式）：**
1. 通过 target（手机号/邮箱）查找用户
2. 校验 auth_verify_code 表中的验证码（type=login，未过期，未使用）
3. 校验通过后标记验证码已使用
4. 后续同 password 方式步骤 4-9

**错误码：** 2004（需要图片验证码）、2005（用户名或密码错误）、2006（账号已禁用）

---

#### 接口 3：发送验证码

```
POST /api/v1/auth/sms/send
权限：公开（白名单）
```

**请求参数（SmsSendVO）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| target | String | 是 | 手机号或邮箱 |
| type | String | 是 | 用途：login / forgot_password |

**业务逻辑：**
1. 校验限流：同 target 60 秒内仅可发送 1 次（BR-15），超限返回 1003（RATE_LIMIT）
2. 生成 6 位纯数字验证码
3. 落库 auth_verify_code，有效期 5 分钟（expire_at = now + 5min）
4. 首版不真实发送短信/邮件，仅落库

---

#### 接口 4：忘记密码

```
POST /api/v1/auth/forgot-password
权限：公开（白名单）
```

**请求参数（ForgotPasswordVO）：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| target | String | 是 | 最长 128 | 手机号或邮箱 |
| code | String | 是 | 最长 16 | 验证码 |
| newPassword | String | 是 | 6-128 位 | 新密码 |

**业务逻辑：**
1. 校验验证码（type=forgot_password，未过期，未使用）
2. 通过 target 查找用户（手机号或邮箱）
3. 若 target 对应多个用户（手机/邮箱重复）：取第一个匹配且 status=0 的用户；若无则返回 2007（USER_NOT_FOUND）
4. BCrypt 加密新密码，更新 password_hash

---

#### 接口 5：获取当前用户信息

```
POST /api/v1/auth/profile
权限：登录
```

**请求参数：** 无（从 Token 中获取 userId）

**响应（ProfileVO）：**

```json
{
  "status": 200,
  "data": {
    "id": 1,
    "username": "zhangsan",
    "nickname": "张三",
    "avatarUrl": "https://...",
    "mobile": "138****0001",
    "email": "z***@example.com",
    "roleCode": "user",
    "createTime": "2026-01-01 10:00:00"
  }
}
```

---

#### 接口 10：获取 OAuth 授权 URL

```
GET /api/v1/auth/oauth/authorize?provider=wechat&state=login
权限：公开（白名单）
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| provider | String | 是 | 平台：wechat / qq / douyin / volcano |
| state | String | 否 | 默认 login；传 bind 则自动拼接 userId |

**响应：**

```json
{
  "status": 200,
  "data": { "url": "https://open.weixin.qq.com/connect/oauth2/authorize?..." }
}
```

**业务逻辑：**
1. `AuthOAuthService.getAuthorizeUrl(provider, state)` 构造授权 URL
2. 如果 state=bind 且用户已登录，state 改写为 `bind:<userId>`
3. 未配置/未启用的 provider 返回 1001

---

#### 接口 11：OAuth 回调处理

```
GET /api/v1/auth/oauth/callback?code=xxx&state=xxx
权限：公开（白名单）
返回：RedirectView（非 JSON）
```

**业务逻辑：**
1. 解析 provider（从 state 中提取）
2. `AuthOAuthService.handleCallback(provider, code, state)` 处理：
   - 用 code 换 access_token
   - 拉取第三方用户信息
   - state=login：查找已绑定用户登录 / 首次登录自动创建用户
   - state=bind:userId：绑定到指定用户
3. 成功：重定向到 `/pages/auth/oauth-callback.html?token=xxx&userId=xxx&username=xxx`
4. 失败：重定向到 `/pages/auth/login.html?oauth_error=xxx`

---

#### 接口 15：用户列表（管理员）

```
POST /api/v1/auth/user/search
权限：管理员（role_code=admin）
```

**请求参数（AuthUserSearchVO，继承 BasicQueryDto）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认 0 |
| rows | Integer | 否 | 每页条数，默认 30 |
| sortName | String | 否 | 排序字段，默认 id |
| sortOrder | String | 否 | asc/desc，默认 desc |
| username | String | 否 | 模糊搜索 |
| nickname | String | 否 | 模糊搜索 |
| roleCode | String | 否 | 精确筛选 |
| status | Integer | 否 | 状态筛选 |

**响应（PageResultVO）：**

```json
{
  "status": 200,
  "data": {
    "total": 100,
    "list": [
      {
        "id": 1,
        "username": "zhangsan",
        "nickname": "张三",
        "roleCode": "user",
        "status": 0,
        "lastLoginAt": "2026-02-25 10:00:00",
        "createTime": "2026-01-01 10:00:00"
      }
    ],
    "pageNum": 0,
    "pageSize": 30
  }
}
```

---

#### 接口 18：封禁/解封用户

```
POST /api/v1/auth/user/ban
权限：管理员
```

**请求参数（AuthUserBanVO，@RequestBody）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户 ID |
| ban | Boolean | 是 | true=封禁 false=解封 |
| reason | String | 否 | 封禁原因 |

**业务逻辑：**
- 封禁：status=1, banned_at=now, banned_reason=reason
- 解封：status=0, banned_at=null, banned_reason=null

---

#### 接口 27：保存角色授权

```
POST /api/v1/auth/role/resources/save
权限：管理员
```

**请求参数（AuthRoleResourceSaveVO）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| roleId | Long | 是 | 角色 ID |
| resourceIds | List\<Long\> | 是 | 资源 ID 列表（全量覆盖） |

**业务逻辑：**
1. 事务内删除该角色原有的所有 auth_role_resource 记录
2. 批量插入新的关联记录
3. 整个操作在一个 @Transactional 中

---

## 四、页面设计（Frontend Design）

### 4.1 页面清单

#### SSI 页面（逐步迁移中）

| # | 页面 | 路径 | 入口 | 说明 |
|---|------|------|------|------|
| 1 | 登录页 | /pages/auth/login.html | 公开 | 用户名+密码+验证码登录 |
| 2 | 忘记密码页 | /pages/auth/forgot-password.html | 公开 | 输入手机/邮箱→发送验证码→重置密码 |
| 3 | OAuth 回调页 | /pages/auth/oauth-callback.html | OAuth 回调 | 处理 OAuth 回调 token |
| 4 | 用户管理 | /admin/auth/user-list.html | 管理端菜单 | 用户列表 + 编辑侧滑窗 |
| 5 | 角色管理 | /admin/auth/role-list.html | 管理端菜单 | 角色列表 + 编辑侧滑窗 + 授权弹窗 |
| 6 | 资源管理 | /admin/auth/resource-list.html | 管理端菜单 | 资源树(jstree) + 编辑侧滑窗 |
| 7 | 个人资料 | /admin/auth/profile.html | 头像下拉菜单 | 修改昵称/头像/密码 |

#### Vue 3 SPA 路由对照表

| 路由 | 组件 | 说明 |
|------|------|------|
| /login | LoginView | Vue 3 登录页（支持密码/验证码/OAuth） |
| /forgot-password | ForgotPasswordView | 忘记密码页 |
| /admin/auth/user | AuthUserView | 用户管理 |
| /admin/auth/role | AuthRoleView | 角色管理 |
| /admin/auth/resource | AuthResourceView | 资源管理 |

### 4.2 页面详细设计

#### 页面 1：登录页

```
┌──────────────────────────────────┐
│         抖音运营 SaaS 平台        │
│                                  │
│  ┌────────────────────────────┐  │
│  │ 用户名  [________________] │  │
│  │ 密  码  [________________] │  │
│  │ 验证码  [______] [图片]    │  │  ← 失败后出现
│  │                            │  │
│  │      [ 登 录 ]             │  │
│  │                            │  │
│  │  短信登录 | 邮箱登录        │  │
│  │  忘记密码？                 │  │
│  │                            │  │
│  │  ── 第三方登录 ──          │  │
│  │  [微信] [QQ] [抖音]        │  │
│  └────────────────────────────┘  │
└──────────────────────────────────┘
```

- 调用 `POST /api/v1/auth/login`
- 密码错误后自动调用 `GET /api/v1/auth/captcha` 显示验证码
- 成功后：Token 存 localStorage，跳转管理端
- 第三方登录：调用 `GET /api/v1/auth/oauth/authorize` 获取 URL 后跳转

#### 页面 2：忘记密码页

```
┌──────────────────────────────────┐
│         忘记密码                  │
│                                  │
│  ┌────────────────────────────┐  │
│  │ 手机号/邮箱 [______________] │  │
│  │ 验证码     [______] [发送]  │  │
│  │ 新密码     [______________] │  │
│  │ 确认密码   [______________] │  │
│  │                            │  │
│  │      [ 重置密码 ]           │  │
│  │                            │  │
│  │  返回登录 ←                 │  │
│  └────────────────────────────┘  │
└──────────────────────────────────┘
```

- 调用 `POST /api/v1/auth/sms/send` 发送验证码（type=forgot_password）
- 调用 `POST /api/v1/auth/forgot-password` 重置密码
- 成功后跳转登录页

#### 页面 4：用户管理

```
┌──────────────────────────────────────────────────┐
│ 用户管理                                          │
├──────────────────────────────────────────────────┤
│ [搜索关键词___] [搜索]    [刷新] [新增用户]       │
├──────────────────────────────────────────────────┤
│ □ | ID | 用户名 | 昵称 | 角色 | 状态 | 最后登录 | 操作       │
│ □ | 1  | admin  | 管理员| 管理 | 正常 | 02-25   | 编辑 封禁  │
│ □ | 2  | zhang  | 张三  | 用户 | 正常 | 02-24   | 编辑 封禁  │
├──────────────────────────────────────────────────┤
│              < 1 2 3 ... 10 >                     │
└──────────────────────────────────────────────────┘
```

- 列表调用 `POST /api/v1/auth/user/search`
- 操作列：编辑（打开 Offcanvas）、封禁/解封（调用 `/auth/user/ban`）
- 编辑侧滑窗：用户名、昵称、角色（下拉选择）

#### 页面 5：角色管理

- 列表调用 `POST /api/v1/auth/role/search`
- 操作列：编辑、授权、删除
- 授权弹窗：
  - 调用 `/auth/resource/tree` 获取菜单树
  - 调用 `/auth/role/resources` 获取已授权 ID
  - 使用 jstree + checkbox 展示
  - 保存时调用 `/auth/role/resources/save`

#### 页面 6：资源管理

- 调用 `POST /api/v1/auth/resource/tree` 获取树形数据
- 使用 jstree 展示，节点图标按类型区分
- 编辑侧滑窗：资源类型、编码、名称、模块、请求方式、父级、排序

---

## 五、开发任务拆解（Task Breakdown）

### 5.1 后端任务

| # | 任务 | 输入 | 输出 | 依赖 |
|---|------|------|------|------|
| B1 | 编写并执行 SQL | 表结构设计 | schema.sql + demo.sql 已执行 | 无 |
| B2 | Entity 层 | schema.sql | AuthUser, AuthRole, AuthResource, AuthRoleResource, AuthLoginLog, AuthVerifyCode, AuthThirdPartyBind | B1 |
| B3 | Repository 层 | Entity | 7 个 Repository 接口 | B2 |
| B4 | VO 层 | 接口设计 | LoginVO, LoginResultVO, ProfileVO, ProfileUpdateVO, ChangePasswordVO, SmsSendVO, ForgotPasswordVO, CaptchaVO 等 | B2 |
| B5 | AuthTokenStore | 需求 | Token 存储接口 + 内存实现 | 无 |
| B6 | AuthLoginService | 接口设计 | 登录（3 种方式）、发送验证码（含限流 BR-15）、忘记密码 | B3, B4, B5 |
| B7 | AuthUserService | 接口设计 | 用户 CRUD、封禁/解封、在线用户、登录日志 | B3, B4 |
| B8 | AuthRoleService | 接口设计 | 角色 CRUD、角色全量列表、角色授权资源 | B3, B4 |
| B9 | AuthResourceService | 接口设计 | 资源 CRUD、菜单树、用户资源编码查询 | B3, B4 |
| B10 | AuthPermissionService | 需求 | 权限校验（当前用户是否可访问指定 API） | B3 |
| B11 | AuthMenuService | 接口设计 | 获取当前用户菜单树 | B3, B10 |
| B12 | AuthController | 接口设计 | 登录/验证码/profile/菜单/资源编码（14 个端点） | B6, B7 |
| B13 | AuthUserController | 接口设计 | 用户管理 CRUD（管理端，6 个端点） | B7 |
| B14 | AuthRoleController | 接口设计 | 角色管理 + 授权（管理端，7 个端点） | B8 |
| B15 | AuthResourceController | 接口设计 | 资源管理 CRUD + 树（管理端，5 个端点） | B9 |
| B16 | resource-data.sql | 接口清单 | auth 模块自身的菜单/API/按钮资源注册 | B15 |
| B17 | CaptchaService | 需求 | 图片验证码生成/校验/防暴力（recordLoginFailure, requireCaptcha） | 无 |
| B18 | AuthLoginService 验证码登录 | 接口设计 | 短信/邮箱验证码登录逻辑 | B6, B3 |
| B19 | AuthLoginService 忘记密码 | 接口设计 | 忘记密码（验证码校验 + 密码重置） | B6, B3 |
| B20 | AuthOAuthService | 接口设计 | OAuth 授权 URL、回调处理、首次登录自动创建 | B3, B5 |
| B21 | AuthController OAuth 端点 | 接口设计 | authorize/callback/bindings/bind/unbind（5 个端点） | B20 |
| B22 | AuthTokenStore 过期清理 | 需求 | 内存实现：定时任务（如每 10 分钟）清理过期 Token；Redis 实现可依赖 TTL 自动过期 | B5 |

### 5.2 前端任务

| # | 任务 | 依赖 |
|---|------|------|
| F1 | 登录页（密码+验证码+短信+OAuth） | B12 |
| F2 | 用户管理页 | B13 |
| F3 | 角色管理页 + 授权弹窗 | B14, B15 |
| F4 | 资源管理页（jstree） | B15 |
| F5 | 个人资料页 | B12 |
| F6 | 忘记密码页 | B12 |
| F7 | Vue 3 SPA 路由 + 登录页/忘记密码页迁移 | F1, F6 |

### 5.3 建议开发顺序

```
B1 → B2 → B3 → B4 → B5 → B17
                              ↓
              B6 → B18 → B19 → B12 → F1
              B7 → B13 → F2
              B8 → B14 → F3
              B9 → B15 → F4
              B10, B11
              B16
              B20 → B21 → F1(OAuth部分)
              B22
              F5, F6, F7
```

---

## 六、测试用例（Test Cases）

### 6.1 冒烟测试（Smoke Test）

| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| S1 | 密码登录 | POST /auth/login {username, password} | 200，返回 Token |
| S2 | 获取用户信息 | POST /auth/profile（携带 Token） | 200，返回用户详情 |
| S3 | 用户列表 | POST /auth/user/search（管理员 Token） | 200，返回分页列表 |
| S4 | 角色列表 | POST /auth/role/search（管理员 Token） | 200，返回角色列表 |
| S5 | 资源树 | POST /auth/resource/tree（管理员 Token） | 200，返回树形数据 |
| S6 | 图片验证码 | GET /auth/captcha | 200，返回 captchaId + base64 图片 |

### 6.2 功能测试（Functional Test）

**登录相关：**

| # | 场景 | 输入 | 预期 |
|---|------|------|------|
| F01 | 密码正常登录 | 正确的用户名密码 | 200，返回 Token |
| F02 | 密码错误 | 错误密码 | 2005 |
| F03 | 用户不存在 | 不存在的用户名 | 2005 |
| F04 | 用户被封禁 | status=1 的用户 | 2006 |
| F05 | 密码错误后需验证码 | 连续失败后再次登录不带验证码 | 2004 |
| F06 | 带验证码登录 | 失败后附带正确验证码 | 200 |
| F07 | 验证码过期 | 5 分钟后使用验证码 | 校验失败 |

**图片验证码相关：**

| # | 场景 | 输入 | 预期 |
|---|------|------|------|
| F08 | 生成验证码 | GET /auth/captcha | 200，返回 captchaId + image |
| F09 | 正确校验验证码 | 正确的 captchaId + captchaCode | 校验通过 |
| F10 | 验证码用后失效 | 同一 captchaId 第二次校验 | 校验失败 |
| F11 | 验证码过期 | 5 分钟后校验 | 校验失败 |

**短信/邮箱登录相关：**

| # | 场景 | 输入 | 预期 |
|---|------|------|------|
| F12 | 发送验证码 | POST /auth/sms/send {target, type} | 200 |
| F12a | 验证码发送限流 | 同 target 60 秒内再次发送 | 1003（RATE_LIMIT） |
| F13 | 短信验证码登录 | loginType=sms, target+code | 200，返回 Token |
| F14 | 邮箱验证码登录 | loginType=email, target+code | 200，返回 Token |
| F15 | 验证码错误 | 错误的 code | 校验失败 |
| F16 | 验证码过期 | 5 分钟后使用 | 校验失败 |

**忘记密码相关：**

| # | 场景 | 输入 | 预期 |
|---|------|------|------|
| F17 | 正常重置密码 | 正确的 target + code + newPassword | 200，密码已更新 |
| F17a | target 无对应用户 | target 未绑定任何用户 | 2007（USER_NOT_FOUND） |
| F18 | 验证码错误 | 错误的 code | 校验失败 |
| F19 | 新密码太短 | newPassword 少于 6 位 | 1001 |

**OAuth 相关：**

| # | 场景 | 输入 | 预期 |
|---|------|------|------|
| F20 | 获取授权 URL | GET /auth/oauth/authorize?provider=wechat | 200，返回 url |
| F21 | 未配置的 provider | provider=unknown | 1001 |
| F22 | OAuth 首次登录 | 新第三方用户回调 | 自动创建用户 + 绑定 + 返回 Token |
| F23 | OAuth 已绑定登录 | 已绑定用户回调 | 直接登录返回 Token |
| F24 | 绑定第三方 | POST /auth/oauth/bind {provider, code} | 200，绑定成功 |
| F25 | 解绑第三方 | POST /auth/oauth/unbind {provider} | 200，解绑成功 |
| F26 | 查询已绑定列表 | POST /auth/oauth/bindings | 200，返回 provider 列表 |
| F26a | OAuth 绑定冲突 | 该 provider+open_id 已绑定其他用户时尝试绑定 | 返回 2012（OAUTH_ALREADY_BOUND） |

**权限相关：**

| # | 场景 | 输入 | 预期 |
|---|------|------|------|
| F27 | 无 Token 访问受保护接口 | 不携带 Token | 2001 |
| F28 | 无效 Token | 随机字符串 | 2003 |
| F29 | 普通用户访问管理接口 | user 角色 Token 访问 /auth/user/search | 2002 |
| F30 | 管理员访问管理接口 | admin 角色 Token | 200 |

**用户管理相关：**

| # | 场景 | 操作 | 预期 |
|---|------|------|------|
| F31 | 用户列表分页 | page=0, rows=10 | 返回 <=10 条，total 正确 |
| F32 | 用户搜索 | username=admin | 只返回匹配的用户 |
| F33 | 封禁用户 | POST /auth/user/ban {userId:2, ban:true} | 用户 status 变为 1 |
| F34 | 被封禁用户无法登录 | 登录被封禁的用户 | 2006 |
| F35 | 解封用户 | POST /auth/user/ban {userId:2, ban:false} | 用户可重新登录 |
| F36 | 在线用户列表 | POST /auth/user/online | 返回活跃用户列表 |

**角色授权相关：**

| # | 场景 | 操作 | 预期 |
|---|------|------|------|
| F37 | 保存角色授权 | POST /auth/role/resources/save {roleId, resourceIds} | auth_role_resource 更新 |
| F38 | 查看角色授权 | POST /auth/role/resources {roleId} | 返回该角色已授权的资源 ID 列表 |
| F39 | 授权后权限生效 | 给角色增加新 API 权限，该角色用户可访问 | 200 |

**防暴力破解相关：**

| # | 场景 | 操作 | 预期 |
|---|------|------|------|
| F40 | 首次登录无需验证码 | 正常登录不带 captcha | 200 |
| F41 | 失败后需验证码 | 密码错误 → 再次登录不带验证码 | 2004 |
| F42 | 15 分钟后解除 | 等待 15 分钟后登录 | 无需验证码 |

### 6.3 边界测试（Boundary Test）

| # | 场景 | 预期 |
|---|------|------|
| E01 | 用户名 64 字符（最大值） | 正常创建 |
| E02 | 用户名 65 字符（超限） | 1001 |
| E03 | 密码含特殊字符 | 正常登录 |
| E04 | 分页 rows=0 | 自动修正为默认值 |
| E05 | 分页 rows=1001 | 自动修正为 1000 |
| E06 | sortName 含 SQL 注入字符 | 1001（正则校验拒绝） |

---

## 七、验收标准（Acceptance Criteria）

### 7.1 功能验收

- [ ] 用户可使用用户名+密码登录，获得 Token
- [ ] 用户可使用短信/邮箱验证码登录
- [ ] 用户可通过忘记密码功能重置密码
- [ ] 密码错误后同 IP 15 分钟内需图片验证码
- [ ] 携带有效 Token 可访问受保护接口
- [ ] Token 过期或无效返回 2001/2003
- [ ] 无权限访问返回 2002
- [ ] 用户可修改密码（需验证原密码）和个人资料
- [ ] OAuth 授权/回调/绑定流程可用（微信/QQ/抖音/火山）
- [ ] 管理员可查看用户列表、搜索、分页正常
- [ ] 管理员可新增/编辑用户
- [ ] 管理员可封禁/解封用户，被封禁用户无法登录
- [ ] 管理员可查看在线用户列表
- [ ] 管理员可 CRUD 角色
- [ ] 管理员可给角色授权资源
- [ ] 管理员可查看/编辑/删除资源
- [ ] 资源树正确展示菜单层级
- [ ] 登录日志记录每次登录（成功/失败）

### 7.2 技术验收

- [ ] 所有接口返回 RESTResult 格式，包含 traceId
- [ ] 密码使用 BCrypt 加密存储
- [ ] Entity 使用 `@SQLRestriction("deleted = 0")` 逻辑删除
- [ ] 写操作有 @Transactional
- [ ] 分页参数有上限校验（rows <= 1000）
- [ ] 无 SQL 注入风险（sortName 正则校验、LIKE 转义）
- [ ] 登录失败后同 IP 15 分钟内需验证码
- [ ] OAuth 授权/回调/绑定流程可用
- [ ] Token 过期自动清理（定时任务或懒清理）

---

## 八、上线检查清单（Release Checklist）

- [ ] `sql/auth/schema.sql` 已在目标数据库执行
- [ ] `sql/auth/demo.sql` 已执行（超级管理员账号已创建，status=0 正常）
- [ ] `sql/auth/resource-data.sql` 已执行（auth 模块菜单/API/按钮已注册）
- [ ] admin 角色已绑定所有 auth 模块资源
- [ ] user 角色已绑定基础资源（profile、change-password、menu/search、resource/search）
- [ ] 错误码 2001-2012 已在 ErrorCode.java 和错误码注册表中
- [ ] AuthTokenFilter 白名单包含 6 条路径：login, captcha, sms/send, forgot-password, oauth/authorize, oauth/callback
- [ ] OAuth 提供商已配置（wechat/qq/douyin/volcano 的 appId、appSecret、redirectUri）
- [ ] 登录页可访问，登录后可正常进入管理端
- [ ] 冒烟测试 S1-S6 全部通过
- [ ] 无硬编码密码或 Token 密钥
- [ ] 生产环境 admin 密码已修改为强密码（建议：≥12 位，含大小写字母+数字+特殊字符）
