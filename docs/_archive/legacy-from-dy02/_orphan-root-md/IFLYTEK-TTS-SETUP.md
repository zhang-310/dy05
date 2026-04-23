# 讯飞 TTS 配置与部署

## 一、获取凭证

1. 登录 [讯飞开放平台](https://www.xfyun.cn/)
2. 进入「语音合成（流式版）」服务
3. 创建应用，获取：
   - **APPID**
   - **APIKey**
   - **APISecret**

## 二、配置方式

### 方式 A：环境变量（推荐）

在 `.env` 或系统环境中设置：

```bash
IFLYTEK_APP_ID=你的APPID
IFLYTEK_API_KEY=你的APIKey
IFLYTEK_API_SECRET=你的APISecret
```

### 方式 B：系统配置

在「系统管理 → 配置管理」中新增：

| 配置键 | 值 |
|--------|-----|
| ai.tts.iflytek.app-id | 你的 APPID |
| ai.tts.iflytek.api-key | 你的 APIKey |
| ai.tts.iflytek.api-secret | 你的 APISecret |

## 三、生效逻辑

- **已配置讯飞**：优先使用讯飞 WebSocket 合成，音色为小燕、小宇、晓晓、小琪等
- **未配置讯飞**：使用 `TTS_URL`（默认 `http://localhost:5000`）HTTP 代理

## 四、音色说明

| 音色 ID | 名称 | 说明 |
|---------|------|------|
| xiaoyan | 小燕 | 青年女声（默认） |
| xiaoyu | 小宇 | 青年男声 |
| xiaoxiao | 晓晓 | 温柔女声 |
| xiaoqi | 小琪 | 知性女声 |

## 五、安全提醒

- 请勿将 APIKey、APISecret 提交到代码仓库
- 建议定期在讯飞控制台轮换密钥
- 生产环境使用环境变量或配置中心管理
