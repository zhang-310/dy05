import { useState } from 'react'
import { Alert, Box, Button, Card, CardContent, Chip, Divider, LinearProgress, MenuItem, Stack, TextField, Typography } from '@mui/material'
import CompareArrowsIcon from '@mui/icons-material/CompareArrows'
import RefreshIcon from '@mui/icons-material/Refresh'
import { alpha } from '@mui/material/styles'
import { useQuery } from '@tanstack/react-query'
import { liveApi, type LiveScriptVersion } from '@/api/live'
import { PageHeader } from '@/components/base/PageHeader'
import { getErrorMessage } from '@/utils/errorHandler'

function formatDate(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

function versionLabel(version: LiveScriptVersion) {
  return `${version.versionLabel || `V${version.versionNo}`} (${formatDate(version.createTime)})`
}

const HISTORY_COMPARE_READY_ENDPOINTS = {
  versionList: '/live/script/version/getByScriptId',
  diff: '/live/script/version/diff',
} as const

const HISTORY_COMPARE_CONTEXT_ENDPOINTS = [
  HISTORY_COMPARE_READY_ENDPOINTS.versionList,
  HISTORY_COMPARE_READY_ENDPOINTS.diff,
]

const HISTORY_COMPARE_UNSUPPORTED_ACTIONS = [
  'version-save',
  'version-activate',
  'version-delete',
  'version-rollback',
  'local-diff-fallback',
  'script-save',
  'product-version-compare',
  'shortvideo-export',
]

export default function HistoryComparePage() {
  const [scriptId, setScriptId] = useState('')
  const [loadedScriptId, setLoadedScriptId] = useState(0)
  const [v1, setV1] = useState<number>(0)
  const [v2, setV2] = useState<number>(0)

  const versionsQuery = useQuery({
    queryKey: ['script-versions', loadedScriptId],
    queryFn: () => liveApi.getVersionsByScriptId(loadedScriptId),
    enabled: loadedScriptId > 0,
  })
  const vList = Array.isArray(versionsQuery.data) ? versionsQuery.data : []

  const diffQuery = useQuery({
    queryKey: ['version-diff', v1, v2],
    queryFn: () => liveApi.versionDiff({ versionId1: v1, versionId2: v2 }),
    enabled: v1 > 0 && v2 > 0 && v1 !== v2,
  })

  const handleLoad = () => {
    const id = Number(scriptId)
    if (id > 0) {
      setLoadedScriptId(id)
      setV1(0)
      setV2(0)
    }
  }

  const diffData = diffQuery.data
  const leftContent = diffData?.oldContent ?? diffData?.leftContent ?? diffData?.versionA?.content
  const rightContent = diffData?.newContent ?? diffData?.rightContent ?? diffData?.versionB?.content
  const versionContext = `上下文：route=/admin/live/history-compare; scriptId=${loadedScriptId || Number(scriptId) || '空'}`
  const diffContext = `上下文：route=/admin/live/history-compare; scriptId=${loadedScriptId || '空'}; oldVersionId=${v1 || '空'}; newVersionId=${v2 || '空'}`

  return (
    <Box
      sx={{ py: 1 }}
      data-testid="live-history-compare-workbench"
      data-contract-scope="live-script-version-compare"
      data-ready-endpoints={Object.values(HISTORY_COMPARE_READY_ENDPOINTS).join('|')}
      data-context-endpoints={HISTORY_COMPARE_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={HISTORY_COMPARE_UNSUPPORTED_ACTIONS.join('|')}
      data-input-script-id={Number(scriptId) > 0 ? Number(scriptId) : 0}
      data-loaded-script-id={loadedScriptId}
      data-version-count={vList.length}
      data-old-version-id={v1}
      data-new-version-id={v2}
      data-has-diff={String(Boolean(diffData))}
    >
      <PageHeader
        title="直播话术历史对比"
        subtitle="版本列表和差异对比使用 `/live/script/version/getByScriptId` 与 `/diff` 真实契约。"
        breadcrumbs={[{ label: '直播' }, { label: '历史版本对比' }]}
        actions={loadedScriptId > 0 ? (
          <Button startIcon={<RefreshIcon />} variant="outlined" onClick={() => versionsQuery.refetch()} disabled={versionsQuery.isFetching}>
            刷新版本
          </Button>
        ) : undefined}
      />

      <Stack spacing={2}>
        <Alert
          severity="info"
          data-testid="live-history-contract-alert"
          data-contract-source={HISTORY_COMPARE_CONTEXT_ENDPOINTS.join('|')}
          data-no-version-save="true"
          data-no-version-activate="true"
          data-no-version-delete="true"
          data-no-version-rollback="true"
          data-no-local-diff-fallback="true"
          data-no-shortvideo-export="true"
        >
          后端按话术 ID 返回版本，`diff` 请求字段为 `oldVersionId/newVersionId`。页面会兼容旧组件使用的 `content/isActive` 字段，但展示以真实 VO 为准。
        </Alert>

        <Card
          variant="outlined"
          data-testid="live-history-control-card"
          data-contract-source={HISTORY_COMPARE_CONTEXT_ENDPOINTS.join('|')}
          data-version-list-source={HISTORY_COMPARE_READY_ENDPOINTS.versionList}
          data-diff-source={HISTORY_COMPARE_READY_ENDPOINTS.diff}
          data-only-script-id-filter="true"
        >
          <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
            <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems={{ xs: 'stretch', md: 'center' }} flexWrap="wrap">
              <TextField
                size="small"
                label="话术脚本 ID"
                value={scriptId}
                onChange={(e) => setScriptId(e.target.value)}
                sx={{ width: { xs: '100%', md: 180 } }}
                inputProps={{ inputMode: 'numeric' }}
              />
              <Button variant="contained" onClick={handleLoad} disabled={Number(scriptId) <= 0}>加载版本列表</Button>
              {vList.length > 0 && (
                <>
                  <TextField select size="small" label="旧版本" value={v1} onChange={(e) => setV1(Number(e.target.value))} sx={{ minWidth: 220 }}>
                    <MenuItem value={0}>选择旧版本</MenuItem>
                    {vList.map((v) => <MenuItem key={v.id} value={v.id}>{versionLabel(v)}</MenuItem>)}
                  </TextField>
                  <TextField select size="small" label="新版本" value={v2} onChange={(e) => setV2(Number(e.target.value))} sx={{ minWidth: 220 }}>
                    <MenuItem value={0}>选择新版本</MenuItem>
                    {vList.map((v) => <MenuItem key={v.id} value={v.id}>{versionLabel(v)}</MenuItem>)}
                  </TextField>
                </>
              )}
            </Stack>
          </CardContent>
        </Card>

        {(versionsQuery.isLoading || diffQuery.isFetching) && <LinearProgress sx={{ borderRadius: 1 }} />}

        {versionsQuery.isError && (
          <Alert
            severity="error"
            data-testid="live-history-version-list-error"
            data-contract-source={HISTORY_COMPARE_READY_ENDPOINTS.versionList}
            data-no-static-version-fallback="true"
            action={<Button color="inherit" size="small" onClick={() => versionsQuery.refetch()}>重试</Button>}
          >
            {HISTORY_COMPARE_READY_ENDPOINTS.versionList} 版本列表加载失败：{getErrorMessage(versionsQuery.error)}。{versionContext}
          </Alert>
        )}
        {diffQuery.isError && (
          <Alert
            severity="error"
            data-testid="live-history-diff-error"
            data-contract-source={HISTORY_COMPARE_READY_ENDPOINTS.diff}
            data-no-local-diff-fallback="true"
            data-selection-retained="true"
            action={<Button color="inherit" size="small" onClick={() => diffQuery.refetch()}>重试</Button>}
          >
            {HISTORY_COMPARE_READY_ENDPOINTS.diff} 版本对比失败：{getErrorMessage(diffQuery.error)}。{diffContext}。失败会保留两个版本选择，不使用本地 diff 伪造结果。
          </Alert>
        )}

        {v1 > 0 && v2 > 0 && v1 === v2 && (
          <Alert
            severity="warning"
            data-testid="live-history-same-version-warning"
            data-no-local-diff-fallback="true"
          >
            请选择不同的两个版本进行对比。
          </Alert>
        )}

        {loadedScriptId > 0 && !versionsQuery.isLoading && !versionsQuery.isError && vList.length === 0 && (
          <Alert
            severity="info"
            data-testid="live-history-empty-versions"
            data-contract-source={HISTORY_COMPARE_READY_ENDPOINTS.versionList}
            data-no-static-version-fallback="true"
          >
            该话术暂无历史版本。请先在直播工作台保存新版本。
          </Alert>
        )}

        {vList.length > 0 && !diffData && (
          <Card
            variant="outlined"
            data-testid="live-history-version-list-card"
            data-contract-source={HISTORY_COMPARE_READY_ENDPOINTS.versionList}
            data-version-count={vList.length}
            data-no-version-mutation="true"
          >
            <CardContent>
              <Stack direction="row" alignItems="center" justifyContent="space-between" mb={1}>
                <Typography variant="subtitle2">版本历史（{vList.length} 个）</Typography>
                <Chip label={`话术 #${loadedScriptId}`} variant="outlined" size="small" />
              </Stack>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                {vList.map((v) => (
                  <Chip
                    key={v.id}
                    data-testid="live-history-version-chip"
                    data-version-id={v.id}
                    data-version-active={String(Boolean(v.isActive))}
                    label={versionLabel(v)}
                    size="small"
                    variant={v.isActive ? 'filled' : 'outlined'}
                    color={v.isActive ? 'primary' : 'default'}
                  />
                ))}
              </Stack>
            </CardContent>
          </Card>
        )}

        {diffData && (
          <Box
            data-testid="live-history-diff-result"
            data-contract-source={HISTORY_COMPARE_READY_ENDPOINTS.diff}
            data-old-version-id={v1}
            data-new-version-id={v2}
            data-no-local-diff-fallback="true"
          >
            <Stack direction="row" alignItems="center" spacing={1} mb={1}>
              <CompareArrowsIcon fontSize="small" color="primary" />
              <Typography variant="subtitle2">版本对比结果</Typography>
              {typeof diffData.similarity === 'number' && <Chip size="small" label={`相似度 ${diffData.similarity}%`} />}
              {typeof diffData.recommendNew === 'boolean' && <Chip size="small" color={diffData.recommendNew ? 'success' : 'default'} label={diffData.recommendNew ? '推荐新版本' : '保留原版本'} />}
            </Stack>

            {diffData.recommendation && (
              <Alert
                severity="info"
                sx={{ mb: 2 }}
                data-testid="live-history-diff-recommendation"
                data-contract-source={HISTORY_COMPARE_READY_ENDPOINTS.diff}
              >
                {diffData.recommendation}
              </Alert>
            )}

            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '1fr 1fr' }, gap: 2 }}>
              <Card
                variant="outlined"
                data-testid="live-history-left-version-card"
                data-version-id={diffData.oldVersionId ?? v1}
              >
                <CardContent>
                  <Typography variant="caption" color="text.secondary" display="block" mb={1}>
                    旧版本（ID: {diffData.oldVersionId ?? v1} / V{diffData.oldVersionNo ?? '-'})
                  </Typography>
                  <Divider sx={{ mb: 1 }} />
                  <Box component="pre" sx={{ fontSize: 12, whiteSpace: 'pre-wrap', wordBreak: 'break-word', m: 0, minHeight: 180 }}>
                    {leftContent || '无内容'}
                  </Box>
                </CardContent>
              </Card>

              <Card
                variant="outlined"
                data-testid="live-history-right-version-card"
                data-version-id={diffData.newVersionId ?? v2}
              >
                <CardContent>
                  <Typography variant="caption" color="text.secondary" display="block" mb={1}>
                    新版本（ID: {diffData.newVersionId ?? v2} / V{diffData.newVersionNo ?? '-'})
                  </Typography>
                  <Divider sx={{ mb: 1 }} />
                  <Box component="pre" sx={{ fontSize: 12, whiteSpace: 'pre-wrap', wordBreak: 'break-word', m: 0, minHeight: 180 }}>
                    {rightContent || '无内容'}
                  </Box>
                </CardContent>
              </Card>
            </Box>

            {(diffData.diffHtml || (diffData.changedFields && diffData.changedFields.length > 0)) && (
              <Card
                variant="outlined"
                sx={{ mt: 2 }}
                data-testid="live-history-diff-summary-card"
                data-contract-source={HISTORY_COMPARE_READY_ENDPOINTS.diff}
              >
                <CardContent>
                  <Typography variant="subtitle2" mb={1}>差异摘要</Typography>
                  {diffData.changedFields && diffData.changedFields.length > 0 && (
                    <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mb: 1 }}>
                      {diffData.changedFields.map((field) => <Chip key={field} size="small" label={field} />)}
                    </Stack>
                  )}
                  {diffData.diffHtml && (
                    <Box
                      data-testid="live-history-diff-summary-surface"
                      data-contract-source={HISTORY_COMPARE_READY_ENDPOINTS.diff}
                      data-no-local-diff-fallback="true"
                      sx={(theme) => ({
                        fontSize: 13,
                        bgcolor: theme.palette.mode === 'dark'
                          ? theme.palette.background.default
                          : alpha(theme.palette.common.black, 0.025),
                        border: `1px solid ${theme.palette.divider}`,
                        color: 'text.primary',
                        p: 2,
                        borderRadius: 1,
                        overflow: 'auto',
                        '& ins': {
                          bgcolor: alpha(theme.palette.success.main, theme.palette.mode === 'dark' ? 0.24 : 0.12),
                          color: 'inherit',
                          textDecoration: 'none',
                        },
                        '& del': {
                          bgcolor: alpha(theme.palette.error.main, theme.palette.mode === 'dark' ? 0.24 : 0.12),
                          color: 'inherit',
                        },
                      })}
                      dangerouslySetInnerHTML={{ __html: diffData.diffHtml }}
                    />
                  )}
                </CardContent>
              </Card>
            )}
          </Box>
        )}
      </Stack>
    </Box>
  )
}
