# 内容日历预测（K-5）与运营看板告警（N-5）

## `POST /api/v1/short-video/content/calendar-forecast`

**Body**：`year`、`month`（与控制器现有字段一致；需登录与数据范围）。

**响应（摘要）**：`RESTResult.data` 为对象，通常含：

| 字段 | 说明 |
|------|------|
| `heuristic` | 排期密度启发式（`method`: `density_heuristic_v1` 等） |
| `statistical` | 统计预测：`method` 为 `statistical_v1` 或 **`statistical_v2`**（由 `app.shortvideo.content-calendar.forecast-stat-version` 决定） |
| `statistical.daily[]` | 每日行：含 `engagementScoreForecast`、v2 时可有区间字段（见实现 `enrichStatisticalV2`） |
| **`statistical.featureVersion`** | 配置口径版本（如 `v1`/`v2`），便于前端/cache 区分 |
| **`statistical.historicalVideoCountInScope`** | **账户级占位特征**：当前可见 `owner` 范围内、未删除的成片视频计数（供「样本量」提示，**非 ML 特征工程**） |
| **`statistical.mlExtensionHint`** | 仅当 `app.shortvideo.content-calendar.ml-extension-hint=true`（`SV_CONTENT_CALENDAR_ML_EXTENSION_HINT`）时出现；文案说明可接外部推理服务。**默认应关闭**，避免被误解为已上线 ML |

**验收口径**：曲线与分数为 **规则 + 历史月统计**，不是抖音官方或黑盒模型预测；`historicalVideoCountInScope` 仅作透明度字段。

---

## `POST /api/v1/short-video/content/ops-dashboard-summary`

**`rulesSummary`** 中 `completionGap.severity === "warn"` 时，若启用下方开关，会 **额外** 写一条 `sys_operation_log`（不替代主业务响应）：

| 配置 | 环境变量 | 默认 |
|------|-----------|------|
| `app.shortvideo.ops-alert.write-operation-log` | `SV_OPS_ALERT_WRITE_OPERATION_LOG` | `false` |

- **module**：`shortvideo_ops`
- **action**：`threshold_warn`
- **errorMsg / 摘要**：`completionGap.message` 文案

用于 N-5「订阅告警 MVP」：**运营阈值命中可追溯**；生产若担心刷屏保持默认 `false`，仅在Staging或内网打开。
