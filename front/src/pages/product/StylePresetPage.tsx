import { useState } from 'react'
import {
  Alert, Box, Card, CardContent, Grid, Typography, Stack, Button, TextField, Chip,
  FormControl, InputLabel, Select, MenuItem,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import RefreshIcon from '@mui/icons-material/Refresh'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog, PageHeader, DataGridEmptyOverlay, ErrorAlert } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { productApi, type StylePreset } from '@/api/product'
import { getErrorMessage } from '@/utils/errorHandler'

const SCENES = ['护肤', '彩妆', '保健', '食品', '服饰', '家居', '其他']
const TONES = ['专业', '亲切', '活泼', '权威', '温暖']
const STYLE_PRESET_ENDPOINTS = {
  listAll: '/product/style-preset/list-all',
  listEnabled: '/product/style-preset/list',
  save: '/product/style-preset/save',
  delete: '/product/style-preset/delete',
  recommend: '/product/style-preset/recommend',
  products: '/product/search',
} as const
const STYLE_PRESET_READY_ENDPOINTS = [
  STYLE_PRESET_ENDPOINTS.listAll,
  STYLE_PRESET_ENDPOINTS.save,
  STYLE_PRESET_ENDPOINTS.delete,
] as const
const STYLE_PRESET_CONTEXT_ENDPOINTS = [
  STYLE_PRESET_ENDPOINTS.listEnabled,
  STYLE_PRESET_ENDPOINTS.products,
  STYLE_PRESET_ENDPOINTS.recommend,
] as const
const STYLE_PRESET_UNSUPPORTED_ACTIONS = [
  'management-list-from-enabled-list',
  'ml-style-training-feedback',
  'product-script-generation',
  'server-export',
] as const

export default function StylePresetPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20, scene: '' })
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<StylePreset>>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [recommendProductId, setRecommendProductId] = useState<number | ''>('')

  const { data = [], isFetching, isError, error, refetch } = useQuery({
    queryKey: ['style-presets'],
    queryFn: () => productApi.stylePresetList(),
  })

  const { data: products, isFetching: productsFetching, isError: productsIsError, error: productsError, refetch: refetchProducts } = useQuery({
    queryKey: ['style-preset-products'],
    queryFn: () => productApi.list({ page: 0, rows: 200 }),
  })

  const {
    data: recommendedCodes,
    isFetching: recommendFetching,
    isError: recommendIsError,
    error: recommendError,
    refetch: refetchRecommend,
  } = useQuery({
    queryKey: ['style-preset-recommend', recommendProductId],
    queryFn: () => productApi.stylePresetRecommend(Number(recommendProductId)),
    enabled: recommendProductId !== '',
  })

  const saveMut = useMutation({
    mutationFn: (params: Partial<StylePreset>) => productApi.stylePresetSave(params),
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); setForm({}); qc.invalidateQueries({ queryKey: ['style-presets'] }) },
    onError: (e: Error) => toast(getErrorMessage(e), 'error'),
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => productApi.stylePresetDelete(id),
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['style-presets'] }) },
    onError: (e: Error) => toast(getErrorMessage(e), 'error'),
  })

  const allRows = Array.isArray(data) ? data : []
  const rows = allRows.filter(r => !search.scene || r.category === search.scene)
  const productList = products?.list ?? []
  const selectedProduct = productList.find((product) => product.id === recommendProductId)
  const recommended = (recommendedCodes ?? []).map((code) => {
    const preset = allRows.find((row) => row.presetCode === code)
    return {
      code,
      preset,
      label: preset?.presetName ?? code,
      available: Boolean(preset),
      enabled: Boolean(preset?.isEnabled),
    }
  })
  const missingRecommendedCount = recommended.filter((item) => !item.available).length
  const disabledRecommendedCount = recommended.filter((item) => item.available && !item.enabled).length
  const total = rows.length
  const enabledCount = allRows.filter(r => r.isEnabled).length
  const disabledCount = allRows.length - enabledCount

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'presetName', headerName: '预设名称', flex: 1, minWidth: 140 },
    { field: 'presetCode', headerName: '预设代码', width: 150 },
    { field: 'category', headerName: '适用场景', width: 100,
      renderCell: ({ value }) => <Chip label={String(value ?? '-')} size="small" variant="outlined" /> },
    { field: 'styleValue', headerName: '语气风格', width: 90,
      renderCell: ({ value }) => <Chip label={String(value ?? '-')} size="small" /> },
    { field: 'description', headerName: '描述', flex: 1,
      renderCell: ({ value }) => (
        <Typography variant="body2" noWrap>{String(value ?? '')}</Typography>
      ) },
    { field: 'sortOrder', headerName: '排序', width: 80, type: 'number', valueFormatter: (v) => v ?? 0 },
    { field: 'isEnabled', headerName: '状态', width: 80,
      renderCell: ({ value }) => <Chip label={value ? '启用' : '停用'} color={value ? 'success' : 'default'} size="small" /> },
    { field: 'actions', headerName: '操作', width: 130, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5}>
          <Button size="small" onClick={() => { saveMut.reset(); setForm(row as StylePreset); setFormOpen(true) }}>编辑</Button>
          <Button size="small" color="error" onClick={() => setDeleteId((row as StylePreset).id)}>删除</Button>
        </Stack>
      ) },
  ]

  const searchSlot = (
    <FormControl size="small" sx={{ minWidth: 120 }}>
      <InputLabel id="style-preset-scene-filter-label">场景</InputLabel>
      <Select
        labelId="style-preset-scene-filter-label"
        id="style-preset-scene-filter"
        value={search.scene}
        label="场景"
        onChange={e => setSearch(s => ({ ...s, scene: e.target.value, page: 0 }))}
      >
        <MenuItem value="">全部</MenuItem>
        {SCENES.map(s => <MenuItem key={s} value={s}>{s}</MenuItem>)}
      </Select>
    </FormControl>
  )

  const actionSlot = (
    <Button
      variant="contained"
      startIcon={<AddIcon />}
      onClick={() => { saveMut.reset(); setForm({ isEnabled: true, sortOrder: 0 }); setFormOpen(true) }}
    >
      新建预设
    </Button>
  )

  const handleSave = () => {
    const presetName = String(form.presetName ?? '').trim()
    const presetCode = String(form.presetCode ?? '').trim()
    const styleValue = String(form.styleValue ?? '').trim()
    if (!presetName) { toast('请填写预设名称', 'warning'); return }
    if (!presetCode) { toast('请填写预设代码', 'warning'); return }
    if (!styleValue) { toast('请选择或填写语气风格', 'warning'); return }
    saveMut.mutate({
      ...form,
      presetName,
      presetCode,
      styleValue,
      category: String(form.category ?? '').trim() || undefined,
      description: String(form.description ?? '').trim() || undefined,
      isEnabled: form.isEnabled ?? true,
      sortOrder: Number(form.sortOrder ?? 0),
    })
  }

  return (
    <Box
      data-testid="style-preset-workbench"
      data-contract-scope="product-style-preset-admin"
      data-ready-endpoints={STYLE_PRESET_READY_ENDPOINTS.join('|')}
      data-context-endpoints={STYLE_PRESET_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={STYLE_PRESET_UNSUPPORTED_ACTIONS.join('|')}
      data-management-source={STYLE_PRESET_ENDPOINTS.listAll}
      data-scene-filter={search.scene || 'all'}
      data-row-count={rows.length}
      data-total-count={allRows.length}
      data-enabled-count={enabledCount}
      data-disabled-count={disabledCount}
      data-recommend-product-id={recommendProductId === '' ? '' : String(recommendProductId)}
      data-recommended-count={recommended.length}
      data-missing-recommended-count={missingRecommendedCount}
      data-disabled-recommended-count={disabledRecommendedCount}
      data-no-static-style-fallback="true"
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="话术风格预设"
        subtitle="管理商品话术生成使用的 style_preset；管理页读取全部预设，生成弹窗只读取启用预设。"
        breadcrumbs={[{ label: '商品' }, { label: '风格预设' }]}
        actions={actionSlot}
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="style-preset-contract-alert"
        data-management-source={STYLE_PRESET_ENDPOINTS.listAll}
        data-enabled-list-scope="generation-dialog-only"
        data-no-enabled-list-management-source="true"
        data-no-ml-training-request="true"
        data-no-product-script-generation="true"
        data-no-server-export-request="true"
      >
        真实接口：<code>{STYLE_PRESET_ENDPOINTS.listAll}</code>、<code>{STYLE_PRESET_ENDPOINTS.save}</code>、<code>{STYLE_PRESET_ENDPOINTS.delete}</code>。
        <code>{STYLE_PRESET_ENDPOINTS.listEnabled}</code> 仅返回启用项，不能用于管理停用预设。
      </Alert>

      <Grid container spacing={2}>
        {[
          ['预设总数', allRows.length],
          ['启用', enabledCount],
          ['停用', disabledCount],
          ['当前筛选', rows.length],
        ].map(([label, value]) => (
          <Grid item xs={6} md={3} key={String(label)}>
            <Card
              variant="outlined"
              data-testid="style-preset-kpi-card"
              data-contract-source={STYLE_PRESET_ENDPOINTS.listAll}
            >
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">{label}</Typography>
                <Typography variant="h6" fontWeight={700}>{value}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {isError && (
        <Box
          data-testid="style-preset-list-error"
          data-contract-source={STYLE_PRESET_ENDPOINTS.listAll}
          data-no-static-style-fallback="true"
        >
          <ErrorAlert
            title="风格预设加载失败"
            message={`${getErrorMessage(error)}。请检查 ${STYLE_PRESET_ENDPOINTS.listAll} 和登录态。`}
            onRetry={() => refetch()}
          />
        </Box>
      )}

      {saveMut.isError && (
        <Alert
          severity="error"
          data-testid="style-preset-save-error-page"
          data-contract-source={STYLE_PRESET_ENDPOINTS.save}
          data-dialog-input-preserved="true"
        >
          保存失败：{getErrorMessage(saveMut.error)}。请检查 {STYLE_PRESET_ENDPOINTS.save}；弹窗和输入已保留。
        </Alert>
      )}
      {deleteMut.isError && (
        <Alert
          severity="error"
          data-testid="style-preset-delete-error"
          data-contract-source={STYLE_PRESET_ENDPOINTS.delete}
          data-row-preserved="true"
        >
          删除失败：{getErrorMessage(deleteMut.error)}。请检查 {STYLE_PRESET_ENDPOINTS.delete}；预设行不会从本地移除。
        </Alert>
      )}

      <Card
        variant="outlined"
        data-testid="style-preset-recommend-card"
        data-contract-source={STYLE_PRESET_ENDPOINTS.recommend}
        data-product-source={STYLE_PRESET_ENDPOINTS.products}
        data-recommend-product-id={recommendProductId === '' ? '' : String(recommendProductId)}
        data-recommended-count={recommended.length}
        data-missing-recommended-count={missingRecommendedCount}
        data-disabled-recommended-count={disabledRecommendedCount}
        data-no-product-script-generation="true"
      >
        <CardContent>
          <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
            <Typography variant="subtitle2" fontWeight={700} sx={{ mr: 1 }}>
              商品风格推荐诊断
            </Typography>
            <FormControl size="small" sx={{ minWidth: 240 }}>
              <InputLabel id="style-preset-recommend-product-label">选择商品</InputLabel>
              <Select
                labelId="style-preset-recommend-product-label"
                id="style-preset-recommend-product"
                value={recommendProductId}
                label="选择商品"
                onChange={(event) => setRecommendProductId(event.target.value === '' ? '' : Number(event.target.value))}
                disabled={productsFetching}
              >
                <MenuItem value="">请选择商品</MenuItem>
                {productList.map((product) => (
                  <MenuItem key={product.id} value={product.id}>{product.productName || `商品 #${product.id}`}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <Button
              size="small"
              variant="outlined"
              startIcon={<RefreshIcon />}
              onClick={() => recommendProductId === '' ? refetchProducts() : refetchRecommend()}
              disabled={productsFetching || recommendFetching}
            >
              刷新推荐
            </Button>
          </Stack>

          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            真实接口：<code>{STYLE_PRESET_ENDPOINTS.recommend}</code> 返回风格编码，本区会映射到管理表中的预设名称、启用状态和缺失编码。
          </Typography>

          {productsIsError && (
            <Alert
              severity="warning"
              sx={{ mt: 1.5 }}
              data-testid="style-preset-products-error"
              data-contract-source={STYLE_PRESET_ENDPOINTS.products}
              data-no-local-product-fallback="true"
            >
              商品列表加载失败：{getErrorMessage(productsError)}。请检查 {STYLE_PRESET_ENDPOINTS.products}。
            </Alert>
          )}

          {recommendIsError && (
            <Alert
              severity="error"
              sx={{ mt: 1.5 }}
              data-testid="style-preset-recommend-error"
              data-contract-source={STYLE_PRESET_ENDPOINTS.recommend}
              data-input-preserved="true"
            >
              风格推荐加载失败：{getErrorMessage(recommendError)}。请检查 {STYLE_PRESET_ENDPOINTS.recommend} 和商品权限。
            </Alert>
          )}

          {recommendProductId === '' ? (
            <Alert severity="info" sx={{ mt: 1.5 }}>
              请选择商品后查看推荐风格；推荐结果用于检查规则推荐、预设编码和启用状态是否一致。
            </Alert>
          ) : (
            <Stack spacing={1.25} sx={{ mt: 1.5 }}>
              <Typography
                variant="caption"
                color="text.secondary"
                data-testid="style-preset-recommend-summary"
                data-contract-source={STYLE_PRESET_ENDPOINTS.recommend}
                data-missing-recommended-count={missingRecommendedCount}
                data-disabled-recommended-count={disabledRecommendedCount}
              >
                当前商品：{selectedProduct?.productName ?? `#${recommendProductId}`}；推荐 {recommended.length} 个编码，
                缺失 {missingRecommendedCount} 个，停用 {disabledRecommendedCount} 个。
              </Typography>
              {recommended.length > 0 ? (
                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                  {recommended.map((item) => (
                    <Chip
                      key={item.code}
                      label={`${item.label} / ${item.code}`}
                      color={!item.available ? 'warning' : item.enabled ? 'success' : 'default'}
                      variant={item.enabled ? 'filled' : 'outlined'}
                      size="small"
                      data-contract-source={STYLE_PRESET_ENDPOINTS.recommend}
                      data-recommend-code={item.code}
                      data-recommend-available={String(item.available)}
                      data-recommend-enabled={String(item.enabled)}
                    />
                  ))}
                </Stack>
              ) : (
                <Alert
                  severity="warning"
                  data-testid="style-preset-recommend-empty"
                  data-contract-source={STYLE_PRESET_ENDPOINTS.recommend}
                  data-no-static-recommend-fallback="true"
                >
                  后端未返回推荐编码，请检查商品分类、价格、AI 卖点和推荐规则。
                </Alert>
              )}
            </Stack>
          )}
        </CardContent>
      </Card>

      <StandardDataGrid
        rows={rows} columns={columns} rowCount={total} loading={isFetching}
        paginationMode="client"
        searchSlot={searchSlot}
        slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        showExport={false}
        sx={{ height: 500 }}
      />

      <FormDialog open={formOpen} title={form.id ? '编辑风格预设' : '新建风格预设'}
        onClose={() => setFormOpen(false)} onConfirm={handleSave} loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {saveMut.isError && (
            <Alert
              severity="error"
              data-testid="style-preset-save-error-dialog"
              data-contract-source={STYLE_PRESET_ENDPOINTS.save}
              data-input-preserved="true"
            >
              保存失败：{getErrorMessage(saveMut.error)}。请检查 {STYLE_PRESET_ENDPOINTS.save}，当前输入不会被清空。
            </Alert>
          )}
          <TextField label="预设名称" value={form.presetName ?? ''} onChange={e => setForm(f => ({ ...f, presetName: e.target.value }))} fullWidth />
          <TextField label="预设代码" value={form.presetCode ?? ''} onChange={e => setForm(f => ({ ...f, presetCode: e.target.value }))} fullWidth />
          <Stack direction="row" spacing={2}>
            <FormControl fullWidth size="small">
              <InputLabel id="style-preset-category-label">适用场景</InputLabel>
              <Select
                labelId="style-preset-category-label"
                id="style-preset-category"
                value={form.category ?? ''}
                label="适用场景"
                onChange={e => setForm(f => ({ ...f, category: e.target.value }))}
              >
                {SCENES.map(s => <MenuItem key={s} value={s}>{s}</MenuItem>)}
              </Select>
            </FormControl>
            <FormControl fullWidth size="small">
              <InputLabel id="style-preset-tone-label">语气风格</InputLabel>
              <Select
                labelId="style-preset-tone-label"
                id="style-preset-tone"
                value={form.styleValue ?? ''}
                label="语气风格"
                onChange={e => setForm(f => ({ ...f, styleValue: e.target.value }))}
              >
                {TONES.map(t => <MenuItem key={t} value={t}>{t}</MenuItem>)}
              </Select>
            </FormControl>
          </Stack>
          <TextField label="描述" value={form.description ?? ''} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} fullWidth multiline minRows={3} />
          <Stack direction="row" spacing={2}>
            <TextField label="排序" type="number" value={form.sortOrder ?? 0} onChange={e => setForm(f => ({ ...f, sortOrder: Number(e.target.value) }))} fullWidth size="small" />
            <FormControl fullWidth size="small">
              <InputLabel id="style-preset-status-label">状态</InputLabel>
              <Select
                labelId="style-preset-status-label"
                id="style-preset-status"
                value={form.isEnabled === false ? '0' : '1'}
                label="状态"
                onChange={e => setForm(f => ({ ...f, isEnabled: e.target.value === '1' }))}
              >
                <MenuItem value="1">启用</MenuItem>
                <MenuItem value="0">停用</MenuItem>
              </Select>
            </FormControl>
          </Stack>
        </Stack>
      </FormDialog>

      <ConfirmDialog
        open={deleteId !== null}
        content={`确定删除该风格预设？失败时将保留当前预设行，并显示 ${STYLE_PRESET_ENDPOINTS.delete} 的错误。`}
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId && deleteMut.mutate(deleteId)}
        loading={deleteMut.isPending}
      />
    </Box>
  )
}
