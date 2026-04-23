# Cursor 知识库迁移到 dy01 AI 模块指南

> 将 `C:\opencode\cursor` 中 Cursor Docs 索引的文档，迁移到 dy01 项目 AI 知识库。

---

## 一、迁移原理

| 源（Cursor） | 目标（dy01） |
|--------------|--------------|
| Cursor Docs 索引的**源文档**（本地文件/文件夹） | dy01 `KnowledgeBaseController` |
| 文档格式：.md、.txt、.pdf、.docx、.html 等 | 通过 API 上传 `title` + `content` + `fileType` |
| 索引存储在 Cursor 本地（不可直接导出） | 自动分块 → Milvus 向量 + ES 全文 → 混合检索 |

**关键**：Cursor 不提供索引数据导出。迁移需基于**原始文档源**（你添加到 Cursor Docs 的文件夹/文件）。

---

## 二、前提条件

1. **确认源文档路径**：`C:\opencode\cursor` 或你在 Cursor 设置中配置的 Docs 源路径
2. **dy01 已运行**：后端 + PostgreSQL + Milvus + Elasticsearch + Redis
3. **获取 JWT Token**：登录 dy01 后从浏览器/接口获取，用于 API 调用

---

## 三、dy01 知识库 API 说明

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/ai/knowledge-base/create` | POST | 创建知识库，Body: `{ "name": "xxx", "description": "xxx" }` |
| `/api/v1/ai/knowledge-base/{kbId}/document` | POST | 上传文档，Body: `{ "title": "xxx", "content": "正文", "fileType": "md" }` |
| `/api/v1/ai/knowledge-base/list` | GET | 知识库列表 |
| `/api/v1/ai/knowledge-base/{kbId}/documents` | GET | 文档列表 |

**认证**：请求头需携带 `Authorization: Bearer <JWT>`（与登录接口一致）。

---

## 四、迁移流程

### 步骤 1：在 dy01 创建知识库

使用 Swagger 或 curl：

```bash
# 1. 登录获取 Token
curl -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"你的密码"}'

# 2. 创建知识库（将 <TOKEN> 替换为上一步返回的 token）
curl -X POST "http://localhost:8080/api/v1/ai/knowledge-base/create" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"name":"Cursor迁移知识库","description":"从 C:\\opencode\\cursor 迁移"}'
```

记录返回的 `id` 作为 `kbId`。

### 步骤 2：准备迁移脚本（Python 示例）

在 `C:\opencode\cursor` 或任意目录创建 `migrate_to_dy01.py`：

```python
# migrate_to_dy01.py
import os
import requests
from pathlib import Path

# ===== 配置 =====
SOURCE_DIR = r"C:\opencode\cursor"      # Cursor 文档源路径
DY01_BASE = "http://localhost:8080"      # dy01 服务地址
JWT_TOKEN = "你的JWT_TOKEN"              # 登录后的 token
KB_ID = 1                                # 上一步创建的知识库 ID

# 支持的文件类型及扩展名
SUPPORTED = {
    ".md": "md", ".txt": "text", ".html": "html", ".htm": "html",
    # PDF/DOCX 需要额外解析库，此处仅处理纯文本
}

def read_text_file(path):
    """读取文本文件，忽略二进制"""
    try:
        with open(path, "r", encoding="utf-8", errors="ignore") as f:
            return f.read()
    except Exception as e:
        print(f"[跳过] {path}: {e}")
        return None

def upload_document(title, content, file_type):
    url = f"{DY01_BASE}/api/v1/ai/knowledge-base/{KB_ID}/document"
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {JWT_TOKEN}"
    }
    body = {"title": title, "content": content, "fileType": file_type}
    r = requests.post(url, json=body, headers=headers, timeout=60)
    if r.status_code != 200:
        raise Exception(f"上传失败 {r.status_code}: {r.text}")
    return r.json()

def main():
    count = 0
    for root, _, files in os.walk(SOURCE_DIR):
        for fn in files:
            ext = os.path.splitext(fn)[1].lower()
            if ext not in SUPPORTED:
                continue
            path = os.path.join(root, fn)
            content = read_text_file(path)
            if not content or len(content.strip()) < 10:
                continue
            title = os.path.splitext(fn)[0]
            try:
                upload_document(title, content, SUPPORTED[ext])
                count += 1
                print(f"[OK] {path}")
            except Exception as e:
                print(f"[FAIL] {path}: {e}")
    print(f"\n完成，共导入 {count} 个文档")

if __name__ == "__main__":
    main()
```

### 步骤 3：安装依赖并运行

```bash
pip install requests
python migrate_to_dy01.py
```

### 步骤 4：验证

1. 在 dy01 前端打开「知识库管理」`/admin/ai/knowledge`
2. 检查文档列表是否与源目录一致
3. 使用「混合搜索」测试检索效果

---

## 五、PDF / DOCX 支持（可选）

若 `C:\opencode\cursor` 含 PDF、DOCX，需先转成文本再上传。可增加：

```python
# 需要: pip install pypdf2 python-docx
def read_pdf(path):
    from pypdf2 import PdfReader
    reader = PdfReader(path)
    return "\n".join(p.extract_text() or "" for p in reader.pages)

def read_docx(path):
    from docx import Document
    return "\n".join(p.text for p in Document(path).paragraphs)
```

在 `SUPPORTED` 中增加 `".pdf": "pdf"`, `".docx": "docx"`，并在 `read_text_file` 中按类型分发。

---

## 六、注意事项

| 项 | 说明 |
|----|------|
| **权限** | `userId` 由 JWT 解析，确保登录用户有权限操作知识库 |
| **大文件** | dy01 默认 500 字/块、50 字重叠，超长文档会自动分块 |
| **编码** | 建议源文件使用 UTF-8，脚本已用 `errors="ignore"` 兜底 |
| **速率** | 每文档会触发 Embedding + Milvus + ES 写入，大批量时建议加 `time.sleep(0.5)` 限速 |

---

## 七、快速对照

| Cursor Docs 配置 | 迁移对应 |
|------------------|----------|
| Local folder: `C:\opencode\cursor` | `SOURCE_DIR` |
| 文档列表（Cursor 设置中可见） | 脚本遍历 `SOURCE_DIR` 下的文件 |
| Cursor 索引（本地） | 迁移后由 dy01 重新分块、向量化、建索引 |

迁移完成后，可在 dy01 的「知识库管理」和「AI 进化引擎」中使用这些知识，与抖音运营场景统一管理。
