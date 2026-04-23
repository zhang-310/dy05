# 风格融合模式实现文档

## 实现时间
2026-04-04

## 功能概述
风格融合模式允许用户选择多个话术风格（如"专业+热情+温暖"），系统将这些风格融合为一条连贯、自然的话术，而不是生成多条独立的话术。

## 核心逻辑

### 1. 生成流程

```
用户选择多个风格 + 开启融合模式
  ↓
后端接收 fusionMode=true
  ↓
为每个风格生成独立片段（时长平分）
  ↓
使用 AI 融合多个片段
  ↓
合规检测
  ↓
保存融合话术（style 字段记录所有风格，如 "professional+warm+enthusiastic"）
  ↓
返回单条融合话术
```

### 2. 关键实现

#### 后端实现

**ProductScriptServiceImpl.java**

```java
// 同步生成
@Override
public MultiStyleGenerateResultVO generateMultiStyleScripts(MultiStyleGenerateRequestVO vo, Long userId) {
    // ...

    // 风格融合模式：多风格融合为一条话术
    if (vo.getFusionMode() != null && vo.getFusionMode() && finalStyles.size() >= 2) {
        return generateFusionScript(vo, finalStyles, product, duration, userId, result);
    }

    // 普通模式：每个风格独立生成
    // ...
}

// 风格融合生成
private MultiStyleGenerateResultVO generateFusionScript(
        MultiStyleGenerateRequestVO vo, List<String> styles,
        DyProduct product, int duration, Long userId,
        MultiStyleGenerateResultVO result) {

    // 1. 为每个风格生成独立话术片段（时长平分）
    List<String> styleContents = new ArrayList<>();
    int totalTokens = 0;

    for (String style : styles) {
        ProductAiService.ProductScriptAiResult aiResult =
            productAiService.generateScript(
                vo.getProductId(), vo.getScriptType(), style,
                vo.getPersonaId(), duration / styles.size(),  // 时长平分
                userId, vo.getUseKbRef(), vo.getScene(), vo.getKbCategories()
            );
        if (aiResult.scriptContent() != null && !aiResult.scriptContent().isBlank()) {
            styleContents.add(aiResult.scriptContent());
            totalTokens += aiResult.tokenUsage();
        }
    }

    // 2. 使用 AI 融合多个风格片段
    String fusedContent = fuseStyleContents(
        product, vo.getScriptType(), styles, styleContents, duration, userId
    );

    // 3. 合规检测
    ComplianceService.ComplianceResult comp = complianceService.checkAndFix(fusedContent);
    fusedContent = comp.fixedText();

    // 4. 保存融合话术（style 字段记录所有风格，用 + 连接）
    DyProductScript script = self.saveProductScriptWithLock(
        vo.getProductId(), vo.getScriptType(),
        String.join("+", styles),  // 如 "professional+warm+enthusiastic"
        fusedContent, vo.getPersonaId(), duration, totalTokens, userId, vo.getScene()
    );

    return result;
}

// AI 融合逻辑
private String fuseStyleContents(
        DyProduct product, String scriptType, List<String> styles,
        List<String> contents, int duration, Long userId) {

    String systemPrompt = """
        你是直播话术融合专家。任务：将多个不同风格的话术片段融合为一条连贯、自然的话术。

        要求：
        1. 保留各风格的核心特点，自然过渡
        2. 避免风格冲突，确保整体协调
        3. 保持话术流畅性和感染力
        4. 控制在指定时长内
        5. 只输出融合后的话术正文，无标题无解释
        """;

    StringBuilder userPrompt = new StringBuilder();
    userPrompt.append("商品：").append(product.getProductName()).append("\n");
    userPrompt.append("话术类型：").append(scriptType).append("\n");
    userPrompt.append("目标时长：").append(duration).append("秒\n");
    userPrompt.append("融合风格：").append(String.join("、", styles)).append("\n\n");

    for (int i = 0; i < styles.size() && i < contents.size(); i++) {
        userPrompt.append("【").append(styles.get(i)).append("风格片段】\n");
        userPrompt.append(contents.get(i)).append("\n\n");
    }

    userPrompt.append("请将以上片段融合为一条连贯的话术，保留各风格特点，自然过渡。只输出话术正文：");

    LlmClient.LlmResponse response = llmClient.chatWithFallback(
        models, systemPrompt, userPrompt.toString()
    );

    return response.content().trim();
}
```

**SSE 异步生成支持**

```java
@Override
public void generateMultiStyleScriptsWithProgress(
        MultiStyleGenerateRequestVO vo, Long userId, SseEmitter emitter) {

    aiTaskExecutor.execute(() -> {
        // ...

        // 风格融合模式
        if (vo.getFusionMode() != null && vo.getFusionMode() && finalStyles.size() >= 2) {
            Map<String, Object> progressData = new HashMap<>();
            progressData.put("style", String.join("+", finalStyles));
            progressData.put("status", "loading");
            progressData.put("fusionMode", true);
            emitter.send(SseEmitter.event().name("progress").data(progressData));

            try {
                // 生成各风格片段
                List<String> styleContents = new ArrayList<>();
                int totalTokens = 0;
                for (String style : finalStyles) {
                    ProductAiService.ProductScriptAiResult aiResult =
                        productAiService.generateScript(...);
                    styleContents.add(aiResult.scriptContent());
                    totalTokens += aiResult.tokenUsage();
                }

                // AI 融合
                String fusedContent = fuseStyleContents(...);

                // 合规检测
                ComplianceService.ComplianceResult comp =
                    complianceService.checkAndFix(fusedContent);
                fusedContent = comp.fixedText();

                // 保存
                DyProductScript script = self.saveProductScriptWithLock(...);

                progressData.put("status", "done");
                progressData.put("success", true);
                emitter.send(SseEmitter.event().name("progress").data(progressData));
            } catch (Exception e) {
                progressData.put("status", "failed");
                progressData.put("message", "融合失败: " + e.getMessage());
                emitter.send(SseEmitter.event().name("progress").data(progressData));
            }
        } else {
            // 普通模式：每个风格独立生成
            // ...
        }
    });
}
```

#### 前端实现

**ScriptGeneratePanel.tsx**

```tsx
// 融合模式开关
<FormControlLabel
  control={
    <Switch
      checked={fusionMode}
      onChange={(e, v) => setFusionMode(v)}
      color="secondary"
      size="small"
    />
  }
  label={
    <Typography variant="body2" color="text.secondary">
      <MergeTypeIcon sx={{ fontSize: 16 }} /> 风格融合
    </Typography>
  }
/>

{fusionMode && (
  <Typography variant="caption" color="text.disabled">
    多风格融合为一条话术，适合长时段混合讲解
  </Typography>
)}

// 生成按钮
<Button
  variant="contained"
  startIcon={fusionMode ? <MergeTypeIcon /> : <AutoAwesomeIcon />}
  onClick={onStart}
  disabled={genStyles.length === 0 || (fusionMode && genStyles.length < 2)}
>
  {fusionMode
    ? `融合生成（${genStyles.length} 种风格 → 1 条话术）`
    : `生成 ${genStyles.length} 条话术`}
</Button>
```

**ProductScriptManagePage.tsx**

```tsx
// SSE 调用
generateMultiStyleScriptsSse(
  {
    productId: Number(productId),
    styles: genStyles,
    scriptType,
    fusionMode,  // 传递融合模式标志
    personaId: genPersonaId === '' ? undefined : genPersonaId,
    duration: genDuration,
    scene: genScene || undefined,
    useKbRef,
    kbCategories: selectedKbCategories,
  },
  {
    onProgress: (event) => {
      // 处理进度事件
      // event.fusionMode 标识是否为融合模式
      // event.style 在融合模式下为 "professional+warm+enthusiastic"
    },
    onDone: () => {
      toast('话术生成完成', 'success')
      loadScripts()
    },
    onError: (err) => {
      toast(`生成失败: ${err.message}`, 'error')
    },
  }
)
```

### 3. 数据存储

**dy_product_script 表**

融合话术的 `style` 字段存储格式：
```
professional+warm+enthusiastic
```

查询时可通过 `LIKE '%professional%'` 查找包含特定风格的话术。

### 4. UI 交互

#### 配置面板
- 用户选择多个风格（至少 2 个）
- 开启"风格融合"开关
- 按钮文案变为"融合生成（3 种风格 → 1 条话术）"
- 融合模式下，生成按钮要求至少选择 2 个风格

#### 进度展示
- 融合模式下，进度条显示单个融合任务
- style 显示为 "professional+warm+enthusiastic"
- 标签显示"融合模式"

#### 结果展示
- 生成 1 条融合话术
- 话术列表中 style 显示为组合风格
- 可正常激活、编辑、删除

## 使用场景

### 适用场景
1. **长时段直播**：需要在一条话术中展现多种风格，避免单一风格疲劳
2. **混合讲解**：开场专业介绍 → 中段热情推荐 → 结尾温暖关怀
3. **风格过渡**：自然融合多种风格，避免生硬切换
4. **个性化定制**：根据主播特点，定制专属风格组合

### 不适用场景
1. **A/B 测试**：需要独立对比不同风格效果
2. **风格切换**：需要在不同时段使用不同风格
3. **单一风格**：只需要一种风格的话术

## 技术优势

1. **智能融合**：AI 自动处理风格冲突，确保话术连贯
2. **时长控制**：自动平分时长，确保各风格均衡
3. **合规保障**：融合后的话术仍需通过合规检测
4. **灵活组合**：支持任意风格组合，最多 10 种
5. **实时反馈**：SSE 推送融合进度，用户体验流畅

## 性能考虑

### Token 消耗
- 普通模式：N 个风格 = N 次 AI 调用
- 融合模式：N 个风格 = N+1 次 AI 调用（N 次片段生成 + 1 次融合）

### 生成时间
- 融合模式比普通模式多 1 次 AI 调用
- 但最终只生成 1 条话术，总体时间可能更短

### 建议
- 融合模式适合 2-4 个风格
- 超过 4 个风格，融合效果可能下降

## 测试验证

### 单元测试
```bash
mvn test -Dtest=ProductScriptServiceTest#testFusionMode
```

### 集成测试
1. 选择 3 个风格：专业、热情、温暖
2. 开启融合模式
3. 生成话术
4. 验证：
   - 生成 1 条话术
   - style 字段为 "professional+enthusiastic+warm"
   - 话术内容包含 3 种风格特点
   - 话术连贯自然

## 后续优化

1. **风格权重**：允许用户设置各风格的权重比例
2. **融合策略**：支持顺序融合、交叉融合等多种策略
3. **预览功能**：生成前预览各风格片段
4. **智能推荐**：根据商品属性推荐最佳风格组合
5. **效果分析**：统计融合话术的使用效果

## 总结

风格融合模式已完整实现，核心功能包括：
- ✅ 多风格片段生成
- ✅ AI 智能融合
- ✅ 合规检测
- ✅ SSE 实时进度
- ✅ 前端 UI 支持
- ✅ 数据存储

系统已具备生产环境部署条件。
