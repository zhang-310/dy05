# 启用 Milvus 知识库

创建知识库需要 Milvus 向量数据库，按以下步骤启用。

---

## 一、启动 Milvus 及相关服务

知识库依赖：**Milvus**（向量库）、**Elasticsearch**（全文检索）、**etcd**、**MinIO**。

### 方式 A：使用主 docker-compose（推荐）

```bash
cd c:\claude\dy01
docker compose -f docker/docker-compose.yml --profile ai-builtin up -d
```

Milvus 将映射到主机端口 **19530**。

### 方式 B：使用 AI 独立编排

```bash
cd c:\claude\dy01
docker compose -f docker/docker-compose.ai.yml up -d
```

Milvus 映射到 **19631**，启动时需指定 `MILVUS_PORT=19631`（见下）。

---

## 二、启用 Milvus 配置

### 方式 1：环境变量（推荐）

```bash
# Windows PowerShell
$env:MILVUS_ENABLED="true"
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 若使用 docker-compose.ai.yml（端口 19631）
$env:MILVUS_PORT="19631"
```

### 方式 2：配置文件

在 `application-dev.yml` 或 `application.yml` 中覆盖：

```yaml
app:
  milvus:
    enabled: true
    host: localhost
    port: 19530   # 使用 docker-compose.ai.yml 时改为 19631
```

---

## 三、验证

1. 等待 Milvus 健康检查通过：
   ```bash
   docker ps  # 确认 dy-milvus 或 dy-elasticsearch 等为 healthy
   ```

2. 重启后端后，再次创建知识库，应不再出现 “Milvus 未启用” 错误。

3. Ollama 需已拉取 embedding 模型（若尚未拉取）：
   ```bash
   ollama pull qwen3-embedding:4b
   ```

---

## 四、文档本地归档（可选）

文档入库后默认仅存于 PostgreSQL。若需本地备份，可开启归档：

```yaml
# application.yml 或 application-dev.yml
app:
  ai:
    kb:
      doc-archive-enabled: true
      doc-archive-dir: D:/docs
```

**目录结构**：`{doc-archive-dir}/{yyyy-MM-dd}/{知识库名}/{标题}.md`

- 例：`D:/docs/2026-02-28/douyin/抖音运营技巧.md`
- 手动上传或路径导入均会归档（启用时）
