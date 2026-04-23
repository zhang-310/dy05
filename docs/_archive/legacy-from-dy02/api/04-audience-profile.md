# POST `/api/v1/short-video/dashboard/audience-profile`（N-3）

统一 POST，body 可为 `{}`。返回 **`Map`**，前端 `ShortVideoDashboardPage` 受众卡片与「展开/复制 JSON」同源。

## `dataSource`

| 值 | 含义 |
|----|------|
| `empty` | 未登录或 `ownerId` 为空 |
| `derived_from_publish_engagement` | 本地 **`≥3`** 条带播放量且含 **`publishTime`** 的 `sv_video`，由发布时段/时长分桶与互动率推算 |
| `placeholder` | 样本不足时的示例结构（年龄/性别/地域为占位，**非**开放平台真实画像） |

## 衍生模式（`derived_from_publish_engagement`）主要字段

- `sampleVideoCount`：参与统计的视频条数  
- `engagementByPublishHourSlot`：`hourSlot`（`0-12` / `12-18` / `18-24`）、`sampleCount`、`avgEngagementPercent`  
- `bestPerformingPublishHourSlot`：互动率均值最高的时段键  
- `engagementByDurationBucket`：`durationBucket`（`short_lt_30s` / `mid_30_60s` / `long_ge_60s`）、`sampleCount`、`avgEngagementPercent`  
- `demographicsAvailable`：衍生模式下固定 **`false`**（年龄性别地域仍以开放平台为准）  
- `note`：运营口径说明（非抖音官方受众画像）

## 占位模式（`placeholder`）主要字段

- `sampleVideoCount`：有效样本数（可能为 0）  
- `demographicsAvailable`：**`true`**（仅表示 UI 可展示占位结构）  
- `ageBuckets` / `gender` / `topRegions`：示例分布  

实现：`ShortVideoDashboardServiceImpl#getAudienceProfilePlaceholder`。
