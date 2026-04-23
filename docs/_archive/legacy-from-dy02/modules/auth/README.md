# 认证授权模块（auth）

## 模块概述

管理用户账号、角色权限、资源树、组织机构、OAuth 三方登录，提供 JWT Token 认证。

## 后端结构

```
module/auth/
├── config/
│   └── OAuthProviderProperties.java          # OAuth 提供商配置
├── controller/
│   ├── AuthController.java                   # 登录/登出/验证码/OAuth
│   ├── AuthUserController.java               # 用户 CRUD
│   ├── AuthRoleController.java               # 角色 CRUD
│   ├── AuthResourceController.java           # 资源树 CRUD
│   ├── OrganizationController.java           # 组织 CRUD
│   └── DashboardController.java              # 仪表盘统计
├── entity/
│   ├── AuthUser.java                         # 用户（auth_user）
│   ├── AuthRole.java                         # 角色（auth_role）
│   ├── AuthResource.java                     # 资源/菜单（auth_resource）
│   ├── AuthRoleResource.java                 # 角色-资源关联
│   ├── AuthOrganization.java                 # 组织（auth_organization）
│   ├── AuthOrgMember.java                    # 组织成员
│   ├── AuthLoginLog.java                     # 登录日志（auth_login_log）
│   ├── AuthThirdPartyBind.java               # 三方绑定
│   └── AuthVerifyCode.java                   # 验证码
├── service/
│   ├── AuthUserService.java                  # 用户服务
│   ├── AuthRoleService.java                  # 角色服务
│   ├── AuthResourceService.java              # 资源服务
│   ├── AuthPermissionService.java            # 权限校验服务
│   ├── AuthLoginService.java                 # 登录服务
│   ├── AuthLoginLogService.java              # 登录日志服务
│   ├── AuthTokenStore.java                   # Token 存储接口
│   ├── AuthOAuthService.java                 # OAuth 三方登录服务
│   ├── AuthMenuService.java                  # 菜单服务
│   ├── CaptchaService.java                   # 验证码服务
│   └── OrganizationService.java              # 组织服务
├── service/impl/
│   ├── AuthUserServiceImpl.java
│   ├── AuthRoleServiceImpl.java
│   ├── AuthResourceServiceImpl.java
│   ├── AuthPermissionServiceImpl.java
│   ├── AuthLoginServiceImpl.java
│   ├── AuthLoginLogServiceImpl.java
│   ├── AuthOAuthServiceImpl.java
│   ├── AuthMenuServiceImpl.java
│   ├── CaptchaServiceImpl.java               # 验证码
│   ├── InMemoryAuthTokenStore.java           # 内存 Token 存储
│   ├── RedisAuthTokenStore.java              # Redis Token 存储
│   └── OrganizationServiceImpl.java
└── vo/
    ├── LoginVO / LoginResultVO               # 登录请求/响应
    ├── AuthUserSaveVO / SearchVO / VO        # 用户 CRUD
    ├── AuthRoleSaveVO / SearchVO / VO        # 角色 CRUD
    ├── AuthResourceSaveVO / SearchVO / VO    # 资源 CRUD
    ├── ProfileVO / ProfileUpdateVO           # 个人信息
    ├── ChangePasswordVO / ForgotPasswordVO   # 密码
    ├── OnlineUserVO                          # 在线用户
    └── MenuItemVO                            # 菜单树
```

## 数据库表

| 表名 | 说明 |
|------|------|
| auth_user | 用户账号 |
| auth_role | 角色 |
| auth_resource | 资源/菜单树 |
| auth_role_resource | 角色-资源关联 |
| auth_organization | 组织/机构 |
| auth_org_member | 组织成员 |
| auth_login_log | 登录日志 |
| auth_third_party_bind | 三方账号绑定 |
| auth_verify_code | 验证码 |

SQL 文件：`sql/auth/`

## API 接口

| 接口 | Controller | 说明 |
|------|-----------|------|
| POST /api/v1/auth/login | AuthController | 登录 |
| POST /api/v1/auth/logout | AuthController | 登出 |
| POST /api/v1/auth/captcha | AuthController | 获取验证码 |
| POST /api/v1/auth/profile | AuthController | 获取个人信息 |
| POST /api/v1/auth/profile/update | AuthController | 更新个人信息 |
| POST /api/v1/auth/sms/send | AuthController | 发送短信验证码 |
| POST /api/v1/auth/forgot-password | AuthController | 忘记密码 |
| POST /api/v1/auth/profile/change-password | AuthController | 修改密码 |
| POST /api/v1/auth/menu/search | AuthController | 菜单搜索 |
| POST /api/v1/auth/resource/search | AuthController | 资源搜索 |
| GET /api/v1/auth/oauth/authorize | AuthController | OAuth 授权 |
| GET /api/v1/auth/oauth/callback | AuthController | OAuth 回调 |
| POST /api/v1/auth/oauth/bindings | AuthController | 三方绑定列表 |
| POST /api/v1/auth/oauth/bind | AuthController | 绑定三方账号 |
| POST /api/v1/auth/oauth/unbind | AuthController | 解绑三方账号 |
| POST /api/v1/auth/user/search | AuthUserController | 用户搜索 |
| POST /api/v1/auth/user/get | AuthUserController | 获取用户详情 |
| POST /api/v1/auth/user/save | AuthUserController | 保存用户 |
| POST /api/v1/auth/user/delete | AuthUserController | 删除用户 |
| POST /api/v1/auth/user/ban | AuthUserController | 封禁用户 |
| POST /api/v1/auth/user/login-logs | AuthUserController | 用户登录日志 |
| POST /api/v1/auth/user/online | AuthUserController | 在线用户列表 |
| POST /api/v1/auth/role/search | AuthRoleController | 角色搜索 |
| POST /api/v1/auth/role/get | AuthRoleController | 获取角色详情 |
| POST /api/v1/auth/role/save | AuthRoleController | 保存角色 |
| POST /api/v1/auth/role/delete | AuthRoleController | 删除角色 |
| POST /api/v1/auth/role/resources | AuthRoleController | 角色资源列表 |
| POST /api/v1/auth/role/resources/save | AuthRoleController | 保存角色资源 |
| POST /api/v1/auth/resource/get | AuthResourceController | 获取资源详情 |
| POST /api/v1/auth/resource/save | AuthResourceController | 保存资源 |
| POST /api/v1/auth/resource/delete | AuthResourceController | 删除资源 |
| POST /api/v1/auth/resource/tree-full | AuthResourceController | 完整资源树 |
| POST /api/v1/organization/my | OrganizationController | 当前用户组织 |
| POST /api/v1/organization/create | OrganizationController | 创建组织 |
| POST /api/v1/organization/update | OrganizationController | 更新组织 |
| POST /api/v1/organization/members | OrganizationController | 组织成员 |
| POST /api/v1/organization/invite | OrganizationController | 邀请成员 |
| POST /api/v1/organization/remove | OrganizationController | 移除成员 |
| POST /api/v1/organization/invitations | OrganizationController | 邀请列表 |
| POST /api/v1/organization/invitation/accept | OrganizationController | 接受邀请 |
| POST /api/v1/organization/invitation/reject | OrganizationController | 拒绝邀请 |
| POST /api/v1/organization/search-talents | OrganizationController | 搜索达人 |
| POST /api/v1/dashboard/admin | DashboardController | 管理员仪表盘 |
| POST /api/v1/dashboard/org | DashboardController | 机构仪表盘 |
| POST /api/v1/dashboard/talent | DashboardController | 达人仪表盘 |

## 前端页面

| 页面 | 文件 | 路由 |
|------|------|------|
| 登录 | `pages/LoginPage.tsx` | `/login` |
| 用户管理 | `pages/crud/UsersPage.tsx` | `/admin/auth/users` |
| 角色管理 | `pages/crud/RolesPage.tsx` | `/admin/auth/roles` |
| 资源管理 | `pages/crud/ResourcesPage.tsx` | `/admin/auth/resources` |
| 登录日志 | `pages/crud/LoginLogsPage.tsx` | `/admin/auth/login-logs` |

## 前端 API

文件：`api/auth.ts`（通过 `api/index.ts` 导出）

| 函数 | 后端接口 |
|------|----------|
| auth.login(data) | /auth/login |
| auth.logout() | /auth/logout |
| auth.getProfile() | /auth/profile |

## 角色体系

| 角色码 | 路由前缀 | 说明 |
|--------|----------|------|
| admin | /admin | 平台管理员 |
| org_admin | /org | 机构管理员 |
| talent | /talent | 达人/创作者 |
| user | /talent | 普通用户 |

## 认证流程

1. 用户 POST `/api/v1/auth/login` 提交用户名/密码
2. 后端校验通过后，生成 JWT Token，存入 Redis（或内存）
3. 前端将 Token 存入 localStorage
4. 后续请求通过 `Authorization: Bearer <token>` 头携带
5. `AuthTokenFilter` 拦截每个请求校验 Token
6. `@CurrentUserId` 注解自动注入当前用户 ID
