# 06 性能优化分析

## 1. N+1 查询与全表扫描

| 文件 | 行号 | 问题 | 严重性 |
|------|------|------|--------|
| `ai/service/impl/AiAdminInfraServiceImpl.java` | 384 | findAll() + 循环 getMilvusStats | P0 |
| `live/service/impl/EffectivenessScoreServiceImpl.java` | 296 | findAll() 内存过滤 | P0 |
| `ai/service/impl/EvolutionServiceImpl.java` | 415 | findAll().stream().filter().count() | P0 |
| `storage/service/impl/BosCleanupServiceImpl.java` | 32 | findAll() + 时间内存过滤 | P0 |
| `ai/config/AiViralDetectionScheduler.java` | 63-82 | 两次 findAll() + 循环 findById | P0 |
| `live/service/impl/LiveProductServiceImpl.java` | 118-131 | 循环 findById + save | P1 |

## 2. 循环 save 改 saveAll

| 文件 | 行号 |
|------|------|
| `abtest/service/impl/AbTestServiceImpl.java` | 102-105, 118-121 |
| `attribution/service/impl/AttributionServiceImpl.java` | 87, 110 |

## 3. 配置优化

### HikariCP（当前偏保守）
```yaml
# 当前 → 建议
maximum-pool-size: 20 → 40
minimum-idle: 5 → 10
batch_size: 20 → 100
fetch_size: 100 → 500
```

### 异步线程池（规模不足）
```
evolveTaskExecutor: core=2,max=4 → core=4,max=8
indexTaskExecutor: core=2,max=4 → core=4,max=8
```

## 4. 缓存缺失

- 脚本搜索建议（高频查询，无 @Cacheable）
- Dashboard 缓存 TTL 仅 5 分钟

## 5. 前端性能

- DataGrid autoHeight 有性能代价
- 图片缺少 loading="lazy"
- 部分页面缺少 React.memo/useMemo/useCallback
