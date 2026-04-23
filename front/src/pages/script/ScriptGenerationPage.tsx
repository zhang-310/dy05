import { useState } from 'react'
import {
  Box, Card, CardContent, TextField, Button, Typography,
  FormControl, InputLabel, Select, MenuItem, CircularProgress,
  Divider, Stack,
} from '@mui/material'
import { AutoAwesome as AiIcon, ContentCopy as CopyIcon } from '@mui/icons-material'
import { PageHeader } from '@/components/base'
import { scriptApi } from '@/api/script'
import { useToast } from '@/contexts/ToastContext'

export default function ScriptGenerationPage() {
  const toast = useToast()
  const [productName, setProductName] = useState('')
  const [price, setPrice] = useState('')
  const [features, setFeatures] = useState('')
  const [style, setStyle] = useState('professional')
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<Record<string, unknown> | null>(null)

  const handleGenerate = async () => {
    if (!productName || !features) { toast('请填写商品名称和核心卖点', 'warning'); return }
    setLoading(true)
    try {
      const res = await scriptApi.generate({
        productName,
        productPrice: parseFloat(price) || 0,
        keyFeatures: features,
        style,
      })
      setResult({
        script: res.content != null ? String(res.content) : undefined,
        title: res.title != null ? String(res.title) : undefined,
        sections: undefined,
      })
    } catch {
      toast('生成失败，请重试', 'error')
    } finally {
      setLoading(false)
    }
  }

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text).then(() => toast('已复制', 'success'))
  }

  return (
    <Box sx={{ bgcolor: 'var(--color-surface-dark)', minHeight: '100vh', p: 'var(--spacing-lg)' }}>
      <PageHeader
        title="话术生成"
        breadcrumbs={[{ label: '话术管理' }, { label: '话术生成' }]}
      />
      <Card sx={{ mb: 'var(--spacing-lg)', bgcolor: 'var(--color-surface)', borderRadius: 'var(--border-radius-xl)', border: '1px solid var(--color-surface-light)', boxShadow: 'var(--shadow-elevation-1)' }}>
        <CardContent sx={{ p: 'var(--spacing-lg)' }}>
          <Stack spacing={2}>
            <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
              <TextField
                label="商品名称" required
                value={productName} onChange={(e) => setProductName(e.target.value)}
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 'var(--border-radius-lg)',
                    bgcolor: 'var(--color-surface-dark)',
                    color: 'var(--color-text-primary)',
                    '& fieldset': { borderColor: 'var(--color-surface-light)' },
                    '&:hover fieldset': { borderColor: 'var(--color-primary)' }
                  }
                }}
              />
              <TextField
                label="价格"
                value={price} onChange={(e) => setPrice(e.target.value)}
                type="number"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 'var(--border-radius-lg)',
                    bgcolor: 'var(--color-surface-dark)',
                    color: 'var(--color-text-primary)',
                    '& fieldset': { borderColor: 'var(--color-surface-light)' },
                    '&:hover fieldset': { borderColor: 'var(--color-primary)' }
                  }
                }}
              />
            </Box>
            <TextField
              label="核心卖点（用逗号分隔）" required multiline rows={3}
              value={features} onChange={(e) => setFeatures(e.target.value)}
              sx={{
                '& .MuiOutlinedInput-root': {
                  borderRadius: 'var(--border-radius-lg)',
                  bgcolor: 'var(--color-surface-dark)',
                  color: 'var(--color-text-primary)',
                  '& fieldset': { borderColor: 'var(--color-surface-light)' },
                  '&:hover fieldset': { borderColor: 'var(--color-primary)' }
                }
              }}
            />
            <FormControl size="small" sx={{
              '& .MuiOutlinedInput-root': {
                borderRadius: 'var(--border-radius-lg)',
                bgcolor: 'var(--color-surface-dark)',
                color: 'var(--color-text-primary)',
                '& fieldset': { borderColor: 'var(--color-surface-light)' },
                '&:hover fieldset': { borderColor: 'var(--color-primary)' }
              },
              '& .MuiInputLabel-root': { color: 'var(--color-text-secondary)' }
            }}>
              <InputLabel>话术风格</InputLabel>
              <Select value={style} label="话术风格" onChange={(e) => setStyle(e.target.value)}>
                <MenuItem value="professional">专业严谨</MenuItem>
                <MenuItem value="casual">轻松亲切</MenuItem>
                <MenuItem value="urgent">紧迫促销</MenuItem>
                <MenuItem value="storytelling">故事叙述</MenuItem>
              </Select>
            </FormControl>
            <Button
              variant="contained"
              startIcon={loading ? <CircularProgress size={16} color="inherit" /> : <AiIcon />}
              onClick={handleGenerate}
              disabled={loading}
              sx={{
                alignSelf: 'flex-start',
                height: '44px',
                borderRadius: 'var(--border-radius-lg)',
                bgcolor: 'var(--color-primary)',
                color: '#000',
                fontWeight: 600,
                '&:hover': { bgcolor: 'var(--color-primary-dark)' }
              }}
            >
              {loading ? 'AI 生成中...' : 'AI 生成话术'}
            </Button>
          </Stack>
        </CardContent>
      </Card>

      {!!result && (
        <Card sx={{ bgcolor: 'var(--color-surface)', borderRadius: 'var(--border-radius-xl)', border: '1px solid var(--color-surface-light)', boxShadow: 'var(--shadow-elevation-1)' }}>
          <CardContent sx={{ p: 'var(--spacing-lg)' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 'var(--spacing-md)' }}>
              <Typography variant="h6" sx={{ fontWeight: 700, color: 'var(--color-text-primary)' }}>生成结果</Typography>
              <Button
                size="small" startIcon={<CopyIcon />}
                onClick={() => handleCopy(String(result.content ?? result.script ?? JSON.stringify(result)))}
                sx={{ color: 'var(--color-primary)' }}
              >
                复制
              </Button>
            </Box>
            <Divider sx={{ mb: 'var(--spacing-md)', borderColor: 'var(--color-surface-light)' }} />
            <Typography component="pre" sx={{
              whiteSpace: 'pre-wrap',
              fontFamily: 'monospace',
              lineHeight: 'var(--line-height-base)',
              color: 'var(--color-text-primary)',
              p: 'var(--spacing-md)',
              bgcolor: 'var(--color-surface-dark)',
              borderRadius: 'var(--border-radius-lg)',
              border: '1px solid var(--color-surface-light)'
            }}>
              {String(result.content ?? result.script ?? JSON.stringify(result, null, 2))}
            </Typography>
          </CardContent>
        </Card>
      )}
    </Box>
  )
}
