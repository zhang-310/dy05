import { useCallback, useEffect, useMemo, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Alert,
  Box,
  Button,
  Chip,
  Grid,
  Paper,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import RefreshIcon from '@mui/icons-material/Refresh'
import VideoLibraryIcon from '@mui/icons-material/VideoLibrary'
import type { GridColDef } from '@mui/x-data-grid'
import { ConfirmDialog, DataGridEmptyOverlay, ErrorAlert, PageHeader, StandardDataGrid } from '@/components/base'
import { exportProductToShortVideo } from '@/api/product'
import {
  getProductScriptVersionHistory,
  recommendProductScriptVersions,
  updateProductScriptVersionStatus,
  type ProductScriptVersion,
} from '@/api/product-script-version'
import { useToast } from '@/contexts/ToastContext'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'

const PRODUCT_SCRIPT_VERSION_READY_ENDPOINTS = [
  '/product/script-version/list-by-product',
  '/product/script-version/recommend',
  '/product/script-version/update-status',
  '/product/script/export-to-shortvideo',
] as const
const PRODUCT_SCRIPT_VERSION_CONTEXT_ENDPOINTS = [
  '/product/script-version/best',
  '/product/script-version/detail/{id}',
  '/product/script-version/save',
  '/product/script-version/increase-usage',
  '/product/script-version/ensure-optimization-version',
] as const
const PRODUCT_SCRIPT_VERSION_UNSUPPORTED_ACTIONS = [
  'product-version-rollback',
  'product-version-diff',
  'local-status-toggle',
  'local-shortvideo-project',
  'server-export',
] as const

function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

function formatScore(value: unknown) {
  const score = Number(value ?? 0)
  return Number.isFinite(score) && score > 0 ? `${score.toFixed(1)}分` : '-'
}

function formatPct(value: unknown) {
  const pct = Number(value ?? 0)
  return Number.isFinite(pct) && pct > 0 ? `${pct.toFixed(1)}%` : '-'
}

export default function ProductScriptVersionPage() {
  const { productId } = useParams<{ productId: string }>()
  const navigate = useNavigate()
  const toast = useToast()

  const [rows, setRows] = useState<ProductScriptVersion[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [exportError, setExportError] = useState('')
  const [statusTarget, setStatusTarget] = useState<ProductScriptVersion | null>(null)
  const [statusLoading, setStatusLoading] = useState(false)
  const [exportTarget, setExportTarget] = useState<ProductScriptVersion | null>(null)
  const [exportLoading, setExportLoading] = useState(false)
  const [recommendedVersions, setRecommendedVersions] = useState<Array<ProductScriptVersion & { recommendScore?: number }>>([])
  const [recommendLoading, setRecommendLoading] = useState(false)
  const [recommendError, setRecommendError] = useState('')
  const [versionsLoaded, setVersionsLoaded] = useState(false)

  const currentProductId = Number(productId)

  const load = useCallback(async () => {
    if (!productId) return
    setLoading(true)
    setError('')
    setActionError('')
    setExportError('')
    setVersionsLoaded(false)
    try {
      const res = await getProductScriptVersionHistory(Number(productId))
      setRows(Array.isArray(res) ? res : [])
    } catch (err) {
      setRows([])
      setError(getErrorMessage(err, '加载版本失败，请检查 /product/script-version/list-by-product。'))
    } finally {
      setLoading(false)
      setVersionsLoaded(true)
    }
  }, [productId])

  useEffect(() => {
    void load()
  }, [load])

  const loadRecommendations = useCallback(async () => {
    if (!productId) return
    setRecommendLoading(true)
    setRecommendError('')
    try {
      const res = await recommendProductScriptVersions(Number(productId), undefined, 3)
      const list = Array.isArray(res.versions) ? res.versions : []
      setRecommendedVersions(list.map(item => ({
        id: item.id,
        productId: Number(productId),
        versionNumber: item.versionNumber,
        content: rows.find(row => row.id === item.id)?.content ?? '',
        style: item.style,
        effectivenessScore: item.effectivenessScore,
        usageCount: item.usageCount,
        conversionRate: item.conversionRate,
        isActive: rows.find(row => row.id === item.id)?.isActive,
        isRecommended: rows.find(row => row.id === item.id)?.isRecommended,
        recommendScore: item.recommendScore,
      }) as ProductScriptVersion & { recommendScore?: number }))
    } catch (err) {
      setRecommendedVersions([])
      setRecommendError(getErrorMessage(err, '加载推荐版本失败，请检查 /product/script-version/recommend。'))
    } finally {
      setRecommendLoading(false)
    }
  }, [productId, rows])

  useEffect(() => {
    if (!loading && versionsLoaded && productId) void loadRecommendations()
  }, [loading, versionsLoaded, productId, loadRecommendations])

  const summary = useMemo(() => {
    const active = rows.filter(row => row.isActive).length
    const recommended = rows.filter(row => row.isRecommended).length
    const avgScore = rows.length > 0
      ? rows.reduce((sum, row) => sum + Number(row.effectivenessScore ?? 0), 0) / rows.length
      : 0
    const used = rows.reduce((sum, row) => sum + Number(row.usageCount ?? 0), 0)
    const lowScore = rows.filter(row => Number(row.effectivenessScore ?? 0) > 0 && Number(row.effectivenessScore ?? 0) < 60).length
    const unused = rows.filter(row => Number(row.usageCount ?? 0) === 0).length
    return { active, recommended, avgScore, used, lowScore, unused }
  }, [rows])

  const handleStatusConfirm = useCallback(async () => {
    if (!statusTarget) return
    setStatusLoading(true)
    setActionError('')
    try {
      await updateProductScriptVersionStatus(statusTarget.id, !statusTarget.isActive)
      toast(statusTarget.isActive ? '已停用版本' : '已启用版本', 'success')
      setStatusTarget(null)
      void load()
    } catch (err) {
      const message = getErrorMessage(err, '更新版本状态失败')
      setActionError(`/product/script-version/update-status：${message}`)
      setStatusTarget(null)
      toast(message, 'error')
    } finally {
      setStatusLoading(false)
    }
  }, [statusTarget, toast, load])

  const handleExport = useCallback(async () => {
    if (!productId || !exportTarget) return
    setExportLoading(true)
    setExportError('')
    try {
      const result = await exportProductToShortVideo({
        productId: Number(productId),
        versionId: exportTarget.id,
        style: exportTarget.style,
        duration: 60,
      })
      toast('已导出短视频项目', 'success')
      setExportTarget(null)
      navigate(`${shortvideoRoutes.workbench}?projectId=${result.projectId}`)
    } catch (err) {
      const message = getErrorMessage(err, '导出短视频项目失败')
      setExportError(`/product/script/export-to-shortvideo：${message}`)
      setExportTarget(null)
      toast(message, 'error')
    } finally {
      setExportLoading(false)
    }
  }, [productId, exportTarget, toast, navigate])

  const columns: GridColDef<ProductScriptVersion>[] = [
    { field: 'versionNumber', headerName: '版本号', width: 90 },
    {
      field: 'content',
      headerName: '话术内容',
      flex: 1,
      minWidth: 260,
      renderCell: (p) => (
        <Tooltip title={String(p.value || '')}>
          <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'block', maxWidth: '100%' }}>
            {String(p.value || '').slice(0, 100)}{String(p.value || '').length > 100 ? '...' : ''}
          </span>
        </Tooltip>
      ),
    },
    { field: 'style', headerName: '风格', width: 110, renderCell: (p) => <Chip size="small" label={String(p.value || '通用')} variant="outlined" /> },
    {
      field: 'isActive',
      headerName: '状态',
      width: 110,
      renderCell: (p) => (
        <Chip
          label={p.row.isActive ? '启用' : '停用'}
          size="small"
          color={p.row.isActive ? 'success' : 'default'}
        />
      ),
    },
    { field: 'effectivenessScore', headerName: '效果评分', width: 110, renderCell: (p) => formatScore(p.value) },
    { field: 'conversionRate', headerName: '转化率', width: 100, renderCell: (p) => formatPct(p.value) },
    { field: 'usageCount', headerName: '使用次数', width: 100, renderCell: (p) => Number(p.value ?? 0) },
    { field: 'createdAt', headerName: '创建时间', width: 170, valueGetter: (_value, row) => row.createdAt ?? row.createTime ?? '-' },
    {
      field: 'actions',
      headerName: '操作',
      width: 210,
      sortable: false,
      renderCell: (p) => (
        <Stack direction="row" spacing={0.75}>
          <Button size="small" variant="outlined" onClick={() => setStatusTarget(p.row)}>
            {p.row.isActive ? '停用' : '启用'}
          </Button>
          <Button size="small" variant="outlined" startIcon={<VideoLibraryIcon />} onClick={() => setExportTarget(p.row)}>
            导出
          </Button>
        </Stack>
      ),
    },
  ]

  return (
    <Box
      data-testid="product-script-version-workbench"
      data-contract-scope="product-script-version-management"
      data-product-id={productId ?? ''}
      data-ready-endpoints={PRODUCT_SCRIPT_VERSION_READY_ENDPOINTS.join('|')}
      data-context-endpoints={PRODUCT_SCRIPT_VERSION_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={PRODUCT_SCRIPT_VERSION_UNSUPPORTED_ACTIONS.join('|')}
      data-row-count={rows.length}
      data-active-count={summary.active}
      data-recommended-count={summary.recommended}
      data-recommend-result-count={recommendedVersions.length}
      data-low-score-count={summary.lowScore}
      data-unused-count={summary.unused}
      data-no-static-version-fallback="true"
      sx={{ p: 2, minHeight: 'calc(100vh - 48px)', display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="商品话术版本"
        subtitle={`商品 ID: ${productId ?? '-'} · 版本列表使用 /product/script-version/list-by-product，启停使用 /update-status。`}
        breadcrumbs={[{ label: '商品' }, { label: '话术版本' }]}
        actions={(
          <>
            <Button size="small" variant="outlined" startIcon={<ArrowBackIcon />} onClick={() => navigate(`/org/product/${productId}/scripts`)}>
              返回话术
            </Button>
            <Button size="small" variant="outlined" startIcon={<RefreshIcon />} onClick={() => void load()} disabled={loading}>
              刷新
            </Button>
            <Button size="small" variant="outlined" onClick={() => void loadRecommendations()} disabled={recommendLoading}>
              刷新推荐
            </Button>
          </>
        )}
      />

      <Grid container spacing={2}>
        {[
          { label: '版本总数', value: rows.length, hint: 'ProductScriptVersionVO' },
          { label: '启用版本', value: summary.active, hint: 'isActive=true' },
          { label: '推荐版本', value: summary.recommended, hint: 'isRecommended=true' },
          { label: '平均评分', value: summary.avgScore > 0 ? summary.avgScore.toFixed(1) : '-', hint: `使用 ${summary.used} 次` },
        ].map(item => (
          <Grid item xs={6} md={3} key={item.label}>
            <Paper
              variant="outlined"
              data-testid="product-script-version-kpi-card"
              data-contract-source="/product/script-version/list-by-product"
              sx={{ p: 1.5 }}
            >
              <Typography variant="caption" color="text.secondary">{item.label}</Typography>
              <Typography variant="h5" fontWeight={700}>{item.value}</Typography>
              <Typography variant="caption" color="text.secondary">{item.hint}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>

      <Alert
        severity="info"
        data-testid="product-script-version-contract-alert"
        data-list-source="/product/script-version/list-by-product"
        data-recommend-source="/product/script-version/recommend"
        data-status-source="/product/script-version/update-status"
        data-export-source="/product/script/export-to-shortvideo"
        data-no-product-version-rollback="true"
        data-no-product-version-diff="true"
        data-no-server-export-request="true"
        data-no-local-shortvideo-project="true"
      >
        后端暂无按 `productId + versionNumber` 回滚和版本 diff 接口，本页不再展示伪回滚；需要发布某个版本时使用启用状态或直接导出为短视频项目。
      </Alert>

      {error && (
        <Box data-testid="product-script-version-list-error" data-contract-source="/product/script-version/list-by-product" data-no-static-version-fallback="true">
          <ErrorAlert title="版本加载失败" message={error} onRetry={load} />
        </Box>
      )}
      {actionError && (
        <Box data-testid="product-script-version-status-error" data-contract-source="/product/script-version/update-status" data-no-local-status-toggle="true">
          <ErrorAlert severity="warning" title="版本状态更新失败" message={`${actionError}。失败时不会本地切换启用状态。`} onRetry={load} />
        </Box>
      )}
      {exportError && (
        <Box data-testid="product-script-version-export-error" data-contract-source="/product/script/export-to-shortvideo" data-no-local-shortvideo-project="true">
          <ErrorAlert severity="warning" title="短视频导出失败" message={`${exportError}。导出失败不会创建本地假项目或跳转工作台。`} />
        </Box>
      )}
      {!loading && !error && rows.length === 0 && (
        <Alert
          severity="warning"
          data-testid="product-script-version-empty"
          data-contract-source="/product/script-version/list-by-product"
          data-no-static-version-fallback="true"
        >
          当前商品没有 ProductScriptVersion 记录。可先在商品话术页生成或保存版本；短视频导出会退回到 dy_product_script 的激活话术。
        </Alert>
      )}

      <Paper
        variant="outlined"
        data-testid="product-script-version-recommend-card"
        data-contract-source="/product/script-version/recommend"
        data-result-count={recommendedVersions.length}
        data-list-row-count={rows.length}
        data-no-static-recommend-fallback="true"
        sx={{ p: 1.5 }}
      >
        <Stack direction="row" justifyContent="space-between" alignItems="center" spacing={1} flexWrap="wrap" useFlexGap>
          <Box>
            <Typography variant="subtitle2" fontWeight={700}>推荐版本诊断</Typography>
            <Typography variant="caption" color="text.secondary">
              真实接口：/product/script-version/recommend，按效果评分、使用次数和转化率返回推荐分数。
            </Typography>
          </Box>
          <Button size="small" variant="outlined" onClick={() => void loadRecommendations()} disabled={recommendLoading}>
            刷新推荐
          </Button>
        </Stack>
        {recommendError && (
          <Alert
            severity="error"
            sx={{ mt: 1.5 }}
            data-testid="product-script-version-recommend-error"
            data-contract-source="/product/script-version/recommend"
            data-list-preserved="true"
          >
            {recommendError}
          </Alert>
        )}
        {!recommendError && recommendedVersions.length === 0 && !recommendLoading && (
          <Alert
            severity="info"
            sx={{ mt: 1.5 }}
            data-testid="product-script-version-recommend-empty"
            data-contract-source="/product/script-version/recommend"
            data-no-static-recommend-fallback="true"
          >
            暂无推荐版本。请先生成话术版本并回流效果评分、使用次数或转化率。
          </Alert>
        )}
        {recommendedVersions.length > 0 && (
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mt: 1.5 }}>
            {recommendedVersions.map(item => (
              <Chip
                key={item.id}
                label={`V${item.versionNumber ?? item.id} ${item.style ?? '通用'} · 推荐分 ${Number(item.recommendScore ?? 0).toFixed(1)}`}
                color={item.isActive ? 'success' : 'default'}
                variant={item.isRecommended ? 'filled' : 'outlined'}
                size="small"
                data-contract-source="/product/script-version/recommend"
                data-version-id={item.id}
              />
            ))}
          </Stack>
        )}
      </Paper>

      <Box sx={{ flex: 1, minHeight: 460 }}>
        <StandardDataGrid
          rows={rows}
          columns={columns}
          loading={loading}
          rowHeight={60}
          getRowId={(row) => row.id}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
          showExport={false}
        />
      </Box>

      <ConfirmDialog
        open={statusTarget !== null}
        title={statusTarget?.isActive ? '停用话术版本' : '启用话术版本'}
        content={`确定${statusTarget?.isActive ? '停用' : '启用'}版本 V${statusTarget?.versionNumber ?? statusTarget?.id}？该操作会调用 /product/script-version/update-status，失败不会本地切换状态。`}
        onClose={() => setStatusTarget(null)}
        onConfirm={handleStatusConfirm}
        loading={statusLoading}
      />

      <ConfirmDialog
        open={exportTarget !== null}
        title="导出版本为短视频项目"
        content={`确定将商品 ${currentProductId} 的版本 V${exportTarget?.versionNumber ?? exportTarget?.id} 导出为短视频项目？该操作会调用 /product/script/export-to-shortvideo。`}
        onClose={() => setExportTarget(null)}
        onConfirm={handleExport}
        loading={exportLoading}
      />
    </Box>
  )
}
