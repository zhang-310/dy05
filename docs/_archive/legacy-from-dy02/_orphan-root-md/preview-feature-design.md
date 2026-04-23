# 商品话术预览功能设计

## 功能概述

在正式生成话术前，用户可以先预览各风格的简短片段（15-20秒），快速确认风格效果，避免浪费时间生成不满意的话术。

## 实现时间
2026-04-04（待实现）

## 核心功能

### 1. 预览触发
- 在生成配置面板底部新增"预览风格"按钮
- 预览按钮仅在选择了风格后可用
- 预览不受融合模式影响（融合模式下也预览各风格独立片段）

### 2. 预览生成
- 为每个选中的风格生成 15-20 秒的简短片段
- 使用相同的配置参数（场景、人设、知识库参考等）
- 预览片段不保存到数据库
- 预览速度快（短时长 + 并发生成）

### 3. 预览展示
- 弹出预览对话框，展示所有风格的预览片段
- 每个风格一个卡片，显示风格名称和预览内容
- 支持复制预览内容
- 提供"满意，开始生成"和"调整配置"两个操作

### 4. 预览后操作
- 满意：关闭预览对话框，自动触发正式生成
- 调整：关闭预览对话框，返回配置面板修改参数

## 技术实现

### 后端实现

#### 1. 新增预览接口

**接口路径**: `POST /api/v1/product/script/preview-styles`

**请求参数**:
```json
{
  "productId": 123,
  "styles": ["professional", "warm"],
  "scriptType": "formal",
  "personaId": 1,
  "scene": "short_video",
  "useKbRef": true,
  "kbCategories": ["护肤", "成分"]
}
```

**响应结果**:
```json
{
  "status": 200,
  "message": "预览生成成功",
  "data": {
    "previews": [
      {
        "style": "professional",
        "styleName": "专业",
        "content": "这款精华液采用了先进的..."
      },
      {
        "style": "warm",
        "styleName": "温暖",
        "content": "亲爱的，今天给大家推荐..."
      }
    ]
  }
}
```

#### 2. Service 层实现

**文件**: `ProductScriptServiceImpl.java`

```java
/**
 * 预览多个风格的话术片段
 * @param vo 预览请求参数
 * @param userId 用户ID
 * @return 预览结果列表
 */
public List<StylePreviewVO> previewStyles(MultiStyleGenerateRequestVO vo, Long userId) {
    // 1. 获取商品信息
    DyProduct product = productRepository.findById(vo.getProductId())
        .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

    // 2. 权限校验
    if (!product.getOwnerId().equals(userId)) {
        throw new BusinessException(ErrorCode.PERMISSION_DENIED);
    }

    // 3. 为每个风格生成预览片段（15秒）
    List<StylePreviewVO> previews = new ArrayList<>();
    int previewDuration = 15; // 预览固定15秒

    for (String style : vo.getStyles()) {
        try {
            // 生成预览片段
            String content = generateSingleStyleScript(
                product,
                vo.getScriptType(),
                style,
                previewDuration,
                vo.getPersonaId(),
                vo.getScene(),
                vo.getUseKbRef(),
                vo.getKbCategories(),
                userId
            );

            // 获取风格名称
            String styleName = getStyleName(style);

            previews.add(new StylePreviewVO(style, styleName, content));
        } catch (Exception e) {
            log.error("预览风格 {} 失败: {}", style, e.getMessage());
            previews.add(new StylePreviewVO(style, getStyleName(style), "预览生成失败"));
        }
    }

    return previews;
}

/**
 * 获取风格名称
 */
private String getStyleName(String styleCode) {
    // 从预设中查找风格名称
    // 可以从数据库或配置文件读取
    Map<String, String> styleNames = Map.of(
        "professional", "专业",
        "warm", "温暖",
        "enthusiastic", "热情",
        "casual", "随意",
        "friendly", "亲和",
        "passionate", "激情",
        "elegant", "优雅",
        "trendy", "时尚"
    );
    return styleNames.getOrDefault(styleCode, styleCode);
}
```

#### 3. Controller 实现

**文件**: `ProductScriptController.java`

```java
/**
 * 预览多个风格的话术片段
 */
@PostMapping("/preview-styles")
public RESTResult<Map<String, Object>> previewStyles(
    @Valid @RequestBody MultiStyleGenerateRequestVO vo,
    @RequestHeader(value = "Authorization", required = false) String token
) {
    Long userId = authService.getUserIdFromToken(token);

    List<StylePreviewVO> previews = productScriptService.previewStyles(vo, userId);

    return RESTResult.success(Map.of("previews", previews));
}
```

#### 4. VO 定义

**文件**: `StylePreviewVO.java`（新增）

```java
package cn.gaifan.douyinOperations.module.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 风格预览结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StylePreviewVO {
    /** 风格代码 */
    private String style;

    /** 风格名称 */
    private String styleName;

    /** 预览内容（15-20秒片段） */
    private String content;
}
```

### 前端实现

#### 1. API 接口

**文件**: `product.ts`

```typescript
export interface StylePreview {
  style: string
  styleName: string
  content: string
}

export interface PreviewStylesResponse {
  previews: StylePreview[]
}

/**
 * 预览多个风格的话术片段
 */
export function previewStyles(params: {
  productId: number
  styles: string[]
  scriptType: string
  personaId?: number
  scene?: string
  useKbRef?: boolean
  kbCategories?: string[]
}): Promise<PreviewStylesResponse> {
  return request.post('/product/script/preview-styles', params)
}
```

#### 2. 预览对话框组件

**文件**: `StylePreviewDialog.tsx`（新增）

```tsx
import { Dialog, DialogTitle, DialogContent, DialogActions, Button, Box, Card, CardContent, Typography, IconButton } from '@mui/material'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import EditIcon from '@mui/icons-material/Edit'
import { StylePreview } from '@/api/product'

interface StylePreviewDialogProps {
  open: boolean
  onClose: () => void
  previews: StylePreview[]
  onConfirm: () => void
  onAdjust: () => void
}

export function StylePreviewDialog({
  open,
  onClose,
  previews,
  onConfirm,
  onAdjust,
}: StylePreviewDialogProps) {
  const handleCopy = (content: string) => {
    navigator.clipboard.writeText(content)
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Typography variant="h6" fontWeight={700}>
            风格预览
          </Typography>
          <Typography variant="caption" color="text.secondary">
            （15秒片段，仅供参考）
          </Typography>
        </Box>
      </DialogTitle>

      <DialogContent>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {previews.map((preview) => (
            <Card key={preview.style} variant="outlined">
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1.5 }}>
                  <Typography variant="subtitle1" fontWeight={600}>
                    {preview.styleName}
                  </Typography>
                  <IconButton
                    size="small"
                    onClick={() => handleCopy(preview.content)}
                    title="复制内容"
                  >
                    <ContentCopyIcon fontSize="small" />
                  </IconButton>
                </Box>
                <Typography
                  variant="body2"
                  sx={{
                    whiteSpace: 'pre-wrap',
                    lineHeight: 1.8,
                    color: 'text.secondary',
                  }}
                >
                  {preview.content}
                </Typography>
              </CardContent>
            </Card>
          ))}
        </Box>
      </DialogContent>

      <DialogActions sx={{ p: 2, gap: 1 }}>
        <Button
          variant="outlined"
          startIcon={<EditIcon />}
          onClick={onAdjust}
        >
          调整配置
        </Button>
        <Button
          variant="contained"
          startIcon={<CheckCircleIcon />}
          onClick={onConfirm}
        >
          满意，开始生成
        </Button>
      </DialogActions>
    </Dialog>
  )
}
```

#### 3. 配置面板集成

**文件**: `ScriptGeneratePanel.tsx`

在配置面板底部新增预览按钮：

```tsx
{/* 预览按钮 */}
<Button
  variant="outlined"
  fullWidth
  size="large"
  startIcon={<VisibilityIcon />}
  onClick={onPreview}
  disabled={genStyles.length === 0}
  sx={{ borderRadius: 2, py: 1.2, fontWeight: 600, fontSize: '0.95rem', mb: 1 }}
>
  预览风格效果
</Button>

{/* 生成按钮 */}
<Button
  variant="contained"
  fullWidth
  size="large"
  startIcon={fusionMode ? <MergeTypeIcon /> : <AutoAwesomeIcon />}
  onClick={onStart}
  disabled={genStyles.length === 0 || (fusionMode && genStyles.length < 2)}
  sx={{ borderRadius: 2, py: 1.2, fontWeight: 700, fontSize: '0.95rem' }}
>
  {fusionMode
    ? `融合生成（${genStyles.length} 种风格 → 1 条话术）`
    : `生成 ${genStyles.length} 条话术`}
</Button>
```

#### 4. 页面状态管理

**文件**: `ProductScriptManagePage.tsx`

```typescript
const [previewDialogOpen, setPreviewDialogOpen] = useState(false)
const [previewResults, setPreviewResults] = useState<StylePreview[]>([])
const [previewLoading, setPreviewLoading] = useState(false)

const handlePreview = useCallback(async () => {
  if (!productId || genStyles.length === 0) return

  setPreviewLoading(true)
  try {
    const result = await previewStyles({
      productId: Number(productId),
      styles: genStyles,
      scriptType,
      personaId: genPersonaId === '' ? undefined : genPersonaId,
      scene: genScene || undefined,
      useKbRef,
      kbCategories: selectedKbCategories,
    })

    setPreviewResults(result.previews)
    setPreviewDialogOpen(true)
  } catch (error) {
    toast('预览失败', 'error')
  } finally {
    setPreviewLoading(false)
  }
}, [productId, genStyles, scriptType, genPersonaId, genScene, useKbRef, selectedKbCategories, toast])

const handlePreviewConfirm = useCallback(() => {
  setPreviewDialogOpen(false)
  handleStart() // 触发正式生成
}, [handleStart])

const handlePreviewAdjust = useCallback(() => {
  setPreviewDialogOpen(false)
  // 返回配置面板，用户可以调整参数
}, [])
```

## 使用流程

1. **配置参数** - 用户选择风格、场景、人设等参数
2. **点击预览** - 点击"预览风格效果"按钮
3. **等待生成** - 系统快速生成各风格的15秒预览片段
4. **查看预览** - 弹出对话框展示所有风格的预览内容
5. **确认或调整**:
   - 满意：点击"满意，开始生成"，触发完整生成
   - 不满意：点击"调整配置"，返回修改参数

## 优势

1. **快速验证** - 15秒预览片段生成速度快，快速验证风格效果
2. **节省时间** - 避免生成不满意的完整话术，节省等待时间
3. **降低成本** - 减少无效的 AI 调用，降低 token 消耗
4. **提升体验** - 用户可以先试后买，提升使用体验
5. **灵活调整** - 预览后可以随时调整配置，直到满意为止

## 技术优势

1. **不保存数据库** - 预览结果不保存，减少数据库压力
2. **并发生成** - 多个风格并发生成，提升预览速度
3. **复用逻辑** - 复用现有的生成逻辑，减少代码重复
4. **轻量级** - 预览片段短，AI 调用成本低

## 后续优化

1. **缓存预览结果** - 相同配置的预览结果可以缓存，避免重复生成
2. **预览对比** - 支持多次预览结果的对比
3. **预览编辑** - 允许用户在预览基础上微调内容
4. **预览保存** - 满意的预览片段可以直接保存为话术
5. **预览评分** - 用户可以对预览结果评分，用于优化推荐算法

## 实现优先级

**优先级**: 中等

**原因**:
- 预览功能可以显著提升用户体验
- 但不是核心功能，不影响基本使用
- 可以在完成其他中期优化后实现

**建议实现顺序**:
1. 风格权重配置 ✅
2. 权重预设模板 ✅
3. 融合策略选择 ✅
4. **预览功能** ← 当前
5. 智能推荐优化

## 工作量估算

- 后端实现：2-3 小时
- 前端实现：3-4 小时
- 测试验证：1 小时
- 文档编写：1 小时

**总计**: 7-9 小时
