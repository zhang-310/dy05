import { Typography, Card, Paper } from '@mui/material'
import { useTranslation } from 'react-i18next'
import { useMemo } from 'react'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid } from '@/components/base'
import type { ProductGmvRowVO, ProductGmvSummaryVO, ProfitMatrixPreviewVO, ProfitMatrixRowVO } from '@/types/dashboard'
import { formatCurrencyYuan, parseMoney } from '@/types/dashboard'

export interface KpiDataGridProps {
  loading: boolean
  productGmv: ProductGmvSummaryVO | null
  profitMatrix: ProfitMatrixPreviewVO | null
}

export function KpiDataGrid({ loading, productGmv, profitMatrix }: KpiDataGridProps) {
  const { t } = useTranslation()

  const productGmvColumns = useMemo<GridColDef<ProductGmvRowVO>[]>(
    () => [
      {
        field: 'productId',
        headerName: t('kpi.colProductId'),
        width: 110,
        valueFormatter: (v) => (v != null && v !== '' ? String(v) : '-'),
      },
      {
        field: 'productName',
        headerName: t('kpi.colProductName'),
        flex: 1,
        minWidth: 160,
        valueFormatter: (v) => (v ? String(v) : t('kpi.untitled')),
      },
      {
        field: 'sessionCount',
        headerName: t('kpi.colSessionCount'),
        width: 120,
        type: 'number',
        align: 'right',
        headerAlign: 'right',
        valueFormatter: (v) => (v != null ? Number(v).toLocaleString() : '0'),
      },
      {
        field: 'totalGmv',
        headerName: t('kpi.colGmv'),
        width: 130,
        align: 'right',
        headerAlign: 'right',
        sortable: false,
        valueGetter: (_v, row) => formatCurrencyYuan(parseMoney(row.totalGmv)),
      },
    ],
    [t],
  )

  const profitMatrixColumns = useMemo<GridColDef<ProfitMatrixRowVO>[]>(
    () => [
      {
        field: 'liveFormat',
        headerName: t('kpi.colLiveFormat', '直播形式'),
        flex: 1,
        minWidth: 120,
        valueFormatter: (v) => (v ? String(v) : '-'),
      },
      {
        field: 'sessionCount',
        headerName: t('kpi.colSessionCount', '场次数'),
        width: 100,
        type: 'number',
        align: 'right',
        headerAlign: 'right',
        valueFormatter: (v) => (v != null ? Number(v).toLocaleString() : '0'),
      },
      {
        field: 'totalGmv',
        headerName: t('kpi.colGmv', 'GMV'),
        width: 130,
        align: 'right',
        headerAlign: 'right',
        sortable: false,
        valueGetter: (_v, row) => formatCurrencyYuan(parseMoney(row.totalGmv)),
      },
      {
        field: 'estimatedMarginRate',
        headerName: t('kpi.colMargin', '毛利率'),
        width: 120,
        align: 'right',
        headerAlign: 'right',
        valueGetter: (_v, row) => {
          const rate = parseMoney(row.estimatedMarginRate)
          const suffix = row.isEstimated ? ' *' : ''
          return `${(rate * 100).toFixed(1)}%${suffix}`
        },
      },
      {
        field: 'note',
        headerName: t('kpi.colNote', '备注'),
        flex: 1,
        minWidth: 120,
        valueFormatter: (v) => (v ? String(v) : ''),
      },
    ],
    [t],
  )

  return (
    <>
      {/* Product GMV Grid */}
      <Card variant="outlined" sx={{ mb: 3, p: 2 }}>
        <Typography variant="subtitle2" fontWeight={600} sx={{ mb: 0.5 }}>
          {t('kpi.productGmvTitle')}
        </Typography>
        <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1.5 }}>
          {t('kpi.productGmvHint')}
          {productGmv?.since != null ? ` · ${t('kpi.dateFrom')}: ${productGmv.since}` : ''}
        </Typography>
        <Paper variant="outlined" sx={{ overflow: 'hidden' }}>
          <StandardDataGrid
            rows={productGmv?.rows ?? []}
            columns={productGmvColumns as GridColDef[]}
            loading={loading && !productGmv}
            getRowId={(r) => String(r.productId ?? `p-${r.productName ?? ''}`)}
            hideFooter
            autoHeight
            disableColumnMenu
            sx={{ minHeight: 200 }}
          />
        </Paper>
      </Card>

      {/* Profit Matrix Grid */}
      <Card variant="outlined" sx={{ mb: 3, p: 2 }}>
        <Typography variant="subtitle2" fontWeight={600} sx={{ mb: 0.5 }}>
          {t('kpi.profitMatrixTitle', '利润矩阵预览')}
        </Typography>
        <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1.5 }}>
          {t('kpi.profitMatrixHint', '按直播形式的 GMV × 估算毛利率')}
          {profitMatrix?.since != null ? ` · ${t('kpi.dateFrom')}: ${profitMatrix.since}` : ''}
        </Typography>
        <Paper variant="outlined" sx={{ overflow: 'hidden' }}>
          <StandardDataGrid
            rows={profitMatrix?.rows ?? []}
            columns={profitMatrixColumns as GridColDef[]}
            loading={loading && !profitMatrix}
            getRowId={(r) => String(r.liveFormat ?? `pm-${r.sessionCount ?? 0}`)}
            hideFooter
            autoHeight
            disableColumnMenu
            sx={{ minHeight: 160 }}
          />
        </Paper>
      </Card>
    </>
  )
}
