# 知识库 DOC/PDF 批量导入 + 话术 RAG 集成规划

> 规划日期：2026-03-04
> 需求来源：用户购买了话术文案库（doc/pdf 格式），需要导入知识库并与话术生成联动
> 相关模块：ai（知识库）、product（产品话术）、live（直播话术）

---

## 0. 执行摘要

### 0.1 一句话定位

**支持 DOC/DOCX/PDF 格式的话术文档批量导入知识库，自动去重，并在 AI 话术生成时通过 RAG 检索注入参考素材，提升生成质量。**

### 0.2 当前现状

| 能力 | 状态 | 说明 |
|------|------|------|
| 知识库 RAG 架构 | ✅ 已有 | Milvus + ES 双引擎，RRF 融合检索，完整 |
| 文件导入 | ⚠️ 仅 MD/TXT | `KnowledgeBaseImportServiceImpl` 只处理 `.md`/`.txt` |
| DOC/DOCX 解析 | ⚠️ 依赖已有 | `poi-ooxml 5.2.5` 已在 pom.xml（用于 Excel），可复用解析 DOCX |
| PDF 解析 | ❌ 无 | pom.xml 中无 PDFBox 依赖 |
| 内容去重 | ⚠️ 仅标题 | `existsByKbIdAndTitleAndDeleted`，无内容/语义去重 |
| 话术 RAG 注入 | ❌ 未集成 | `LiveAiService`、`ProductScriptService` 均未调用 `hybridSearch()` |
| 文件上传 API | ❌ 无 | 当前只接收 `{title, content}` 纯文本，不支持 MultipartFile |
| 文件安全校验 | ❌ 无 | 无 magic bytes 验证，无内容安全扫描 |
| 扫描件/加密 PDF 检测 | ❌ 无 | 解析为空内容时无诊断 |
| 导入报告 | ❌ 无 | 仅内存中临时统计，无持久化 |

### 0.3 关键决策

| # | 决策项 | 决策 | 理由 |
|---|--------|------|------|
| D1 | PDF 解析库 | Apache PDFBox 3.0.x | 成熟稳定，Apache 生态统一 |
| D2 | DOC 解析 | POI poi-ooxml（已有）+ poi-scratchpad（旧版 .doc） | 复用现有依赖 |
| D3 | 话术文档分块策略 | 智能分隔符检测优先，固定长度兜底 | 话术是独立条目，不应截断 |
| D4 | 去重方案 | 文档级快速预检 + 导入时向量相似度 > 0.92 跳过 + 检索时结果语义去重 | 三级保险 |
| D5 | RAG 注入位置 | Prompt 末尾追加参考案例区块 | 不改变主 Prompt 结构，低风险 |
| D6 | 专用知识库 | 新建 `huashu`（话术库）| 与 douyin/zhishi 隔离，检索精准 |
| D7 | 文档类型检测 | 自动检测优先，手动选择兜底 | 减少用户操作，提高准确性 |
| D8 | RAG 检索策略 | 复用 queryRewriteService 语义改写 + 品类维度扩展 + 最低相关度过滤 | 避免与现有子查询重复实现 |
| D9 | 文件安全 | magic bytes 校验 + 内容安全扫描 | 防止伪装文件和敏感内容入库 |
| D10 | RAG 缓存策略 | 检索时跳过 Redis 缓存 + 导入后主动清除知识库缓存 | 确保新导入文档立即可检索 |
| D11 | 多层级目录导入 | 递归遍历 + 深度限制 + 隐藏目录过滤 + 目录结构元数据保留 + 目录级错误隔离 | 话术文档库按品类/场景分目录组织，需完整支持 |

---

## 一、依赖变更

### 1.1 pom.xml 新增

```xml
<!-- PDF 解析 -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.3</version>
</dependency>

<!-- 旧版 .doc 格式支持（poi-ooxml 5.2.5 已有，补 scratchpad） -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-scratchpad</artifactId>
    <version>5.2.5</version>
</dependency>

<!-- Apache Tika（可选，用于 magic bytes 文件类型检测） -->
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
    <version>2.9.1</version>
</dependency>
```

**说明：** `poi-ooxml 5.2.5` 已存在于 pom.xml（第 117-122 行，用于排品表 Excel 导入），可直接用于 `.docx` 解析。`poi-scratchpad` 补充旧版 `.doc` 二进制格式支持。`tika-core` 仅用于 magic bytes 检测（轻量，不含解析器）。

---

## 二、文档解析器

### 2.1 新建 DocumentParser 工具类

**文件：** `src/main/java/cn/gaifan/douyinOperations/module/ai/util/DocumentParser.java`

```java
public class DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(DocumentParser.class);

    /**
     * 根据文件扩展名自动选择解析器，提取纯文本
     */
    public static String parse(Path file) throws IOException {
        // [升级] 先做 magic bytes 校验
        validateFileType(file);

        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".docx")) {
            return parseDocx(file);
        } else if (name.endsWith(".doc")) {
            return parseDoc(file);
        } else if (name.endsWith(".pdf")) {
            return parsePdf(file);
        } else {
            // md / txt 保持原有逻辑
            return Files.readString(file, StandardCharsets.UTF_8);
        }
    }

    /**
     * 从 InputStream 解析（用于文件上传场景）
     */
    public static String parse(InputStream is, String fileType) throws IOException { ... }

    /**
     * [升级] 解析结果，包含文本内容 + 元数据
     */
    public static ParseResult parseWithMetadata(Path file) throws IOException {
        validateFileType(file);
        String name = file.getFileName().toString().toLowerCase();
        String text;
        Map<String, String> metadata = new HashMap<>();

        if (name.endsWith(".docx")) {
            text = parseDocxWithMetadata(file, metadata);
        } else if (name.endsWith(".doc")) {
            text = parseDocWithMetadata(file, metadata);
        } else if (name.endsWith(".pdf")) {
            text = parsePdfWithMetadata(file, metadata);
        } else {
            text = Files.readString(file, StandardCharsets.UTF_8);
        }

        return new ParseResult(text, metadata);
    }

    /** DOCX：Apache POI XWPFDocument */
    private static String parseDocx(Path file) throws IOException {
        // [升级] 加密文件检测
        try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(file))) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = para.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text.trim()).append("\n");
                }
            }
            // 表格内容也提取
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String text = cell.getText();
                        if (text != null && !text.isBlank()) {
                            sb.append(text.trim()).append("\t");
                        }
                    }
                    sb.append("\n");
                }
            }
            return sb.toString().trim();
        } catch (EncryptedDocumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                "文件已加密，请先解除密码保护后再导入: " + file.getFileName());
        }
    }

    /**
     * [升级] DOCX 带元数据提取
     */
    private static String parseDocxWithMetadata(Path file, Map<String, String> metadata) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(file))) {
            // 提取文档属性
            POIXMLProperties.CoreProperties core = doc.getProperties().getCoreProperties();
            if (core.getTitle() != null) metadata.put("title", core.getTitle());
            if (core.getCreator() != null) metadata.put("author", core.getCreator());
            if (core.getDescription() != null) metadata.put("description", core.getDescription());
            if (core.getKeywords() != null) metadata.put("keywords", core.getKeywords());
            metadata.put("pageCount", String.valueOf(doc.getProperties().getExtendedProperties()
                .getUnderlyingProperties().getPages()));

            // 正文提取逻辑同 parseDocx
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = para.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text.trim()).append("\n");
                }
            }
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String text = cell.getText();
                        if (text != null && !text.isBlank()) {
                            sb.append(text.trim()).append("\t");
                        }
                    }
                    sb.append("\n");
                }
            }
            return sb.toString().trim();
        } catch (EncryptedDocumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                "文件已加密，请先解除密码保护后再导入: " + file.getFileName());
        }
    }

    /** DOC（旧版二进制）：Apache POI HWPFDocument */
    private static String parseDoc(Path file) throws IOException {
        try (HWPFDocument doc = new HWPFDocument(Files.newInputStream(file))) {
            return doc.getDocumentText();
        } catch (EncryptedDocumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                "文件已加密，请先解除密码保护后再导入: " + file.getFileName());
        }
    }

    /**
     * [升级] DOC 带元数据提取
     */
    private static String parseDocWithMetadata(Path file, Map<String, String> metadata) throws IOException {
        try (HWPFDocument doc = new HWPFDocument(Files.newInputStream(file))) {
            SummaryInformation si = doc.getSummaryInformation();
            if (si != null) {
                if (si.getTitle() != null) metadata.put("title", si.getTitle());
                if (si.getAuthor() != null) metadata.put("author", si.getAuthor());
                if (si.getKeywords() != null) metadata.put("keywords", si.getKeywords());
                metadata.put("pageCount", String.valueOf(si.getPageCount()));
            }
            return doc.getDocumentText();
        } catch (EncryptedDocumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                "文件已加密，请先解除密码保护后再导入: " + file.getFileName());
        }
    }

    /** PDF：Apache PDFBox（[P1 修正] 使用 PDFBox 3.x 推荐的 RandomAccessReadBufferedFile） */
    private static String parsePdf(Path file) throws IOException {
        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBufferedFile(file))) {
            // [升级] 加密 PDF 检测
            if (doc.isEncrypted()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                    "PDF 文件已加密，请先解除密码保护后再导入: " + file.getFileName());
            }
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc).trim();

            // [升级] 扫描件检测：提取文字过少但页数较多 → 疑似扫描件
            if (text.length() < 20 && doc.getNumberOfPages() > 0) {
                log.warn("PDF 疑似扫描件（图片型），提取文字仅 {} 字符，共 {} 页: {}",
                    text.length(), doc.getNumberOfPages(), file.getFileName());
                throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                    "PDF 疑似扫描件（图片型），无法提取文字。建议先用 OCR 工具转换为文字版: " + file.getFileName());
            }

            // [升级] 每页文字密度检查（辅助判断部分页面为扫描件）
            int pages = doc.getNumberOfPages();
            if (pages > 1 && text.length() < pages * 50) {
                log.warn("PDF 文字密度过低（平均每页 {} 字符），可能含扫描页: {}",
                    text.length() / pages, file.getFileName());
            }

            return text;
        } catch (InvalidPasswordException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                "PDF 文件需要密码，请先解除密码保护后再导入: " + file.getFileName());
        }
    }

    /**
     * [升级] PDF 带元数据提取
     */
    private static String parsePdfWithMetadata(Path file, Map<String, String> metadata) throws IOException {
        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBufferedFile(file))) {
            if (doc.isEncrypted()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                    "PDF 文件已加密: " + file.getFileName());
            }

            // 提取 PDF 元数据
            PDDocumentInformation info = doc.getDocumentInformation();
            if (info.getTitle() != null) metadata.put("title", info.getTitle());
            if (info.getAuthor() != null) metadata.put("author", info.getAuthor());
            if (info.getKeywords() != null) metadata.put("keywords", info.getKeywords());
            if (info.getSubject() != null) metadata.put("subject", info.getSubject());
            if (info.getCreator() != null) metadata.put("creator", info.getCreator());
            metadata.put("pageCount", String.valueOf(doc.getNumberOfPages()));

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc).trim();

            if (text.length() < 20 && doc.getNumberOfPages() > 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                    "PDF 疑似扫描件: " + file.getFileName());
            }
            return text;
        } catch (InvalidPasswordException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                "PDF 文件需要密码: " + file.getFileName());
        }
    }

    /**
     * [升级] Magic Bytes 文件类型校验
     * 防止伪装文件（如 .exe 重命名为 .pdf）
     */
    private static void validateFileType(Path file) throws IOException {
        String name = file.getFileName().toString().toLowerCase();
        byte[] header = new byte[8];
        try (InputStream is = Files.newInputStream(file)) {
            int read = is.read(header);
            if (read < 4) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "文件过小或为空: " + name);
            }
        }

        if (name.endsWith(".pdf")) {
            // PDF magic: %PDF (0x25504446)
            if (header[0] != 0x25 || header[1] != 0x50 || header[2] != 0x44 || header[3] != 0x46) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                    "文件扩展名为 .pdf 但内容不是有效的 PDF 格式: " + name);
            }
        } else if (name.endsWith(".doc") || name.endsWith(".docx")) {
            // OLE2/ZIP magic: D0CF11E0 (doc) 或 504B0304 (docx/zip)
            boolean isOle2 = (header[0] & 0xFF) == 0xD0 && (header[1] & 0xFF) == 0xCF
                          && (header[2] & 0xFF) == 0x11 && (header[3] & 0xFF) == 0xE0;
            boolean isZip = header[0] == 0x50 && header[1] == 0x4B
                         && header[2] == 0x03 && header[3] == 0x04;
            if (!isOle2 && !isZip) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                    "文件扩展名为 " + name.substring(name.lastIndexOf('.'))
                    + " 但内容不是有效的 Word 文档格式: " + name);
            }
        }
        // .md/.txt 为纯文本，不做 magic bytes 校验
    }

    /**
     * 检测文件类型字符串
     */
    public static String detectFileType(String filename) {
        String name = filename.toLowerCase();
        if (name.endsWith(".md")) return "md";
        if (name.endsWith(".docx")) return "docx";
        if (name.endsWith(".doc")) return "doc";
        if (name.endsWith(".pdf")) return "pdf";
        return "text";
    }

    /**
     * 解析结果（内容 + 元数据）
     */
    public record ParseResult(String text, Map<String, String> metadata) {}
}
```

### 2.2 扫描件检测策略（升级项 #1）

| 检测维度 | 条件 | 处理 |
|----------|------|------|
| 全文为空 | `text.length() == 0` 且 `pages > 0` | 报错：疑似扫描件 |
| 文字极少 | `text.length() < 20` 且 `pages > 0` | 报错：疑似扫描件 |
| 文字密度低 | `text.length() / pages < 50` | 警告日志，继续导入 |
| 正常 | 其他 | 正常导入 |

**后续可选扩展：** 集成 Tesseract OCR 自动识别扫描件文字（需额外依赖 `tess4j`），但当前阶段先做检测提示即可。

### 2.3 加密文件处理策略（升级项 #2）

| 格式 | 检测方式 | 处理 |
|------|----------|------|
| PDF | `doc.isEncrypted()` + `InvalidPasswordException` | 抛出明确提示 |
| DOCX | `EncryptedDocumentException` | 抛出明确提示 |
| DOC | `EncryptedDocumentException` | 抛出明确提示 |

所有加密文件统一提示："文件已加密，请先解除密码保护后再导入"。在批量导入场景中，加密文件计入 `failed` 而非崩溃。

### 2.4 文档元数据提取（升级项 #3）

**新增 `parseWithMetadata()` 方法**，在解析文本的同时提取文档属性：

| 元数据字段 | DOCX 来源 | DOC 来源 | PDF 来源 |
|-----------|-----------|----------|----------|
| title | CoreProperties.getTitle() | SummaryInformation.getTitle() | PDDocumentInformation.getTitle() |
| author | CoreProperties.getCreator() | SummaryInformation.getAuthor() | PDDocumentInformation.getAuthor() |
| keywords | CoreProperties.getKeywords() | SummaryInformation.getKeywords() | PDDocumentInformation.getKeywords() |
| pageCount | ExtendedProperties.getPages() | SummaryInformation.getPageCount() | getNumberOfPages() |
| description | CoreProperties.getDescription() | - | PDDocumentInformation.getSubject() |

**用途：**
- `title` → 可用于覆盖文件名作为文档标题
- `keywords` → 用于自动分类和 chunk 标签
- `author` → 记录来源信息
- `pageCount` → 扫描件检测的辅助依据

**存储：** 元数据写入 `ai_kb_document` 表新增的 `metadata` JSON 字段（见第九节数据库变更）。

---

## 三、话术文档智能分块

### 3.1 问题

现有分块策略（512 字符 + 50 重叠）适合长文章，但**话术文档**的特点是：
- 每条话术 100-400 字，是独立完整的内容单元
- 按固定 512 字符切会把一条话术截成两半，或把两条话术混在一起
- 影响检索精度：检索到半条话术毫无参考价值

### 3.2 话术专用分块策略

**新建** `ScriptAwareChunker`，在 `uploadDocument()` 中根据 `fileType` 选择分块策略：

```
分块决策逻辑：
  contentType == "script" (用户手动指定)
    → ScriptAwareChunker.split()
  contentType == "auto" 或未指定
    → [升级] DocumentTypeDetector.detect() 自动检测
    → 检测结果为 "script" → ScriptAwareChunker.split()
    → 检测结果为 "mixed"  → [升级] MixedDocumentProcessor.process()
    → 检测结果为 "general" → 现有 splitIntoChunks()（512字符通用分块）
```

**ScriptAwareChunker 分块规则：**

```
输入：纯文本内容
  ↓
Step 1：检测分隔符模式
  - 数字序号：  /^\d+[.、）)]/m       → "1. xxx" "2、xxx"
  - 标题标记：  /^[【\[#]/m           → "【话术一】" "# 话术标题"
  - 空行分隔：  /\n\s*\n/             → 连续空行
  - 分隔线：    /^[-=]{3,}/m          → "---" "==="
  ↓
Step 2：按检测到的主分隔符切分
  - 优先级：数字序号 > 标题标记 > 分隔线 > 空行
  ↓
Step 3：验证每个块
  - 太短（< 30字）→ 与下一块合并
  - 太长（> 1000字）→ 回退到 512 字符分块
  - 正常（30-1000字）→ 作为独立 chunk
  ↓
Step 4：[升级] 为每个 chunk 自动打标签（见 3.5）
  ↓
输出：List<ChunkResult> chunks（每条是一段完整话术 + 标签）
```

**示例：**

输入文档内容：
```
1、姐妹们这套水乳套装单买要398，今天套盒价只要198！...（200字）

2、宝子们看这个精华液，烟酰胺含量5%...（180字）

3、家人们注意了，库存只有50套...（150字）
```

现有分块（512字符）：会把 1 和 2 的一半混在一起
话术分块（按序号）：精确切出 3 个独立 chunk，每条完整话术

### 3.3 文件类型标记

**KbImportVO 新增字段：**

```java
private String contentType;  // "general"(默认) | "script"(话术文档) | "auto"(自动检测)
```

**上传接口也支持该参数，控制分块策略。**

### 3.4 [升级] 文档类型自动检测（升级项 #4，P0）

**新建** `DocumentTypeDetector.java`

```java
public class DocumentTypeDetector {

    /**
     * [P1 修正] 话术关键词配置化：从 application.yml 加载，支持多行业扩展
     * 默认关键词仅为兜底，实际部署时可在管理后台维护（sys_config 表，key = kb.script.keywords）
     */
    private final List<String> scriptKeywords;

    public DocumentTypeDetector(
            @Value("${app.ai.kb.detect.script-keywords:}") String configKeywords) {
        if (configKeywords != null && !configKeywords.isBlank()) {
            this.scriptKeywords = Arrays.asList(configKeywords.split(","));
        } else {
            // 兜底默认关键词（覆盖美妆/食品/服装/家居/数码等行业）
            this.scriptKeywords = List.of(
                // 通用直播话术
                "姐妹们", "宝子们", "家人们", "下单", "秒杀", "限时",
                "抢购", "套盒", "套装", "宝贝", "链接", "库存", "倒计时", "上车",
                // [P1 修正] 新增行业关键词，减少行业偏向
                "穿搭", "面料", "尺码",       // 服装
                "智能", "续航", "芯片",       // 数码
                "软装", "收纳", "家居",       // 家居
                "零食", "口感", "配料"        // 食品
            );
        }
    }

    /**
     * 自动检测文档类型：script（话术文档）、mixed（混合文档）、general（通用文档）
     */
    public static String detect(String content) {
        if (content == null || content.isBlank()) return "general";

        int scriptSignals = 0;
        int totalSignals = 0;

        // 1. 分隔符模式检测
        int numberedItems = countPattern(content, "(?m)^\\d+[.、）)]");
        int bracketTitles = countPattern(content, "(?m)^[【\\[#]");
        int separators = countPattern(content, "(?m)^[-=]{3,}");

        if (numberedItems >= 3 || bracketTitles >= 3) {
            scriptSignals += 2;
        }
        totalSignals += 2;

        // 2. 段落长度分布检测
        String[] paragraphs = content.split("\\n\\s*\\n");
        int shortParas = 0; // 30-500字
        int longParas = 0;  // >500字
        for (String p : paragraphs) {
            int len = p.trim().length();
            if (len >= 30 && len <= 500) shortParas++;
            if (len > 500) longParas++;
        }
        if (shortParas >= 3 && longParas == 0) {
            scriptSignals += 2;
        } else if (shortParas >= 3 && longParas > 0) {
            scriptSignals += 1; // 混合信号
        }
        totalSignals += 2;

        // 3. [P1 修正] 话术关键词密度（从配置加载，而非硬编码）
        int keywordHits = 0;
        for (String kw : scriptKeywords) {
            if (content.contains(kw.trim())) keywordHits++;
        }
        if (keywordHits >= 3) scriptSignals += 2;
        else if (keywordHits >= 1) scriptSignals += 1;
        totalSignals += 2;

        // 4. 综合判断
        double ratio = (double) scriptSignals / totalSignals;
        if (ratio >= 0.7) return "script";
        if (ratio >= 0.4) return "mixed";
        return "general";
    }

    private static int countPattern(String text, String regex) {
        return (int) Pattern.compile(regex).matcher(text).results().count();
    }
}
```

**自动检测触发条件：**
- `contentType` 参数为 `"auto"` 或未传递时
- 自动检测结果优先级低于用户手动指定

### 3.5 [升级] 混合文档分段处理（升级项 #5）

**新建** `MixedDocumentProcessor.java`

当文档被检测为 "mixed" 类型时（既有话术又有说明文字），按区域分段处理：

```
输入：混合文档文本
  ↓
Step 1：按一级分隔符切分大区块
  - 用 "# 标题" / "【标题】" / 分隔线 切分
  ↓
Step 2：对每个区块独立检测类型
  - 话术区块 → ScriptAwareChunker.split()
  - 说明区块 → splitIntoChunks()（512字符通用）
  ↓
Step 3：标记每个 chunk 的来源区块
  - chunk.sectionTitle = "促销话术"
  - chunk.sectionType = "script" / "general"
  ↓
输出：List<ChunkResult> 混合分块结果
```

**示例场景：**

```
# 产品说明
这款水乳套装采用了xxx技术...（800字说明文）

# 直播话术
1、姐妹们这套水乳套装...（200字）
2、宝子们看这个...（180字）

# 售后FAQ
Q: 过敏怎么办？A: ...（500字）
```

处理后：产品说明按 512 字符分块，直播话术按条目分块，FAQ 按 Q&A 对分块。

### 3.6 [升级] Chunk 业务标签自动打标（升级项 #6）

**新增** `ChunkLabeler.java`

在分块完成后，为每个 chunk 自动生成业务标签，存入 Milvus/ES 的 metadata 字段：

```java
/**
 * [P1 修正] 三级标签体系，与 classifyByContent() 的分类逻辑对齐：
 *   一级标签：知识库归属（douyin / zhishi / huashu）— 由 autoClassify 决定，ChunkLabeler 不重复处理
 *   二级标签：内容类型（种草 / 促销 / 产品介绍 / 情绪价值 / 过渡 / 互动 / FAQ）
 *   三级标签：品类（美妆护肤 / 食品 / 服装 / 家居 / 数码）
 *
 * 与 classifyByContent() 的关系：
 *   - classifyByContent() 负责一级分类（决定文档归入哪个知识库）
 *   - ChunkLabeler 负责二级 + 三级标签（chunk 粒度的精细标注）
 *   - 两者共享品类关键词配置，避免标签分裂
 */
public class ChunkLabeler {

    /** 为 chunk 自动打标签（返回二级 + 三级标签集合） */
    public static Set<String> label(String chunkText) {
        Set<String> labels = new HashSet<>();

        // === 二级标签：内容类型 ===
        if (matchAny(chunkText, "种草", "安利", "推荐理由", "真的好用")) labels.add("type:种草");
        if (matchAny(chunkText, "秒杀", "限时", "倒计时", "抢购", "下单", "福利价")) labels.add("type:促销");
        if (matchAny(chunkText, "成分", "功效", "配方", "技术", "参数", "规格")) labels.add("type:产品介绍");
        if (matchAny(chunkText, "姐妹们", "宝子们", "家人们", "直播间")) labels.add("type:直播话术");
        if (matchAny(chunkText, "情绪", "共鸣", "故事", "经历", "感动")) labels.add("type:情绪价值");
        if (matchAny(chunkText, "过渡", "衔接", "接下来", "下一个")) labels.add("type:过渡话术");
        if (matchAny(chunkText, "感谢", "关注", "点赞", "粉丝", "评论")) labels.add("type:互动话术");
        if (matchAny(chunkText, "Q:", "A:", "问:", "答:", "常见问题")) labels.add("type:FAQ");

        // === 三级标签：品类（与 classifyByContent 共享关键词） ===
        if (matchAny(chunkText, "护肤", "面膜", "精华", "水乳", "防晒", "美白")) labels.add("cat:美妆护肤");
        if (matchAny(chunkText, "零食", "美食", "好吃", "口感", "配料")) labels.add("cat:食品");
        if (matchAny(chunkText, "衣服", "穿搭", "面料", "款式", "尺码")) labels.add("cat:服装");
        if (matchAny(chunkText, "家电", "智能", "厨房", "清洁", "收纳")) labels.add("cat:家居");
        if (matchAny(chunkText, "手机", "电脑", "芯片", "续航", "像素")) labels.add("cat:数码");

        return labels;
    }

    private static boolean matchAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
```

**标签命名规范：**
- 二级标签前缀 `type:`，三级标签前缀 `cat:`
- 一级标签由 `classifyByContent()` 负责，决定文档归入哪个知识库（douyin/zhishi/huashu）
- 前端展示时去掉前缀，如 `type:促销` 显示为 "促销"

**标签用途：**
- 存入 Milvus 的 metadata 字段 → 用于 RAG 检索时的元数据过滤（如 `type:促销 AND cat:美妆护肤`）
- 存入 ES 的 `labels` 字段 → 用于前端分类浏览
- 支持 RAG 按标签精准检索：如只从 `type:促销` 中检索参考
- [P1 修正] 与 classifyByContent() 统一标签体系，避免标签分裂

---

## 四、内容去重机制

### 4.1 [升级] 文档级快速预检（升级项 #7）

**改动位置：** `KnowledgeBaseServiceImpl.uploadDocument()` 方法入口

在做分块之前，先对整文档做快速预检，避免白白解析+分块：

```
uploadDocument() 入口：
  ↓
Step 0：文档级快速预检
  a. 标题去重（已有）：existsByKbIdAndTitleAndDeleted()
  b. [新增] 内容指纹去重：
     - fingerprint = MD5(content.trim().replaceAll("\\s+", ""))
     - 查询：existsByKbIdAndContentFingerprint(kbId, fingerprint)
     - 完全一致 → 直接跳过，不进入分块流程
  c. [新增] SimHash 近似去重：
     - [P1 修正] 短文本保护：content.length() < simhashMinLength(默认 200) 时跳过 SimHash
       话术文档通常 100-400 字/条，短文本特征太少、SimHash 碰撞率高
       短文本直接走 MD5 指纹 + chunk 级向量去重（更精确）
     - simhash = SimHash.compute(content)
     - 查询已有文档的 simhash，海明距离 < 3 → 标记为疑似重复
     - 疑似重复 → 继续进入 chunk 级去重（精细判断）
  ↓
Step 1（原有）：splitIntoChunks() / ScriptAwareChunker.split()
  ↓
Step 2：chunk 级去重（见 4.2）
```

**新增字段（ai_kb_document 表）：**

```sql
ALTER TABLE ai_kb_document ADD COLUMN content_fingerprint VARCHAR(64);
ALTER TABLE ai_kb_document ADD COLUMN simhash BIGINT;
CREATE INDEX idx_kb_doc_fingerprint ON ai_kb_document(kb_id, content_fingerprint);
CREATE INDEX idx_kb_doc_simhash ON ai_kb_document(kb_id, simhash);
```

**性能优势：** 文档级预检在毫秒内完成，避免完全重复文档的分块+向量化开销（每文档可省 1-5 秒）。

### 4.2 导入时 chunk 级去重（入口把关）

**改动位置：** `KnowledgeBaseServiceImpl.uploadDocument()` 方法

在分块完成后、插入 Milvus 前，对每个 chunk 做相似度检查：

```
splitIntoChunks() 得到 chunks[]
  ↓
对每个 chunk:
  1. embedding = vectorService.generateEmbedding(chunk)
  2. results = vectorService.search(kbCollection, embedding, topK=1)
  3. 判断：
     score > 0.95  → SKIP（几乎一字不差，丢弃该 chunk）
     score > 0.92  → SKIP 并记录日志（高度相似变体）
     score > 0.80  → KEEP 但设 boost_factor = 0.6（近似内容降权）
     score <= 0.80 → KEEP（新内容，正常入库）
  ↓
过滤后的 chunks → 批量插入 Milvus + ES
```

**阈值配置化：**

```yaml
# application.yml
app.ai.kb:
  dedup:
    enabled: true
    skip-threshold: 0.92      # 高于此值跳过
    downweight-threshold: 0.80 # 高于此值降权
    downweight-factor: 0.6    # 降权系数
    doc-fingerprint: true      # [升级] 文档级 MD5 指纹预检
    doc-simhash: true          # [升级] 文档级 SimHash 近似预检
    simhash-distance: 3        # [升级] SimHash 海明距离阈值
    simhash-min-length: 200    # [P1 修正] SimHash 最低文本长度，短于此值跳过 SimHash
```

**性能考虑：**
- 每个 chunk 多一次 Milvus 查询（~10ms）
- 100 个 chunk 多 1 秒，可接受
- 可通过 `dedup.enabled=false` 关闭（大批量首次导入时）

### 4.3 [升级] 去重批量查询优化（升级项 #9）

当 chunk 数量较大时，逐条查询 Milvus 效率不高。优化方案：

**[Cursor 修正] 降级策略：** 当前 `VectorServiceImpl.search()` 只支持单向量查询，无批量搜索 API。实施时按如下降级路径：
1. **Phase 2 先用逐条去重**（复用现有 `vectorService.search(collection, embedding, 1, null)`），每 chunk ~10ms，100 chunk ~1s，可接受
2. **后续优化时**封装 `VectorService.batchSearch()` 方法（底层使用 `milvusClient.search(SearchParam.withVectors(List))` 实现批量 RPC）
3. 通过配置 `dedup.batch-enabled` 控制是否启用批量模式，默认 false

```java
/**
 * 批量去重：一次性将所有 chunk embedding 提交给 Milvus 批量搜索
 */
private List<ChunkDedupResult> batchDedup(List<String> chunks, String collection) {
    // 1. 批量生成 embedding（已有 batchGenerateEmbeddings 方法）
    List<float[]> embeddings = vectorService.batchGenerateEmbeddings(chunks);

    // 2. Milvus 批量搜索（单次 RPC 调用，减少网络往返）
    List<SearchResults> batchResults = milvusClient.search(
        SearchParam.newBuilder()
            .withCollectionName(collection)
            .withVectors(embeddings)
            .withTopK(1)
            .build()
    );

    // 3. 逐条判断
    List<ChunkDedupResult> results = new ArrayList<>();
    for (int i = 0; i < chunks.size(); i++) {
        float score = batchResults.get(i).getResults(0).getScore();
        results.add(new ChunkDedupResult(
            chunks.get(i), embeddings.get(i), score,
            score > skipThreshold ? "skip" :
            score > downweightThreshold ? "downweight" : "keep"
        ));
    }
    return results;
}
```

**优化效果：**
- 100 个 chunk：从 100 次 RPC（~1s）→ 1 次批量 RPC（~50ms）
- 减少 95% 的网络往返开销

### 4.4 检索时去重

**改动位置：** `KnowledgeBaseServiceImpl.hybridSearch()` 方法

在 RRF 融合后、返回前，增加语义去重步骤：

```java
// 现有：按 docId+chunkIndex 去重（精确去重）
// 新增：语义去重（近似内容只保留得分最高的）
List<SearchResult> deduplicated = semanticDedup(fusedResults, 0.90);
```

```
semanticDedup 逻辑：
  对已排序的结果列表（得分从高到低）：
  1. 取第 1 条加入结果集
  2. 对后续每条，与结果集中所有已选条目算余弦相似度
  3. 若与任何已选条目相似度 > 0.90 → 跳过
  4. 否则加入结果集
  5. 直到结果集达到 topK 或遍历完
```

**注意：** 这里不需要调 Milvus，直接用已有的 embedding 向量在内存中计算余弦相似度。

### 4.5 [升级] 去重预览 / Dry-Run 模式（升级项 #8）

**新增接口：** `POST /api/v1/ai/knowledge-base/{kbId}/dedup-preview`

在正式导入前，用户可以先预览去重效果：

```java
@PostMapping("/{kbId}/dedup-preview")
public RESTResult<?> dedupPreview(
        @PathVariable Long kbId,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "contentType", defaultValue = "auto") String contentType,
        HttpServletRequest request) {

    // 1. 解析文件
    String content = DocumentParser.parse(file.getInputStream(), getFileType(filename));

    // 2. 分块
    List<String> chunks = chunkByStrategy(content, contentType);

    // 3. 对每个 chunk 做去重检查（但不入库）
    List<DedupPreviewItem> preview = new ArrayList<>();
    for (String chunk : chunks) {
        float[] embedding = vectorService.generateEmbedding(chunk);
        SearchResult nearest = vectorService.searchTop1(kbCollection, embedding);
        preview.add(new DedupPreviewItem(
            chunk.substring(0, Math.min(100, chunk.length())),  // 预览前100字
            nearest != null ? nearest.score() : 0,
            nearest != null ? nearest.content().substring(0, Math.min(100, nearest.content().length())) : null,
            classifyAction(nearest != null ? nearest.score() : 0)  // skip / downweight / keep
        ));
    }

    // 4. 汇总统计
    long skipCount = preview.stream().filter(p -> "skip".equals(p.action())).count();
    long downweightCount = preview.stream().filter(p -> "downweight".equals(p.action())).count();
    long keepCount = preview.stream().filter(p -> "keep".equals(p.action())).count();

    return RESTResult.success(Map.of(
        "totalChunks", chunks.size(),
        "skip", skipCount,
        "downweight", downweightCount,
        "keep", keepCount,
        "details", preview
    ));
}
```

**前端展示：**
- 导入前显示："预计导入 X 条，跳过 Y 条（重复），降权 Z 条（近似）"
- 可展开查看每条 chunk 的匹配详情
- 确认后再执行正式导入

---

## 五、文件上传 API

### 5.1 新增接口：文件上传

**端点：** `POST /api/v1/ai/knowledge-base/{kbId}/upload-file`

**改动文件：** `KnowledgeBaseController.java`

```java
@PostMapping("/{kbId}/upload-file")
public RESTResult<?> uploadFile(
        @PathVariable Long kbId,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "contentType", defaultValue = "auto") String contentType,
        HttpServletRequest request) {

    Long userId = AuthTokenFilter.getUserId(request);

    // 1. 校验文件
    String filename = file.getOriginalFilename();
    validateFileExtension(filename);           // 允许 doc/docx/pdf/md/txt
    validateFileSize(file.getSize());          // 上限 50MB

    // 2. [升级] 内容安全扫描（见第十四节）
    contentSecurityCheck(file);

    // 3. 解析文件内容（含 magic bytes 校验、加密检测、扫描件检测）
    String content = DocumentParser.parse(file.getInputStream(), getFileType(filename));

    // 4. [升级] 自动检测文档类型（如未指定）
    if ("auto".equals(contentType)) {
        contentType = DocumentTypeDetector.detect(content);
    }

    // 5. 确定分块策略
    String fileType = "script".equals(contentType) ? "script" : getFileType(filename);

    // 6. 调用已有 uploadDocument
    AiKbDocument doc = knowledgeBaseService.uploadDocument(kbId, title, content, fileType, userId);

    return RESTResult.success("上传成功", doc);
}
```

**文件大小限制（application.yml）：**

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 200MB   # [P1 修正] 支持批量上传 4×50MB，当前 application.yml 为 100MB 不够
```

### 5.2 新增接口：批量文件上传

**端点：** `POST /api/v1/ai/knowledge-base/{kbId}/upload-files`

```java
@PostMapping("/{kbId}/upload-files")
public RESTResult<?> uploadFiles(
        @PathVariable Long kbId,
        @RequestParam("files") List<MultipartFile> files,
        @RequestParam(value = "contentType", defaultValue = "auto") String contentType,
        HttpServletRequest request) {
    // 逐个解析并上传，返回汇总结果
    // [升级] 返回详细导入报告（见 5.4）
}
```

### 5.3 扩展现有目录导入接口

**改动文件：** `KnowledgeBaseImportServiceImpl.java`

#### 5.3.1 多层级目录遍历（新增）

当前 `Files.walk(dir)` 已支持递归遍历子目录，但缺少以下控制能力。用户购买的话术文档库通常按品类/场景分目录组织：

```
话术文档库/
├── 美妆护肤/
│   ├── 种草话术/
│   │   ├── 精华液种草.docx
│   │   └── 面膜推荐.pdf
│   └── 促销话术/
│       ├── 双十一促销.docx
│       └── 限时秒杀.doc
├── 食品/
│   ├── 零食推荐.docx
│   └── 养生茶话术.pdf
├── 服装/
│   └── 穿搭话术/
│       └── 夏季穿搭.docx
└── README.txt              ← 非话术文件，也会被导入
```

**增强后的目录遍历逻辑：**

```java
private static final Set<String> SUPPORTED_EXTENSIONS =
    Set.of(".md", ".txt", ".doc", ".docx", ".pdf");

// [P1 新增] 需排除的隐藏/系统目录
private static final Set<String> EXCLUDED_DIRS =
    Set.of(".git", ".svn", ".hg", "__MACOSX", ".DS_Store", "node_modules", "$RECYCLE.BIN", "System Volume Information");

/**
 * 多层级目录遍历，支持深度限制、隐藏目录过滤、符号链接处理、文件数上限
 */
private List<Path> walkDirectory(Path dir) throws IOException {
    int maxDepth = importMaxDepth;  // 配置化，默认 10 层
    int maxFiles = importMaxFiles;  // 配置化，默认 5000 个

    try (Stream<Path> walk = Files.walk(dir, maxDepth, FileVisitOption.FOLLOW_LINKS)) {
        List<Path> files = walk
            // 排除隐藏目录和系统目录
            .filter(p -> {
                for (int i = 0; i < p.getNameCount(); i++) {
                    String name = p.getName(i).toString();
                    if (name.startsWith(".") || EXCLUDED_DIRS.contains(name)) {
                        return false;
                    }
                }
                return true;
            })
            .filter(Files::isRegularFile)
            // 跳过隐藏文件
            .filter(p -> !p.getFileName().toString().startsWith("."))
            .filter(p -> {
                String n = p.getFileName().toString().toLowerCase();
                return SUPPORTED_EXTENSIONS.stream().anyMatch(n::endsWith);
            })
            .limit(maxFiles)  // 文件数上限保护
            .toList();

        if (files.size() >= maxFiles) {
            log.warn("目录 {} 文件数达到上限 {}，可能有遗漏，建议分批导入", dir, maxFiles);
        }

        return files;
    }
}
```

#### 5.3.2 目录结构元数据保留

将文件相对于导入根目录的路径保留到文档 metadata 中，用于：
- 前端按目录结构浏览导入的文档
- 自动推断品类标签（目录名 → 品类）
- 增量导入时的路径匹配

```java
/**
 * 从相对路径推断目录层级元数据
 * 例如：话术文档库/美妆护肤/种草话术/精华液种草.docx
 *   → relativePath = "美妆护肤/种草话术/精华液种草.docx"
 *   → dirCategory = "美妆护肤"
 *   → dirSubCategory = "种草话术"
 */
private Map<String, String> extractDirMetadata(Path file, Path rootDir) {
    Map<String, String> meta = new HashMap<>();
    Path relative = rootDir.relativize(file);
    meta.put("relativePath", relative.toString().replace('\\', '/'));
    meta.put("fileName", file.getFileName().toString());

    // 目录层级（从根目录开始的每一层目录名）
    int depth = relative.getNameCount();
    if (depth > 1) {
        meta.put("dirCategory", relative.getName(0).toString());   // 第 1 层目录 = 品类
    }
    if (depth > 2) {
        meta.put("dirSubCategory", relative.getName(1).toString()); // 第 2 层目录 = 子分类
    }
    meta.put("dirDepth", String.valueOf(depth - 1));  // 目录深度（不含文件名）

    return meta;
}
```

#### 5.3.3 目录级错误隔离

批量导入时，单个目录/文件的错误不影响其他目录的处理：

```java
/**
 * 按目录分组处理，每个目录独立计数和错误隔离
 */
private ImportResult importByDirectory(Path rootDir, Long kbId, Long userId, ImportProgress progress) {
    List<Path> allFiles = walkDirectory(rootDir);

    // 按直接父目录分组
    Map<Path, List<Path>> byDir = allFiles.stream()
        .collect(Collectors.groupingBy(f -> rootDir.relativize(f.getParent())));

    ImportResult totalResult = new ImportResult();

    for (Map.Entry<Path, List<Path>> entry : byDir.entrySet()) {
        Path dirPath = entry.getKey();
        List<Path> dirFiles = entry.getValue();

        log.info("处理目录 [{}]，含 {} 个文件", dirPath, dirFiles.size());
        progress.setCurrentDir(dirPath.toString());

        try {
            for (Path file : dirFiles) {
                try {
                    processOneFile(file, kbId, userId, rootDir, progress);
                    totalResult.addSuccess();
                } catch (Exception e) {
                    log.error("文件处理失败 [{}]: {}", file.getFileName(), e.getMessage());
                    totalResult.addFailed(file.getFileName().toString(), e.getMessage());
                    // 单文件失败不中断整个目录
                }
            }
        } catch (Exception e) {
            log.error("目录处理异常 [{}]: {}", dirPath, e.getMessage());
            totalResult.addDirError(dirPath.toString(), e.getMessage());
            // 单目录异常不中断整个导入
        }
    }

    return totalResult;
}
```

#### 5.3.4 导入进度增强

进度信息增加目录级粒度：

```java
public class ImportProgress {
    // ... 已有字段
    private String currentDir;       // 当前正在处理的目录
    private int totalDirs;           // 总目录数
    private int processedDirs;       // 已处理目录数
    private Map<String, DirStat> dirStats;  // 每个目录的统计

    @Data
    public static class DirStat {
        private int total;
        private int success;
        private int failed;
        private int skipped;
    }
}
```

#### 5.3.5 文件过滤扩展

**当前代码（第 117-121 行）：**
```java
files = walk.filter(Files::isRegularFile)
    .filter(p -> {
        String n = p.getFileName().toString().toLowerCase();
        return n.endsWith(".md") || n.endsWith(".txt");
    })
    .toList();
```

**改为使用 5.3.1 的 `walkDirectory()` 方法。**

**当前代码（第 193-194 行）：**
```java
content = Files.readString(file, StandardCharsets.UTF_8);
```

**改为：**
```java
content = DocumentParser.parse(file);
```

**当前代码（第 231 行）：**
```java
String fileType = name.endsWith(".md") ? "md" : "text";
```

**改为：**
```java
String fileType = DocumentParser.detectFileType(name);
// md → "md", doc/docx → "docx", pdf → "pdf", 其他 → "text"
```

#### 5.3.6 目录导入配置项

```yaml
app.ai.kb:
  import-parallelism: ${AI_KB_IMPORT_PARALLELISM:6}
  import:
    max-depth: 10              # 目录遍历最大深度（防止无限递归）
    max-files: 5000            # 单次导入最大文件数
    follow-symlinks: true      # 是否跟随符号链接
    exclude-hidden: true       # 是否排除隐藏文件/目录
    preserve-dir-structure: true  # 是否将目录结构写入文档 metadata
    dir-as-category: true      # 是否将第 1 层目录名作为品类标签
```

**KbImportVO 新增字段：**

```java
private String contentType;  // "general" | "script" | "auto"（自动检测，使用智能分块）
```

### 5.4 [升级] 增量导入支持（升级项 #16）

**改动文件：** `KnowledgeBaseImportServiceImpl.java`

新增增量导入能力：记录上次导入时间戳，只处理新增/修改的文件：

```java
@Override
public ImportResult importIncremental(String sourcePath, Long kbId, Long userId, ImportProgress progress) {
    // 1. 获取该目录上次导入时间
    Instant lastImportTime = getLastImportTime(kbId, sourcePath);

    // 2. 遍历目录，只取 lastModified > lastImportTime 的文件
    List<Path> newFiles = walk.filter(Files::isRegularFile)
        .filter(p -> SUPPORTED_EXTENSIONS.stream().anyMatch(
            p.getFileName().toString().toLowerCase()::endsWith))
        .filter(p -> {
            try {
                return Files.getLastModifiedTime(p).toInstant().isAfter(lastImportTime);
            } catch (IOException e) { return true; }
        })
        .toList();

    // 3. 正常导入新文件
    // ... 复用已有 processOneFile 逻辑

    // 4. 记录本次导入时间
    saveImportTimestamp(kbId, sourcePath, Instant.now());
}
```

**新增接口：** `POST /api/v1/ai/knowledge-base/{kbId}/import-incremental`

**前端：** 在导入对话框新增"仅导入新增文件"复选框。

---

## 六、话术生成 RAG 集成

### 6.1 核心思路

在 AI 生成话术的 Prompt 中，注入从知识库检索到的参考话术案例。

### 6.2 改动位置

#### 6.2.1 LiveAiServiceImpl（直播话术）

**方法：** `buildPrompt()`（第 843-923 行）

在 Prompt 末尾追加参考案例区块：

```java
// 现有 prompt 构建完成后，追加 RAG 参考
// [P1 修正] 使用结构化 XML 标签包裹，LLM 对 XML 标签的指令遵从度显著高于纯文本分隔符
String ragContext = buildRagContext(product, scriptType, style);
if (ragContext != null && !ragContext.isBlank()) {
    prompt.append("\n\n");
    prompt.append(ragContext);
    prompt.append("\n请参考以上案例的表达方式，为当前产品生成原创话术。\n");
}
```

**buildRagContext 实现（含升级项）：**

```java
private String buildRagContext(DyProduct product, String scriptType, String style) {
    if (!ragEnabled) return null;

    // 1. 查找话术知识库（按名称 "huashu" 或配置的 kbId）
    Long kbId = resolveScriptKbId();
    if (kbId == null) return null;

    // [P1 修正] Token 预算管理：动态计算可用 RAG 上下文长度
    // 避免固定 2000 字符导致在不同 LLM（4K/8K/32K/128K 上下文窗口）下截断或浪费
    int availableTokenBudget = calculateRagTokenBudget(prompt.length());
    int effectiveMaxContext = Math.min(ragMaxContextLength, availableTokenBudget);
    if (effectiveMaxContext < 200) {
        log.debug("RAG Token 预算不足（剩余 {} 字符），跳过注入", effectiveMaxContext);
        return null;
    }

    // 2. [P0 修正] 两阶段查询扩展（品类维度 + 语义改写，与 queryRewriteService 合并）
    //    阶段 A：品类维度扩展（本方法构建，基于产品/话术类型/风格）
    List<String> categoryQueries = buildCategoryQueries(product, scriptType, style);
    //    阶段 B：语义改写（复用现有 queryRewriteService，生成 2-3 个语义变体）
    //    注意：queryRewriteService 当前绑定"用户已绑定抖音账号"前置条件，
    //    RAG 场景需放宽此限制或传入系统 userId
    List<String> allQueries = new ArrayList<>(categoryQueries);
    if (queryRewriteEnabled) {
        for (String q : categoryQueries) {
            List<String> rewritten = queryRewriteService.rewrite(q);
            if (rewritten != null) allQueries.addAll(rewritten);
        }
    }
    // 合并去重查询（避免完全相同的查询重复检索）
    allQueries = allQueries.stream().distinct().toList();

    // 3. [P0 修正] 检索时跳过 Redis 缓存（确保新导入文档立即可检索）
    // [Cursor 修正] 多查询并行检索：品类扩展 3 条 × rewrite 2-3 条 ≈ 6-9 个查询
    // 串行执行延迟 ~1-2s（每次 ~100-200ms），改为 CompletableFuture 并行，降至 ~200ms
    Set<String> seenChunkIds = ConcurrentHashMap.newKeySet();
    List<SearchResult> allResults = Collections.synchronizedList(new ArrayList<>());

    List<CompletableFuture<Void>> futures = allQueries.stream()
        .map(query -> CompletableFuture.runAsync(() -> {
            List<SearchResult> results = knowledgeBaseService.hybridSearch(
                kbId, query, ragTopK, userId, /*metadataFilter=*/null, /*skipCache=*/true);
            if (results != null) {
                for (SearchResult r : results) {
                    if (seenChunkIds.add(r.chunkId())) {
                        allResults.add(r);
                    }
                }
            }
        }))
        .toList();

    // 等待所有检索完成（超时 5 秒，避免单路阻塞拖垮整体）
    try {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .get(5, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
        log.warn("RAG 并行检索部分超时，已收集 {} 条结果", allResults.size());
    }

    // 4. [升级] 最低相关度过滤（升级项 #11，P0）
    allResults = allResults.stream()
        .filter(r -> r.score() >= ragMinScore)
        .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
        .limit(ragTopK)
        .toList();

    if (allResults.isEmpty()) return null;

    // 5. [P1 修正] 使用结构化 XML 标签拼接参考文本（LLM 指令遵从度更高）
    StringBuilder sb = new StringBuilder();
    sb.append("<reference_scripts>\n");
    sb.append("<note>仅参考风格和技巧，禁止照搬内容</note>\n");
    int totalLen = 0;
    for (int i = 0; i < allResults.size(); i++) {
        SearchResult r = allResults.get(i);
        String content = r.content();
        if (totalLen + content.length() > effectiveMaxContext) {
            break;
        }
        // 提取 chunk 标签中的二级标签（type:xxx）作为 category
        String category = extractTypeLabel(r.labels());
        sb.append(String.format("<script id=\"%d\" category=\"%s\" score=\"%.2f\">",
            i + 1, category, r.score()));
        sb.append(content);
        sb.append("</script>\n");
        totalLen += content.length();
    }
    sb.append("</reference_scripts>");
    return sb.toString();
}
```

#### 6.2.2 [升级] 两阶段查询扩展策略（升级项 #10，P0）

**与现有 `queryRewriteService` 的关系：**

当前 `KnowledgeBaseServiceImpl.hybridSearch()` 已集成 `queryRewriteService`，可生成 2-3 个语义改写子查询。方案的多查询扩展**不重复实现语义改写**，而是做品类维度扩展，两者合并使用：

| 阶段 | 负责方 | 功能 | 示例 |
|------|--------|------|------|
| A：品类维度扩展 | `buildCategoryQueries()` | 基于产品属性构建多角度查询 | "美妆 促销话术"、"水乳套装 补水" |
| B：语义改写 | 现有 `queryRewriteService` | 对每个查询生成语义变体 | "美妆促销" → "化妆品限时特惠"、"护肤品打折推荐" |

**阶段 A：品类维度扩展（新增）**

```java
/**
 * 构建品类维度查询列表（不做语义改写，仅做角度扩展）
 * 语义改写由 queryRewriteService 在阶段 B 完成
 */
private List<String> buildCategoryQueries(DyProduct product, String scriptType, String style) {
    List<String> queries = new ArrayList<>();

    // 查询 1：品类 + 话术类型（主查询）
    StringBuilder q1 = new StringBuilder();
    if (product.getProductCategory() != null) {
        q1.append(product.getProductCategory()).append(" ");
    }
    q1.append(mapScriptType(scriptType));
    if (style != null) q1.append(" ").append(style);
    queries.add(q1.toString().trim());

    // 查询 2：产品名 + 卖点关键词
    if (product.getProductName() != null) {
        StringBuilder q2 = new StringBuilder(product.getProductName());
        if (product.getSellingPoints() != null) {
            // 取第一个卖点关键词
            String sp = product.getSellingPoints().split("[,，]")[0].trim();
            q2.append(" ").append(sp);
        }
        queries.add(q2.toString().trim());
    }

    // 查询 3：风格 + 场景（如有）
    if (style != null && !style.isBlank()) {
        queries.add(style + " " + mapScriptType(scriptType) + " 案例");
    }

    return queries;
}

private String mapScriptType(String scriptType) {
    return switch (scriptType) {
        case "seed" -> "种草话术";
        case "promotion" -> "促销话术";
        case "formal" -> "产品介绍";
        case "emotional" -> "情绪价值话术";
        case "transition" -> "过渡话术";
        default -> "话术";
    };
}
```

#### 6.2.3 [升级] 最低相关度过滤（升级项 #11，P0）

防止不相关内容污染 Prompt：

```java
// 过滤掉相关度低于阈值的结果
allResults = allResults.stream()
    .filter(r -> r.score() >= ragMinScore)  // 默认 0.5
    .toList();

if (allResults.isEmpty()) {
    log.debug("RAG 检索无高于阈值({})的相关结果，跳过注入", ragMinScore);
    return null;
}
```

**配置：**

```yaml
app.ai.kb.rag:
  min-score: 0.5    # 最低相关度阈值，低于此值不注入
```

#### 6.2.4 [升级] 元数据过滤检索（升级项 #12）

利用 chunk 标签做精准检索，避免品类不匹配的话术被召回：

```java
// 构建 Milvus 检索时附加 metadata 过滤条件
String filter = buildMetadataFilter(product, scriptType);

// 例如：labels LIKE "%美妆护肤%" AND labels LIKE "%促销%"
List<SearchResult> results = knowledgeBaseService.hybridSearch(
    kbId, query, ragTopK, userId, filter  // 新增 filter 参数
);
```

**hybridSearch 扩展：**

```java
// KnowledgeBaseService.hybridSearch() 新增重载（含缓存控制 + 元数据过滤）
// [P0 修正] 新增 skipCache 参数，RAG 场景传 true 确保新导入文档可被检索
List<SearchResult> hybridSearch(Long kbId, String query, int topK, Long userId, String metadataFilter, boolean skipCache);

// 向下兼容：原有签名内部调用 skipCache=false
default List<SearchResult> hybridSearch(Long kbId, String query, int topK, Long userId) {
    return hybridSearch(kbId, query, topK, userId, null, false);
}
```

**[P0 修正] 导入后缓存清除：**

在 `KnowledgeBaseServiceImpl.uploadDocument()` 成功后，主动清除该知识库的检索缓存：

```java
// uploadDocument() 末尾新增
cacheManager.evict("kb_search:" + kbId + ":*");  // 清除该 KB 的所有检索缓存
log.info("已清除知识库 {} 的检索缓存", kbId);
```

同样在 `KnowledgeBaseImportServiceImpl` 批量导入完成后清除缓存。

#### 6.2.5 ProductScriptServiceImpl（产品话术）

**[Cursor 修正] RAG 注入统一在 Live 侧：** 产品话术的 Prompt 实际由 `LiveAiServiceImpl` 构建（`buildProductScriptPrompt` / `generateProductScript`），而非 `ProductScriptServiceImpl` 直接拼 Prompt。因此 RAG 注入**不在 Product 侧做**，而是统一在 Live 侧的 `buildProductScriptPrompt()` 中注入（与 `buildPrompt()` 的 RAG 逻辑一致），避免两处 RAG 逻辑分叉。

```java
// LiveAiServiceImpl.buildProductScriptPrompt() 末尾追加（与 buildPrompt 相同模式）
String ragContext = buildRagContext(product, "formal", style);
if (ragContext != null && !ragContext.isBlank()) {
    prompt.append("\n\n");
    prompt.append(ragContext);
    prompt.append("\n请参考以上案例的表达方式，为当前产品生成原创话术。\n");
}
```

`ProductScriptServiceImpl` 无需改动，它调用 `liveAiService.generateProductScript()` 时，RAG 已在 Live 侧注入。

#### 6.2.6 [升级] 扩展 RAG 覆盖范围（升级项 #13）

当前 RAG 仅设计了直播话术和产品话术的注入点。需要扩展到所有话术生成场景：

| 场景 | 服务方法 | RAG 注入 |
|------|----------|----------|
| 直播话术（种草/促销/产品介绍） | `LiveAiServiceImpl.buildPrompt()` | ✅ 已规划 |
| 产品话术（多风格） | `ProductScriptServiceImpl.generate()` | ✅ 已规划 |
| **情绪价值话术** | `LiveAiServiceImpl.buildEmotionalPrompt()` | **新增** |
| **过渡衔接话术** | `LiveAiServiceImpl.buildTransitionPrompt()` | **新增** |
| **短视频脚本** | `ProductScriptServiceImpl.generateVideoScript()` | **新增** |

每个场景的 RAG 查询需定制：
- 情绪价值话术 → 查询 label 含 "情绪价值" 的 chunk
- 过渡衔接话术 → 查询 label 含 "过渡话术" 的 chunk
- 短视频脚本 → 查询 label 含 "种草" 的 chunk

#### 6.2.7 [升级] RAG 来源归因 + Boost 联动（升级项 #14）

记录 RAG 注入的参考来源，并支持用户反馈驱动 boost 调整：

```java
// 1. 生成话术时记录使用了哪些参考
List<RagReference> references = allResults.stream()
    .map(r -> new RagReference(r.docId(), r.chunkId(), r.score()))
    .toList();

// 2. 在返回结果中附带参考来源
generatedScript.setRagReferences(references);

// 3. 用户反馈接口（已有 feedback 机制可扩展）
// POST /api/v1/ai/knowledge-base/rag-feedback
// { scriptId, chunkId, useful: true/false }

// 4. 有用的参考 → 对应 chunk 的 boost_factor += 0.1（上限 2.0）
// 无用的参考 → boost_factor -= 0.05（下限 0.3）
```

### 6.3 配置项

```yaml
# application.yml
app.ai.kb:
  rag:
    enabled: true
    script-kb-name: "huashu"     # 话术知识库名称
    top-k: 3                      # 检索参考条数
    max-context-length: 2000      # 参考文本最大字符数（硬上限）
    token-budget-ratio: 0.15      # [P1 修正] RAG 占 LLM 上下文窗口的最大比例（默认 15%）
    llm-context-window: 32000     # [P1 修正] LLM 上下文窗口大小（字符，按 1 token ≈ 2 中文字符估算）
    min-score: 0.5                # [升级] 最低相关度阈值
    skip-cache: true              # [P0 修正] RAG 检索时跳过 Redis 缓存
    query-rewrite: true           # [P0 修正] 复用 queryRewriteService 做语义改写
    category-expand: true         # [P0 修正] 品类维度扩展（buildCategoryQueries）
    metadata-filter: true         # [升级] 是否启用元数据过滤
    feedback-boost: true          # [升级] 用户反馈是否影响 boost
```

### 6.4 RAG 开关

- 配置 `rag.enabled=false` 可全局关闭
- 前端生成对话框可增加"参考话术库"开关，用户按需启用
- 未创建话术知识库时自动跳过，不影响现有功能

---

## 七、知识库初始化

### 7.1 新增话术专用知识库

**改动文件：** `KnowledgeBaseInitializer.java`

在现有 douyin(ID:2) 和 zhishi(ID:3) 基础上，新增：

```java
// [Cursor 修正] 复用现有 createIfAbsent 模式（按名称查找或创建），不硬编码 ID
// 现有代码已有 createIfAbsent("douyin", ...) 和 createIfAbsent("zhishi", ...)
// 直接追加即可，ID 由数据库自增分配
createIfAbsent("huashu", "话术文案库：直播话术、产品文案、种草话术、促销话术、情绪价值话术等");
```

**RAG 检索时按名称查找 kbId：**

```java
private Long resolveScriptKbId() {
    // 优先从配置读取（app.ai.kb.rag.script-kb-name: huashu）
    // 按名称查找，而非写死 ID，兼容不同环境的数据库序列
    return knowledgeBaseRepository
        .findByUserIdAndKbNameAndDeleted(DEFAULT_USER_ID, ragScriptKbName, 0)
        .map(AiKnowledgeBase::getId)
        .orElse(null);
}
```

### 7.2 分类关键词扩展

**改动文件：** `KnowledgeBaseImportServiceImpl.classifyByContent()`

新增话术分类：

```java
private static final String[] HUASHU_KEYWORDS = {
    "话术", "文案", "种草", "促销", "直播间", "姐妹们", "宝子们",
    "家人们", "下单", "秒杀", "限时", "抢购", "套盒", "套装",
    "性价比", "功效", "成分", "肤质", "补水", "保湿", "抗老"
};
```

`autoClassify` 支持三路分类：`huashu` / `douyin` / `zhishi`

---

## 八、前端改动

### 8.1 知识库文件上传组件

**新增/改动位置：** 知识库管理页面

```tsx
<Dropzone
  accept={{
    'application/pdf': ['.pdf'],
    'application/msword': ['.doc'],
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
    'text/plain': ['.txt'],
    'text/markdown': ['.md']
  }}
  maxSize={50 * 1024 * 1024}  // 50MB
  onDrop={handleUpload}
>
  拖拽 DOC/PDF/TXT 文件到这里，或点击选择文件
</Dropzone>

{/* 内容类型选择 */}
<FormControl>
  <InputLabel>文档类型</InputLabel>
  <Select value={contentType} onChange={e => setContentType(e.target.value)}>
    <MenuItem value="auto">自动检测（推荐）</MenuItem>
    <MenuItem value="general">通用文档（按段落分块）</MenuItem>
    <MenuItem value="script">话术文档（按条目分块，自动去重）</MenuItem>
  </Select>
</FormControl>

{/* [升级] 去重预览按钮 */}
<Button variant="outlined" onClick={handleDedupPreview} disabled={!file}>
  去重预览
</Button>
```

### 8.2 话术生成界面增加 RAG 开关

**改动位置：** `ProductScriptManageDialog`、`LiveScriptBuilderPage` 的生成对话框

```tsx
<FormControlLabel
  control={<Switch checked={useKbRef} onChange={e => setUseKbRef(e.target.checked)} />}
  label="参考话术知识库"
/>
{useKbRef && (
  <Typography variant="caption" color="text.secondary">
    生成时将从话术库检索相似案例作为参考，提升话术质量
  </Typography>
)}

{/* [升级] 生成结果中展示参考来源 */}
{script.ragReferences && script.ragReferences.length > 0 && (
  <Accordion>
    <AccordionSummary>参考来源（{script.ragReferences.length} 条）</AccordionSummary>
    <AccordionDetails>
      {script.ragReferences.map((ref, i) => (
        <Box key={i}>
          <Typography variant="body2">{ref.preview}</Typography>
          <Typography variant="caption">相关度: {(ref.score * 100).toFixed(0)}%</Typography>
          <IconButton size="small" onClick={() => handleRagFeedback(ref.chunkId, true)}>
            <ThumbUpIcon />
          </IconButton>
          <IconButton size="small" onClick={() => handleRagFeedback(ref.chunkId, false)}>
            <ThumbDownIcon />
          </IconButton>
        </Box>
      ))}
    </AccordionDetails>
  </Accordion>
)}
```

### 8.3 批量目录导入界面

**现有功能：** 已有 `import-from-path` 和 `import-from-path-async` 接口 + 前端进度轮询

**改动：** 在导入对话框中增加：
- 文档类型选择（自动检测/通用/话术）
- 去重开关
- [升级] 增量导入复选框
- 支持的格式提示更新为 "支持 .md .txt .doc .docx .pdf"

### 8.4 [升级] 去重预览对话框

**新增组件：** `DedupPreviewDialog.tsx`

```tsx
<Dialog open={dedupOpen} maxWidth="md" fullWidth>
  <DialogTitle>去重预览</DialogTitle>
  <DialogContent>
    <Box display="flex" gap={2} mb={2}>
      <Chip label={`新内容 ${preview.keep} 条`} color="success" />
      <Chip label={`重复跳过 ${preview.skip} 条`} color="error" />
      <Chip label={`近似降权 ${preview.downweight} 条`} color="warning" />
    </Box>
    <Table size="small">
      <TableHead>
        <TableRow>
          <TableCell>内容预览</TableCell>
          <TableCell>相似度</TableCell>
          <TableCell>最近似已有内容</TableCell>
          <TableCell>处理</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {preview.details.map((item, i) => (
          <TableRow key={i}>
            <TableCell>{item.preview}</TableCell>
            <TableCell>{(item.score * 100).toFixed(0)}%</TableCell>
            <TableCell>{item.nearestPreview}</TableCell>
            <TableCell>
              <Chip label={item.action} size="small"
                color={item.action === 'keep' ? 'success' : item.action === 'skip' ? 'error' : 'warning'} />
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  </DialogContent>
  <DialogActions>
    <Button onClick={handleClose}>取消</Button>
    <Button variant="contained" onClick={handleConfirmImport}>确认导入</Button>
  </DialogActions>
</Dialog>
```

### 8.5 [升级] 知识库分类浏览 + 标签管理（升级项 #17）

**改动位置：** 知识库管理页面

新增分类浏览视图：

```tsx
{/* 标签过滤器 */}
<Box display="flex" gap={1} flexWrap="wrap" mb={2}>
  {allLabels.map(label => (
    <Chip
      key={label}
      label={`${label} (${labelCounts[label]})`}
      variant={selectedLabels.includes(label) ? "filled" : "outlined"}
      onClick={() => toggleLabel(label)}
    />
  ))}
</Box>

{/* 文档列表（支持按标签过滤） */}
<DataGrid
  rows={filteredDocs}
  columns={[
    { field: 'title', headerName: '标题', flex: 1 },
    { field: 'fileType', headerName: '格式', width: 80 },
    { field: 'chunkCount', headerName: '分块数', width: 100 },
    { field: 'labels', headerName: '标签', flex: 1,
      renderCell: (params) => params.value.map(l => <Chip key={l} label={l} size="small" />) },
    { field: 'createTime', headerName: '导入时间', width: 160 },
  ]}
/>
```

### 8.6 [升级] 文档 Chunk 预览（升级项 #18）

**新增接口：** `POST /api/v1/ai/knowledge-base/{kbId}/documents/{docId}/chunks`（[P1 修正] 统一 POST 规范）

**前端：** 点击文档可展开查看所有 chunk 及其标签、boost_factor：

```tsx
<Accordion>
  <AccordionSummary>{doc.title} ({doc.chunkCount} 个分块)</AccordionSummary>
  <AccordionDetails>
    {chunks.map((chunk, i) => (
      <Card key={i} variant="outlined" sx={{ mb: 1, p: 1 }}>
        <Typography variant="body2">{chunk.content}</Typography>
        <Box display="flex" gap={0.5} mt={0.5}>
          {chunk.labels.map(l => <Chip key={l} label={l} size="small" />)}
          <Chip label={`boost: ${chunk.boostFactor}`} size="small" variant="outlined" />
        </Box>
      </Card>
    ))}
  </AccordionDetails>
</Accordion>
```

---

## 九、数据库变更

### 9.1 ai_kb_document 表

复用现有字段 + 新增字段：
- `file_type`：用于记录 "docx"/"pdf"/"doc"/"md"/"text"
- `source_type`：新增值 "purchased"（购买的文档）
- `boost_factor`：去重降权时使用

**[升级] 新增字段：**

```sql
-- 文档级去重指纹
ALTER TABLE ai_kb_document ADD COLUMN content_fingerprint VARCHAR(64);
ALTER TABLE ai_kb_document ADD COLUMN simhash BIGINT;

-- 文档元数据（JSON）
ALTER TABLE ai_kb_document ADD COLUMN metadata JSONB;

-- 索引
CREATE INDEX idx_kb_doc_fingerprint ON ai_kb_document(kb_id, content_fingerprint);
CREATE INDEX idx_kb_doc_simhash ON ai_kb_document(kb_id, simhash);
```

### 9.2 [升级] 导入报告持久化表（升级项 #15）

```sql
CREATE TABLE IF NOT EXISTS kb_import_report (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    source_path VARCHAR(500),              -- 导入源路径
    import_type VARCHAR(16),               -- directory / file_upload / incremental
    total_files INT DEFAULT 0,
    success_count INT DEFAULT 0,
    failed_count INT DEFAULT 0,
    skipped_count INT DEFAULT 0,
    dedup_skipped INT DEFAULT 0,           -- 去重跳过数
    dedup_downweighted INT DEFAULT 0,      -- 去重降权数
    new_chunks INT DEFAULT 0,              -- 新增 chunk 总数
    content_type VARCHAR(16),              -- general / script / auto
    errors JSONB,                          -- 错误详情 JSON
    duration_ms BIGINT,                    -- 导入耗时
    user_id BIGINT NOT NULL,
    deleted INTEGER NOT NULL DEFAULT 0,    -- [P0 修正] 逻辑删除（项目规范：所有表必须包含）
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_import_report_kb ON kb_import_report(kb_id, create_time DESC);
```

### 9.3 去重日志表（可选）

```sql
CREATE TABLE IF NOT EXISTS kb_import_dedup_log (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    doc_id BIGINT,
    chunk_text_preview VARCHAR(200),   -- 被跳过的 chunk 前 200 字
    similar_chunk_id BIGINT,            -- 与哪个已有 chunk 重复
    similarity_score DECIMAL(5,4),      -- 相似度分数
    action VARCHAR(16),                 -- skip / downweight
    deleted INTEGER NOT NULL DEFAULT 0, -- [P0 修正] 逻辑删除
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 9.4 [升级] 增量导入时间戳表

```sql
CREATE TABLE IF NOT EXISTS kb_import_checkpoint (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    source_path VARCHAR(500) NOT NULL,
    last_import_time TIMESTAMP NOT NULL,
    file_count INT DEFAULT 0,
    deleted INTEGER NOT NULL DEFAULT 0, -- [P0 修正] 逻辑删除
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(kb_id, source_path)
);
```

---

## 十、[升级] 内容安全（升级项 #19, #20）

### 10.1 Magic Bytes 文件类型校验（升级项 #19）

已集成在 `DocumentParser.validateFileType()` 中（见第二节 2.1）。

| 格式 | Magic Bytes | 说明 |
|------|-------------|------|
| PDF | `%PDF` (25 50 44 46) | PDF 文件头 |
| DOC | `D0 CF 11 E0` | OLE2 复合文档 |
| DOCX | `50 4B 03 04` | ZIP 压缩包（OOXML） |
| TXT/MD | 无 | 纯文本不校验 |

**防护目标：** 防止 `.exe`、`.bat`、`.sh` 等可执行文件被重命名为 `.pdf`/.doc` 后上传。

### 10.2 内容安全扫描（升级项 #20）

**新建** `ContentSecurityScanner.java`

在文件解析后、入库前，对内容做安全扫描：

```java
public class ContentSecurityScanner {

    private static final Logger log = LoggerFactory.getLogger(ContentSecurityScanner.class);

    /** 敏感内容模式 */
    private static final String[] SENSITIVE_PATTERNS = {
        // 个人隐私信息
        "\\b1[3-9]\\d{9}\\b",                          // 手机号
        "\\b\\d{6}(18|19|20)\\d{2}(0[1-9]|1[0-2]).*\\b", // 身份证号（前缀）
        "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", // 邮箱
    };

    /** 违禁广告用语（与现有 ComplianceService 的绝对化用语检测互补） */
    private static final String[] PROHIBITED_CLAIMS = {
        "国家级", "世界级", "最高级", "最佳", "第一", "唯一",
        "治愈", "根治", "药到病除", "包治百病"
    };

    /**
     * 扫描内容安全问题，返回警告列表
     * 不阻断导入，仅记录警告供人工审核
     */
    public static List<SecurityWarning> scan(String content, String filename) {
        List<SecurityWarning> warnings = new ArrayList<>();

        // 1. 个人隐私信息检测
        for (String pattern : SENSITIVE_PATTERNS) {
            Matcher m = Pattern.compile(pattern).matcher(content);
            int count = 0;
            while (m.find()) count++;
            if (count > 0) {
                warnings.add(new SecurityWarning("privacy",
                    String.format("检测到 %d 处疑似个人隐私信息（手机号/身份证/邮箱）", count),
                    "建议脱敏后再导入"));
            }
        }

        // 2. 违禁广告用语检测
        List<String> foundProhibited = new ArrayList<>();
        for (String word : PROHIBITED_CLAIMS) {
            if (content.contains(word)) {
                foundProhibited.add(word);
            }
        }
        if (!foundProhibited.isEmpty()) {
            warnings.add(new SecurityWarning("compliance",
                "检测到违禁广告用语: " + String.join("、", foundProhibited),
                "入库后检索使用时需注意合规"));
        }

        // 3. 异常内容检测（乱码/二进制残留）
        int nonPrintable = 0;
        for (char c : content.toCharArray()) {
            if (c < 0x20 && c != '\n' && c != '\r' && c != '\t') nonPrintable++;
        }
        if (nonPrintable > content.length() * 0.05) {
            warnings.add(new SecurityWarning("encoding",
                "内容中含大量不可打印字符（占比 " + (nonPrintable * 100 / content.length()) + "%）",
                "文件可能解析异常或含二进制内容"));
        }

        if (!warnings.isEmpty()) {
            log.warn("内容安全扫描发现 {} 个问题: {}", warnings.size(), filename);
        }
        return warnings;
    }

    public record SecurityWarning(String type, String message, String suggestion) {}
}
```

**处理策略：**
- 安全扫描**不阻断**导入流程（话术文档中可能合理包含手机号等）
- 扫描结果记入导入报告 + 文档 metadata
- 前端在导入结果中显示安全警告，供用户人工审核
- 可通过配置关闭：`app.ai.kb.security.scan-enabled=false`

**配置：**

```yaml
app.ai.kb:
  security:
    scan-enabled: true
    block-on-privacy: false    # 发现隐私信息是否阻断导入
    block-on-compliance: false # 发现违禁用语是否阻断导入
```

---

## 十点五、[Cursor 修正] 文档解析专用错误码

当前方案对加密/扫描件/伪装文件统一抛 `VALIDATION_FAIL`（通用 1xxx 段），前端无法区分具体原因。新增 AI 模块 4000 段专用错误码：

| 错误码 | 常量名 | 描述 | 触发场景 |
|--------|--------|------|---------|
| 4060 | AI_DOC_ENCRYPTED | 文档已加密 | PDF/DOC/DOCX 加密检测 |
| 4061 | AI_DOC_SCAN_ONLY | 疑似扫描件 | PDF 文字密度过低 |
| 4062 | AI_DOC_FAKE_TYPE | 文件类型伪装 | Magic bytes 与扩展名不符 |
| 4063 | AI_DOC_PARSE_FAIL | 文档解析失败 | POI/PDFBox 解析异常 |
| 4064 | AI_DOC_TOO_LARGE | 文档超过大小限制 | 超过 max-file-size |
| 4065 | AI_IMPORT_DIR_DEPTH | 目录层级超限 | 超过 max-depth |
| 4066 | AI_IMPORT_FILE_LIMIT | 文件数量超限 | 超过 max-files |

**需同步更新：**
- `common/constant/ErrorCode.java` — 新增常量
- `docs/04-错误码注册表.md` — 注册 4060-4066 段
- `frontend-react/src/utils/error-codes.ts` — 前端映射

**DocumentParser 中替换 VALIDATION_FAIL：**

```java
// 替换前（通用错误码，前端无法区分）
throw new BusinessException(ErrorCode.VALIDATION_FAIL, "文件已加密...");

// 替换后（专用错误码，前端可根据 code 显示不同提示）
throw new BusinessException(ErrorCode.AI_DOC_ENCRYPTED, "文件已加密，请先解除密码保护后再导入: " + filename);
```

---

## 十一、改动文件清单

| # | 文件 | 改动类型 | 说明 |
|---|------|---------|------|
| 1 | `pom.xml` | 修改 | 新增 PDFBox、poi-scratchpad、tika-core(可选) 依赖 |
| 2 | `DocumentParser.java` | **新建** | DOC/DOCX/PDF 统一解析 + magic bytes 校验 + 加密检测 + 扫描件检测 + 元数据提取（[P1] PDFBox 3.x API 修正） |
| 3 | `ScriptAwareChunker.java` | **新建** | 话术文档智能分块（按条目） |
| 4 | `DocumentTypeDetector.java` | **新建** | [升级] 文档类型自动检测（script/mixed/general）（[P1] 关键词配置化） |
| 5 | `MixedDocumentProcessor.java` | **新建** | [升级] 混合文档分段处理 |
| 6 | `ChunkLabeler.java` | **新建** | [升级] chunk 业务标签自动打标（[P1] 三级标签体系，与 classifyByContent 对齐） |
| 7 | `ContentSecurityScanner.java` | **新建** | [升级] 内容安全扫描（隐私/违禁/编码） |
| 8 | `KnowledgeBaseImportServiceImpl.java` | 修改 | 多层级目录遍历增强（walkDirectory）、目录结构元数据、目录级错误隔离、解析调用替换、分类扩展、增量导入 |
| 9 | `KnowledgeBaseServiceImpl.java` | 修改 | uploadDocument 增加文档级预检 + chunk 级去重 + 分块策略选择 + 批量去重优化；hybridSearch 增加语义去重 + 元数据过滤 |
| 10 | `KnowledgeBaseController.java` | 修改 | 新增 upload-file、upload-files、dedup-preview、import-incremental、chunks 查看 端点 |
| 11 | `KbImportVO.java` | 修改 | 新增 contentType 字段 |
| 12 | `KbDocumentUploadVO.java` | 修改 | 新增 contentType 字段 |
| 13 | `KnowledgeBaseInitializer.java` | 修改 | 新增 huashu 知识库初始化 |
| 14 | `LiveAiServiceImpl.java` | 修改 | buildPrompt 增加 RAG 参考注入（XML 格式 + Token 预算 + 多查询 + 最低分过滤 + 元数据过滤 + 来源归因） |
| 15 | `ProductScriptServiceImpl.java` | 修改 | 生成时注入 RAG 上下文，扩展覆盖情绪/过渡/短视频 |
| 16 | `application.yml` | 修改 | 新增 dedup、rag、security 配置项 |
| 17 | `AiKbDocument.java` | 修改 | 新增 contentFingerprint、simhash、metadata 字段 |
| 18 | `AiKbDocumentRepository.java` | 修改 | 新增 fingerprint/simhash 查询方法 |
| 19 | 前端知识库页面 | 修改 | 文件上传组件、格式提示、去重预览、分类浏览、chunk 预览 |
| 20 | 前端话术生成对话框 | 修改 | RAG 开关 + 参考来源展示 + 反馈按钮 |
| 21 | 前端导入对话框 | 修改 | 增量导入选项、文档类型选择 |
| 22 | SQL 迁移脚本 | **新建** | ai_kb_document 新字段 + kb_import_report + kb_import_checkpoint |

---

## 十二、实施步骤

### Phase 1：文件解析 + 目录导入扩展

| # | 任务 | 依赖 | 升级项 |
|---|------|------|--------|
| 1 | pom.xml 添加 PDFBox + poi-scratchpad | - | - |
| 2 | 新建 DocumentParser 工具类（doc/docx/pdf/md/txt） | 1 | - |
| 3 | DocumentParser 增加 magic bytes 校验 | 2 | #19 |
| 4 | DocumentParser 增加加密文件检测 | 2 | #2 |
| 5 | DocumentParser 增加扫描件检测 | 2 | #1 |
| 6 | DocumentParser 增加元数据提取（parseWithMetadata） | 2 | #3 |
| 7 | 新建 DocumentTypeDetector（自动检测文档类型） | - | #4 (P0) |
| 8 | 新建 ScriptAwareChunker（话术文档智能分块） | - | - |
| 9 | 新建 MixedDocumentProcessor（混合文档分段处理） | 7, 8 | #5 |
| 10 | 新建 ChunkLabeler（chunk 标签自动打标） | - | #6 |
| 11 | 修改 KnowledgeBaseImportServiceImpl：多层级目录遍历增强（walkDirectory） | 2 | #31 (P1) |
| 12 | 实现目录结构元数据保留（extractDirMetadata） | 11 | #31 |
| 13 | 实现目录级错误隔离（importByDirectory） | 11 | #31 |
| 14 | 修改 KnowledgeBaseServiceImpl.uploadDocument()：支持多种分块策略 | 7, 8, 9 | - |
| 15 | KbImportVO 新增 contentType 字段 | - | - |
| 16 | 测试：准备 doc/pdf 样本文件（含加密/扫描件/混合文档/多层目录），验证导入流程 | 3-14 | - |

### Phase 2：去重机制

| # | 任务 | 依赖 | 升级项 |
|---|------|------|--------|
| 17 | SQL 迁移：ai_kb_document 新增 content_fingerprint、simhash 字段 | - | #7 |
| 18 | 实现文档级快速预检（MD5 指纹 + SimHash，含短文本保护） | 17 | #7, #26 |
| 19 | application.yml 新增 dedup 配置项 | - | - |
| 20 | 修改 uploadDocument()：分块后向量去重逻辑 | 19 | - |
| 21 | 实现批量去重查询优化（Milvus 批量搜索） | 20 | #9 |
| 22 | 修改 hybridSearch()：结果语义去重 | - | - |
| 23 | 新增去重预览接口（dedup-preview） | 20 | #8 |
| 24 | 测试：导入重复/近似内容文档，验证三级去重效果 | 18-22 | - |

### Phase 3：RAG 话术集成

| # | 任务 | 依赖 | 升级项 |
|---|------|------|--------|
| 25 | KnowledgeBaseInitializer 新增 huashu 知识库 | - | - |
| 26 | application.yml 新增 rag 配置项（含 Token 预算） | - | #28 |
| 27 | LiveAiServiceImpl.buildPrompt() 增加 RAG 注入（XML 格式） | 25, 26 | #27 |
| 28 | 实现两阶段查询扩展策略（buildCategoryQueries + queryRewriteService） | 27 | #10 (P0) |
| 29 | 实现最低相关度过滤 | 27 | #11 (P0) |
| 30 | 实现 Token 预算管理（calculateRagTokenBudget） | 27 | #28 |
| 31 | hybridSearch 新增 metadataFilter + skipCache 参数重载 | 10 | #12, #21 |
| 32 | ProductScriptServiceImpl 增加 RAG 注入 | 27 | - |
| 33 | 扩展 RAG 覆盖至情绪/过渡/短视频话术 | 27 | #13 |
| 34 | 实现 RAG 来源归因 + boost 反馈联动 | 27 | #14 |
| 35 | 测试：生成话术时验证多查询检索 + 过滤 + XML Prompt 注入 | 28-34 | - |

### Phase 4：运维 + 安全

| # | 任务 | 依赖 | 升级项 |
|---|------|------|--------|
| 36 | SQL 迁移：新增 kb_import_report + kb_import_checkpoint 表 | - | #15, #16 |
| 37 | 实现导入报告持久化 | 36 | #15 |
| 38 | 实现增量导入功能 | 36, 11 | #16 |
| 39 | 新建 ContentSecurityScanner | - | #20 |
| 40 | 在上传/导入流程中集成安全扫描 | 39 | #20 |
| 41 | application.yml 新增 security 配置项 | - | #19, #20 |
| 42 | 测试：安全扫描（隐私信息/违禁用语/伪装文件）| 39-41 | - |

### Phase 5：API + 前端

| # | 任务 | 依赖 | 升级项 |
|---|------|------|--------|
| 43 | KnowledgeBaseController 新增 upload-file / upload-files 端点 | 2 | #30 |
| 44 | KnowledgeBaseController 新增 dedup-preview 端点 | 23 | #8 |
| 45 | KnowledgeBaseController 新增 import-incremental 端点 | 38 | #16 |
| 46 | KnowledgeBaseController 新增 chunks 查看端点（POST） | - | #18, #29 |
| 47 | 前端知识库页面：文件拖拽上传组件（支持 doc/pdf） | 43 | - |
| 48 | 前端去重预览对话框 | 44 | #8 |
| 49 | 前端话术生成对话框：RAG 开关 + 参考来源 + 反馈 | 27, 34 | #14 |
| 50 | 前端知识库分类浏览 + 标签过滤 | 10 | #17 |
| 51 | 前端文档 chunk 预览 | 46 | #18 |
| 52 | 前端导入对话框：增量导入 + 文档类型选择 + 目录结构展示 | 38, 45 | #16, #31 |
| 53 | 前端导入进度增强：目录级进度展示 | 13 | #31 |

---

## 十三、测试验证

| # | 场景 | 验证点 | 升级项 |
|---|------|--------|--------|
| 1 | 导入 .docx 文件 | DocumentParser 正确提取文本，含表格内容 | - |
| 2 | 导入 .doc 文件（旧版） | poi-scratchpad 正确解析二进制格式 | - |
| 3 | 导入 .pdf 文件 | PDFBox 正确提取，多页无遗漏 | - |
| 4 | 导入含中文的 PDF | 编码正确，无乱码 | - |
| 5 | 导入加密 PDF | 抛出明确错误提示，不崩溃 | #2 |
| 6 | 导入加密 DOCX | 抛出明确错误提示 | #2 |
| 7 | 导入扫描件 PDF（图片型） | 检测提示"疑似扫描件" | #1 |
| 8 | 导入低文字密度 PDF | 日志警告，正常导入 | #1 |
| 9 | 导入 .exe 伪装为 .pdf | magic bytes 校验拦截 | #19 |
| 10 | 提取 DOCX/PDF 元数据 | 标题、作者、关键词正确提取 | #3 |
| 11 | 自动检测话术文档 | 含序号+话术关键词 → 检测为 "script" | #4 |
| 12 | 自动检测通用文档 | 长段落+技术内容 → 检测为 "general" | #4 |
| 13 | 混合文档分段处理 | 话术部分按条目切，说明部分按段落切 | #5 |
| 14 | chunk 自动标签 | "种草"/"促销"/"产品介绍"等标签正确 | #6 |
| 15 | 话术文档智能分块 | 按序号/标题正确切分，每条话术完整 | - |
| 16 | 文档级 MD5 去重 | 完全相同文档直接跳过，不进入分块 | #7 |
| 17 | 文档级 SimHash 近似检测 | 近似文档标记后进入 chunk 级精细判断 | #7 |
| 18 | 导入重复内容 | 相似度 > 0.92 的 chunk 被跳过 | - |
| 19 | 导入近似内容 | 0.80-0.92 的 chunk 降权入库 | - |
| 20 | 批量去重查询 | 100 个 chunk 批量搜索正常工作 | #9 |
| 21 | 检索结果去重 | 相似结果只返回得分最高的一条 | - |
| 22 | 去重预览（dry-run） | 返回正确的 skip/downweight/keep 统计 | #8 |
| 23 | RAG 多查询检索 | 多维度查询，合并去重后返回结果 | #10 |
| 24 | RAG 最低分过滤 | 低于 0.5 的结果不注入 Prompt | #11 |
| 25 | RAG 元数据过滤 | 按标签精准检索话术 | #12 |
| 26 | RAG 话术生成 | Prompt 中包含参考案例，生成质量提升 | - |
| 27 | RAG 关闭时 | 不影响现有生成流程 | - |
| 28 | RAG 覆盖情绪/过渡话术 | 对应场景正确注入参考 | #13 |
| 29 | RAG 来源归因 | 返回结果含参考 chunk 信息 | #14 |
| 30 | RAG boost 反馈 | 反馈后 chunk 的 boost_factor 正确调整 | #14 |
| 31 | 导入报告持久化 | 导入完成后报告写入数据库 | #15 |
| 32 | 增量导入 | 仅处理新增/修改文件 | #16 |
| 33 | 知识库分类浏览 | 按标签过滤文档列表 | #17 |
| 34 | chunk 预览 | 查看文档的分块详情和标签 | #18 |
| 35 | 内容安全扫描-隐私 | 检测手机号/邮箱并记录警告 | #20 |
| 36 | 内容安全扫描-违禁 | 检测绝对化用语并记录警告 | #20 |
| 37 | 批量目录导入 100 个 doc/pdf | 并行处理正常，进度准确，错误汇总正确 | - |
| 38 | 50MB 大文件上传 | 不 OOM，解析正常 | - |
| 39 | 多层级目录导入（3 层嵌套） | 正确递归遍历，目录结构写入 metadata | #31 |
| 40 | 目录含隐藏文件夹（.git） | 自动跳过隐藏目录，不导入 | #31 |
| 41 | 目录深度超过 max-depth | 超限深度文件被忽略，日志提示 | #31 |
| 42 | 目录文件数超过 max-files | 达上限后停止遍历，返回警告 | #31 |
| 43 | 目录中含符号链接 | follow-symlinks=true 时跟随，false 时跳过 | #31 |
| 44 | 单目录内文件失败不影响其他目录 | 目录 A 有错误文件，目录 B 正常导入 | #31 |
| 45 | 目录名推断品类标签 | "美妆护肤/种草话术/xxx.docx" → cat:美妆护肤 标签 | #31 |
| 46 | 前端按目录结构浏览导入文档 | relativePath 正确，可按目录折叠展示 | #31 |

---

## 十四、风险与注意事项

| 风险 | 概率 | 影响 | 应对 |
|------|------|------|------|
| PDF 扫描件（图片型）无法提取文字 | 中 | 导入为空内容 | [升级] 文字密度检测 + 明确提示"疑似扫描件"，建议 OCR |
| DOC 文件含复杂格式（嵌套表格、文本框） | 低 | 部分内容丢失 | 仅提取可识别的段落和表格文本，忽略浮动文本框 |
| 加密 PDF/DOCX 解析崩溃 | 中 | 批量导入中断 | [升级] 加密检测前置，抛出友好提示，不影响其他文件 |
| 伪装文件上传（.exe→.pdf） | 低 | 安全隐患 | [升级] magic bytes 校验拦截 |
| 大量话术去重导致导入变慢 | 低 | 每 chunk +10ms | [升级] 批量查询优化（100 chunk ~50ms）；可关闭去重 |
| RAG 检索结果不相关，污染 Prompt | 中 | 生成质量下降 | [升级] 最低相似度 0.5 过滤 + 元数据标签精准检索 |
| RAG 检索命中旧缓存 | 高 | 新导入话术不可检索 | [P0 修正] hybridSearch 新增 skipCache 参数 + 导入后主动清除缓存 |
| Prompt 超长（RAG 参考 + 产品信息 + 人设） | 低 | LLM 截断 | [P1 修正] Token 预算管理：动态计算可用 RAG 上下文长度 + 硬上限 2000 字符双重保护 |
| 自动类型检测误判 | 低 | 分块策略不当 | 多信号综合判断 + 支持手动覆盖 |
| 导入含个人隐私信息 | 中 | 合规风险 | [升级] 内容安全扫描 + 警告（不阻断，人工审核） |
| SimHash 碰撞（不同内容相似哈希） | 极低 | 误判为重复 | SimHash 仅做预检，最终以 chunk 级向量去重为准 |
| 多层级目录含符号链接循环 | 低 | 无限递归导致 OOM | [P1 新增] max-depth 限制 + FOLLOW_LINKS 仅限显式配置 |
| 超大目录（万级文件）遍历耗时 | 中 | 导入启动慢 | [P1 新增] max-files 上限 + 按目录分组并行 |
| 目录名非标准编码（含特殊字符） | 低 | 路径解析错误 | 使用 Path API（非字符串拼接），UTF-8 统一编码 |

---

## 十五、升级项汇总索引

| # | 升级项 | 优先级 | 所属层 | 文档章节 |
|---|--------|--------|--------|----------|
| 1 | 扫描件 PDF 检测 + OCR 提示 | P1 | 解析层 | 二、2.2 |
| 2 | 加密文件（PDF/DOC/DOCX）检测与友好提示 | P1 | 解析层 | 二、2.3 |
| 3 | 文档元数据提取（标题/作者/关键词/页数） | P2 | 解析层 | 二、2.4 |
| 4 | 文档类型自动检测（script/mixed/general） | **P0** | 分块层 | 三、3.4 |
| 5 | 混合文档分段处理 | P2 | 分块层 | 三、3.5 |
| 6 | Chunk 业务标签自动打标 | P1 | 分块层 | 三、3.6 |
| 7 | 文档级快速预检（MD5 指纹 + SimHash） | P1 | 去重层 | 四、4.1 |
| 8 | 去重预览 / Dry-Run 模式 | P2 | 去重层 | 四、4.5 |
| 9 | 去重批量查询优化（Milvus 批量搜索） | P2 | 去重层 | 四、4.3 |
| 10 | RAG 两阶段查询扩展（品类维度 + 复用 queryRewriteService） | **P0** | RAG 层 | 六、6.2.2 |
| 11 | RAG 最低相关度过滤 | **P0** | RAG 层 | 六、6.2.3 |
| 12 | RAG 元数据过滤检索（按标签精准检索） | P1 | RAG 层 | 六、6.2.4 |
| 13 | RAG 覆盖情绪/过渡/短视频话术场景 | P1 | RAG 层 | 六、6.2.6 |
| 14 | RAG 来源归因 + Boost 反馈联动 | P2 | RAG 层 | 六、6.2.7 |
| 15 | 导入报告持久化 | P1 | 运维层 | 九、9.2 |
| 16 | 增量导入（仅处理新增/修改文件） | P2 | 运维层 | 五、5.4 |
| 17 | 知识库分类浏览 + 标签管理 | P2 | 运维层 | 八、8.5 |
| 18 | 文档 Chunk 预览 | P2 | 运维层 | 八、8.6 |
| 19 | Magic Bytes 文件类型校验 | P1 | 安全层 | 十、10.1 |
| 20 | 内容安全扫描（隐私/违禁/编码异常） | P1 | 安全层 | 十、10.2 |
| 21 | RAG 检索跳过缓存 + 导入后缓存清除 | **P0** | RAG 层 | 六、6.2.4（hybridSearch 扩展） |
| 22 | 新表逻辑删除字段（deleted + update_time） | **P0** | 数据库层 | 九、9.2-9.4 |
| 23 | PDFBox 3.x API 修正（RandomAccessReadBufferedFile） | **P1** | 解析层 | 二、2.1（parsePdf） |
| 24 | 话术关键词配置化（从 YAML/数据库加载） | **P1** | 分块层 | 三、3.4（DocumentTypeDetector） |
| 25 | 标签体系统一（三级标签 + 与 classifyByContent 对齐） | **P1** | 分块层 | 三、3.6（ChunkLabeler） |
| 26 | SimHash 短文本保护（< 200 字跳过） | **P1** | 去重层 | 四、4.1 |
| 27 | RAG Prompt 结构化 XML 格式 | **P1** | RAG 层 | 六、6.2.1 |
| 28 | Token 预算管理（动态计算 RAG 上下文长度） | **P1** | RAG 层 | 六、6.2.1 |
| 29 | chunks API 统一 POST 规范 | **P1** | API 层 | 八、8.6 |
| 30 | 批量上传 max-request-size 提升至 200MB | **P1** | API 层 | 五、5.1 |
| 31 | 多层级目录导入（深度限制/隐藏过滤/结构保留/错误隔离） | **P1** | 导入层 | 五、5.3 |
| 32 | huashu 库按名称查找或创建（不写死 ID） | **P1** | 初始化 | 七、7.1 |
| 33 | RAG 多查询并行检索（CompletableFuture） | **P1** | RAG 层 | 六、6.2.1 |
| 34 | RAG 注入统一在 Live 侧 buildXxxPrompt | **P1** | RAG 层 | 六、6.2.5 |
| 35 | Milvus 批量去重降级路径（先逐条，后批量） | **P1** | 去重层 | 四、4.3 |
| 36 | 文档解析专用错误码（4060-4066） | **P1** | 错误码 | 十点五 |

---

## 变更日志

| 版本 | 日期 | 内容 |
|------|------|------|
| v1.0 | 2026-03-04 | 初始版本：DOC/PDF 解析、智能分块、去重、RAG 集成 |
| v2.0 | 2026-03-04 | 合并 20 项升级：解析层（扫描件检测、加密处理、元数据提取）、分块层（自动类型检测、混合文档处理、chunk 标签）、去重层（文档级预检、预览模式、批量优化）、RAG 层（多查询、最低分过滤、元数据过滤、扩展覆盖、归因反馈）、运维层（报告持久化、增量导入、分类浏览、chunk 预览）、安全层（magic bytes、内容安全扫描） |
| v2.1 | 2026-03-04 | P0 修正合并：A11 — RAG 检索新增 skipCache 参数 + 导入后主动清除知识库缓存；A12 — 多查询扩展改为"两阶段查询扩展"，品类维度扩展（buildCategoryQueries）+ 复用现有 queryRewriteService 语义改写，避免重复实现；A18 — 3 张新表（kb_import_report/kb_import_dedup_log/kb_import_checkpoint）补全 deleted + update_time 字段。新增决策 D10（RAG 缓存策略）、更新 D8、新增升级项 #21/#22、新增风险条目 |
| v2.2 | 2026-03-04 | P1 修正合并（9 项）：A3 — PDFBox 3.x API 修正（RandomAccessReadBufferedFile）；A5 — 话术关键词从硬编码改为配置化（YAML/数据库），新增服装/家居/数码行业关键词；A7 — ChunkLabeler 重构为三级标签体系（type:/cat: 前缀），与 classifyByContent 对齐；A8 — SimHash 增加短文本保护（< 200 字跳过）；A13 — RAG Prompt 注入改用结构化 XML 标签（`<reference_scripts>`）；A14 — Token 预算管理，动态计算可用 RAG 上下文长度；A15 — chunks 查看 API 从 GET 改为 POST（统一规范）；A16 — max-request-size 提升至 200MB。**新增**：多层级目录导入完整方案（5.3.1-5.3.6）— 深度限制、隐藏目录过滤、符号链接处理、目录结构元数据保留（relativePath/dirCategory）、目录级错误隔离、进度增强。新增决策 D11、升级项 #23-#31、测试用例 #39-#46、实施任务 53 项。 |
| v2.3 | 2026-03-04 | Cursor 评审修正合并（5 项）：C1 — huashu 库改为 createIfAbsent 按名称查找或创建，resolveScriptKbId 按名称查找（不写死 ID=4）；C2 — RAG 多查询从串行改为 CompletableFuture 并行检索（~1-2s→~200ms），5s 超时保护；C3 — RAG 注入统一在 Live 侧 buildXxxPrompt（ProductScript 不独立做 RAG）；C4 — Milvus 批量去重明确降级路径（Phase 2 先逐条，后续封装 batchSearch）；C5 — 新增文档解析专用错误码 4060-4066（加密/扫描件/伪装/解析失败/超限）。新增升级项 #32-#36。 |
