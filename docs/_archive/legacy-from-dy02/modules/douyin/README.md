# 抖音账号模块（douyin）

## 模块概述

管理抖音账号、人设配置、视频数据、粉丝画像。支持 OAuth 授权对接抖音开放平台。

## 后端结构

```
module/douyin/
├── controller/
│   ├── DouyinAccountController.java          # 账号 CRUD
│   ├── DouyinPersonaController.java          # 人设管理
│   ├── DouyinVideoController.java            # 视频数据
│   └── FanProfileController.java             # 粉丝画像
│
├── entity/
│   ├── DouyinAccount.java                    # 抖音账号（douyin_account）
│   ├── DyPersona.java                        # 人设（dy_persona）
│   ├── DouyinVideo.java                      # 视频数据（douyin_video）
│   ├── DyFanProfile.java                     # 粉丝画像
│   └── DyFanProfileStats.java               # 画像统计
│
├── service/
│   ├── DouyinPersonaService / Impl           # 人设管理
│   └── DouyinVideoService / Impl             # 视频管理
│
└── vo/
    ├── DouyinAccountSaveVO / SearchVO / VO / StatisticsVO
    ├── DouyinVideoSaveVO / SearchVO / VO
    ├── PersonaSaveVO
    └── FanProfileVO

module/douyinapi/                             # 抖音 OAuth（独立子模块）
├── controller/DouyinOAuthController.java     # OAuth 回调
├── entity/OAuthToken.java                    # Token 存储
├── repository/OAuthTokenRepository.java
└── service/impl/OAuthTokenServiceImpl.java
```

## 数据库表

| 表名 | 说明 |
|------|------|
| douyin_account | 抖音账号 |
| dy_persona | 人设 |
| douyin_video | 视频数据 |
| dy_fan_profile | 粉丝画像 |
| dy_fan_profile_stats | 画像统计 |
| dy_oauth_token | OAuth Token |

SQL 文件：`sql/douyin/`、`sql/douyinapi/`

## 前端页面

| 页面 | 文件 | 路由 |
|------|------|------|
| 账号列表 | `pages/douyin/DouyinAccountPage.tsx` | `/admin/douyin/accounts` |
| 账号详情 | `pages/douyin/DouyinAccountDetailPage.tsx` | `/admin/douyin/accounts/:id` |
| 人设管理 | `pages/douyin/DouyinPersonaPage.tsx` | `/admin/douyin/personas` |

## 前端 API

文件：`api/douyin.ts`

主要接口：`searchAccounts`、`getAccount`、`saveAccount`、`deleteAccount`、`searchVideos`、`listPersonas`、`savePersona`、`deletePersona`、`setDefaultPersona`、`getDefaultPersona`、`getFanProfile`、`getFanProfileStats` 等

## 人设系统

每个账号可配置多个人设（Persona），AI 生成话术和脚本时会参考人设设定：
- 人设模板：预设常用人设风格
- 默认人设：每个账号可设置默认人设
- 人设字段：名称、口吻、风格、目标受众、方言特色等
