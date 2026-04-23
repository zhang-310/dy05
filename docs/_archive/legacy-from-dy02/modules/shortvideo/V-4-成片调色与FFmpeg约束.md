# V-4：成片调色与 FFmpeg 约束（非 LUT）

## 当前能力（与对照表 ⚠️ 一致）

- **`eq` 滤镜**：亮度/对比度/饱和度/伽马（`colorBrightness` / `colorContrast` / `colorSaturation` / `colorGamma`），经 `VideoEditServiceImpl#maybeApplyColorGrade` 与 `auto-compose`、工作流 **compose** 步骤透传一致。  
- **可选 `unsharp`**：`colorUnsharpAmount > 0` 时在 `eq` 之后链式锐化；失败时回退为仅 `eq` 成片（见实现日志）。
- **运行时探测（可选）**：`app.shortvideo.compose.ffmpeg-filter-probe-enabled`（`SV_COMPOSE_FFMPEG_FILTER_PROBE_ENABLED`，默认 true）为 true 时，在套用调色前用 `ffmpeg -h filter=eq` / `unsharp` 各探测一次（结果进程内缓存）；若 `eq` 不存在则**整段跳过调色**；若仅缺 `unsharp` 则**跳过锐化**仍可走 `eq`。设为 false 时与旧行为一致（直接执行 FFmpeg，失败时再整段回退）。

## 明确未接入

- **LUT / 电影级分级管线**：不在当前里程碑。  
- **`hue` / `vibrance` 等**：依赖部署环境 FFmpeg 版本与滤镜可用性；若接入需单独评估、默认关闭与回退策略，并同步 `docs/development/04-文档代码同步清单.md` §八。

## 运维自检

```text
ffmpeg -filters | findstr /i "eq unsharp"
```

（Linux/macOS 将 `findstr` 换为 `grep`。）

## hue / vibrance 等旋钮（未默认开启）

上线前若评估 **hue**、**vibrance**、**colorbalance** 等滤镜：

1. 在目标部署环境执行 `ffmpeg -filters`（或 `-h filter=hue` 等）确认滤镜存在且版本一致。  
2. 增加 **集成测试或试运行任务**：滤镜失败时必须 **回退** 到当前 `eq`（±可选 `unsharp`）成片，并打 **WARN**。  
3. 默认 **关闭** 新旋钮，仅对显式请求或配置开启；同步 `docs/development/04-文档代码同步清单.md` §八。
