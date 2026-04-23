# AI 高配机器配置指南

> 适用配置：AMD 5995 / 256G 内存 / 2×RTX 8000 48G / 2×2T M2

---

## 控制台中文乱码（Windows）

若控制台中文显示乱码，任选其一：

1. **Cursor / VS Code**：已配置 `.vscode/launch.json` 和 `settings.json`，用 **运行/调试** 启动即可；或新开终端后执行 `mvn spring-boot:run`
2. **命令行**：先执行 `chcp 65001`，再启动应用
3. **脚本**：使用 `run.bat` 或 `run.ps1` 启动（已自动设置 UTF-8）
4. **其他 IDE**：在运行配置中添加 VM 参数 `-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8`

---

## 一、快速启用

### 方式 1：激活 highperf 配置（推荐）

```bash
# 开发环境
mvn spring-boot:run -Dspring-boot.run.profiles=dev,highperf

# 或设置环境变量
export SPRING_PROFILES_ACTIVE=dev,highperf
```

### 方式 2：环境变量覆盖

```bash
# AI 额度：每日 5000 次
export AI_QUOTA_DAILY_MAX=5000

# 进化：可选频率
# 每天 3 次：export EVOLVE_CRON="0 0 6,14,22 * * ?"
# 每小时 1 次：export EVOLVE_CRON="0 0 * * * ?"
# 每 10 分钟 1 次：export EVOLVE_CRON="0 */10 * * * ?"
export EVOLVE_CRON="0 0 * * * ?"

# 深度进化：周三+周日，每次 12 个
export DEEP_EVOLVE_CRON="0 30 6 * * WED,SUN"
export DEEP_EVOLVE_MAX_PER_RUN=12

# Embedding 并行（64 核）
export AI_EMBEDDING_PARALLELISM=16
export AI_EMBEDDING_BATCH_SIZE=32
export AI_KB_IMPORT_PARALLELISM=16
```

---

## 二、配置项说明

| 配置项 | 默认 | 高配 | 说明 |
|--------|------|------|------|
| `app.ai.quota.daily-max` | 100 | 5000 | 每日每用户 AI 调用上限 |
| `app.ai.evolve.cron` | 每天 6:00 | 每小时 | 进化调度 |
| `EVOLVE_CRON` 示例 | — | `0 0 * * * ?` 每小时 | `0 */10 * * * ?` 每 10 分钟 |
| `app.ai.deep-evolve.cron` | 每周日 6:30 | 周三+周日 6:30 | 深度进化 |
| `app.ai.deep-evolve.max-per-run` | 3 | 12 | 每次处理待深化问题数 |
| `AI_EMBEDDING_PARALLELISM` | 4 | 16 | Embedding 批并行 |
| `AI_EMBEDDING_BATCH_SIZE` | 16 | 32 | 每批 token 数 |
| `AI_KB_IMPORT_PARALLELISM` | 6 | 16 | 文档导入并行 |
| `app.rate-limit.api-per-minute` | 100 | 500 | API 限流（高配可承受更高 QPS） |
| `app.ai.index-queue.consumer.interval-ms` | 300000 | 60000 | 索引队列消费间隔（5 分钟 → 1 分钟） |
| `app.ai.index-queue.consumer.batch-size` | 5 | 20 | 索引队列每轮处理条数 |
| `app.async.evolve-core/max` | 2/4 | 4/8 | 进化任务线程池 |
| `app.async.ai-core/max` | 4/8 | 8/16 | AI 任务线程池 |
| `app.async.index-core/max` | 2/4 | 4/8 | 索引任务线程池 |

---

## 三、今日额度已创建用户的更新

若今日已产生额度记录（max_count=100），需手动更新：

```sql
UPDATE ai_call_quota SET max_count = 5000 WHERE quota_date = CURRENT_DATE;
```

或等待次日自动使用新额度。

---

## 四、管理员后台动态控制（推荐）

上述「进一步调高」参数可在 **系统配置** 中动态修改，无需重启：

1. 登录管理后台 → **系统配置** → 选择 **AI** 选项卡
2. 编辑以下配置键（若无则新增，config_group=ai）：

| 配置键 | 说明 | 示例值 |
|--------|------|--------|
| `ai.index-queue.consumer.interval-ms` | 索引队列消费间隔（毫秒） | 30000（30秒） |
| `ai.index-queue.consumer.batch-size` | 索引队列每轮处理条数 | 30 |
| `ai.evolve.interval-minutes` | 知识进化执行间隔（分钟） | 10（每10分钟） |
| `ai.dual-write.compensation.batch-size` | 双写补偿每轮处理条数 | 20 |
| `ai.quota.daily-max` | 每日每用户 AI 额度 | 5000 |

首次使用需执行 `sql/config/ai-runtime-config.sql` 初始化默认值。

---

## 五、Ollama 多卡配置（可选）

2×RTX 8000 可让 Ollama 使用多卡：

```bash
# 设置可见 GPU
export CUDA_VISIBLE_DEVICES=0,1

# 启动 Ollama（会自动利用多卡）
ollama serve
```

或使用 `OLLAMA_NUM_GPU=2` 等环境变量（视 Ollama 版本而定）。
