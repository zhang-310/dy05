import { useState } from 'react'
import { Box, Stack, TextField, Button, Card, CardContent, Typography, Divider, LinearProgress, MenuItem, Chip } from '@mui/material'
import CompareArrowsIcon from '@mui/icons-material/CompareArrows'
import { liveApi, type LiveScriptVersion } from '@/api/live'
import { useQuery } from '@tanstack/react-query'
import { useToast } from '@/contexts/ToastContext'

export default function HistoryComparePage() {
  const toast = useToast()
  const [scriptId, setScriptId] = useState('')
  const [loadedScriptId, setLoadedScriptId] = useState(0)
  const [v1, setV1] = useState<number>(0)
  const [v2, setV2] = useState<number>(0)

  const { data: versions, isLoading: vLoading } = useQuery({
    queryKey: ['script-versions', loadedScriptId],
    queryFn: () => liveApi.getVersionsByScriptId(loadedScriptId),
    enabled: loadedScriptId > 0,
  })
  const vList: LiveScriptVersion[] = versions ?? []

  const { data: diffData, isFetching: diffLoading, error: diffError } = useQuery({
    queryKey: ['version-diff', v1, v2],
    queryFn: () => liveApi.versionDiff({ versionId1: v1, versionId2: v2 }),
    enabled: v1 > 0 && v2 > 0 && v1 !== v2,
  })

  // 显示错误提示
  if (diffError) {
    toast('版本对比失败', 'error')
  }

  const handleLoad = () => {
    const id = Number(scriptId)
    if (id > 0) { setLoadedScriptId(id); setV1(0); setV2(0) }
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, py: 1, height: 'calc(100vh - 48px - 32px)' }}>
      {/* 控制栏 */}
      <Card variant="outlined">
        <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
          <Stack direction="row" spacing={2} alignItems="center" flexWrap="wrap">
            <TextField size="small" label="话术脚本 ID" value={scriptId}
              onChange={e => setScriptId(e.target.value)} sx={{ width: 160 }} />
            <Button variant="outlined" size="small" onClick={handleLoad}>加载版本列表</Button>
            {vList.length > 0 && (
              <>
                <TextField select size="small" label="版本 A" value={v1}
                  onChange={e => setV1(Number(e.target.value))} sx={{ width: 180 }}>
                  <MenuItem value={0}>— 选择版本 —</MenuItem>
                  {vList.map(v => <MenuItem key={v.id} value={v.id}>{v.versionNo} ({new Date(v.createTime).toLocaleDateString()})</MenuItem>)}
                </TextField>
                <TextField select size="small" label="版本 B" value={v2}
                  onChange={e => setV2(Number(e.target.value))} sx={{ width: 180 }}>
                  <MenuItem value={0}>— 选择版本 —</MenuItem>
                  {vList.map(v => <MenuItem key={v.id} value={v.id}>{v.versionNo} ({new Date(v.createTime).toLocaleDateString()})</MenuItem>)}
                </TextField>
              </>
            )}
          </Stack>
        </CardContent>
      </Card>

      {vLoading && <LinearProgress />}

      {/* 版本列表 */}
      {vList.length > 0 && !diffData && (
        <Box>
          <Typography variant="subtitle2" mb={1}>版本历史（{vList.length} 个）</Typography>
          <Stack direction="row" spacing={1} flexWrap="wrap">
            {vList.map(v => (
              <Chip key={v.id} label={`${v.versionNo} - ${new Date(v.createTime).toLocaleDateString()}`}
                size="small" variant={v.status === 1 ? 'filled' : 'outlined'}
                color={v.status === 1 ? 'primary' : 'default'} />
            ))}
          </Stack>
        </Box>
      )}

      {diffLoading && <LinearProgress />}

      {/* Diff 展示 */}
      {diffData && (
        <Box sx={{ flex: 1, overflow: 'hidden' }}>
          <Stack direction="row" alignItems="center" spacing={1} mb={1}>
            <CompareArrowsIcon fontSize="small" color="primary" />
            <Typography variant="subtitle2">版本对比结果</Typography>
          </Stack>
          <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2, height: 'calc(100% - 40px)', overflow: 'auto' }}>
            {/* 左侧：版本 A */}
            <Card variant="outlined" sx={{ overflow: 'auto' }}>
              <CardContent>
                <Typography variant="caption" color="text.secondary" display="block" mb={1}>
                  版本 A（ID: {v1}）
                </Typography>
                <Divider sx={{ mb: 1 }} />
                {diffData.leftContent ? (
                  <Box component="pre" sx={{ fontSize: 12, whiteSpace: 'pre-wrap', wordBreak: 'break-word', m: 0 }}>
                    {diffData.leftContent}
                  </Box>
                ) : diffData.versionA ? (
                  <Box component="pre" sx={{ fontSize: 12, whiteSpace: 'pre-wrap', wordBreak: 'break-word', m: 0 }}>
                    {JSON.stringify(diffData.versionA, null, 2)}
                  </Box>
                ) : (
                  <Typography color="text.secondary">无内容</Typography>
                )}
              </CardContent>
            </Card>
            {/* 右侧：版本 B */}
            <Card variant="outlined" sx={{ overflow: 'auto' }}>
              <CardContent>
                <Typography variant="caption" color="text.secondary" display="block" mb={1}>
                  版本 B（ID: {v2}）
                </Typography>
                <Divider sx={{ mb: 1 }} />
                {diffData.rightContent ? (
                  <Box sx={{ fontSize: 12 }}>
                    {diffData.rightContent.split('\n').map((line, i) => {
                      const isAdded = line.startsWith('+')
                      const isRemoved = line.startsWith('-')
                      return (
                        <Box key={i} component="span" display="block"
                          sx={{ bgcolor: isAdded ? 'success.50' : isRemoved ? 'error.50' : 'transparent',
                            color: isAdded ? 'success.dark' : isRemoved ? 'error.dark' : 'text.primary',
                            fontFamily: 'monospace', fontSize: 12, whiteSpace: 'pre-wrap' }}>
                          {line}
                        </Box>
                      )
                    })}
                  </Box>
                ) : diffData.versionB ? (
                  <Box component="pre" sx={{ fontSize: 12, whiteSpace: 'pre-wrap', wordBreak: 'break-word', m: 0 }}>
                    {JSON.stringify(diffData.versionB, null, 2)}
                  </Box>
                ) : (
                  <Typography color="text.secondary">无内容</Typography>
                )}
              </CardContent>
            </Card>
          </Box>
        </Box>
      )}

      {v1 > 0 && v2 > 0 && v1 === v2 && (
        <Typography color="warning.main">请选择不同的两个版本进行对比</Typography>
      )}
    </Box>
  )
}
