/**
 * ProductPage 常量与工具函数
 */
export const FEATURED_OPTIONS = [
  { value: '', label: '全部' },
  { value: 1, label: '精选' },
  { value: 0, label: '未精选' },
]

export const PRODUCT_TYPE_OPTIONS = [
  { value: '', label: '全部' },
  { value: 'loss', label: '亏品' },
  { value: 'control', label: '控单' },
  { value: 'profit', label: '利润品' },
  { value: 'hot', label: '爆品' },
  { value: 'normal', label: '平价' },
]

export const TYPE_LABELS: Record<string, { label: string; color: 'error' | 'warning' | 'success' | 'info' | 'default' }> = {
  loss: { label: '亏品', color: 'error' },
  control: { label: '控单', color: 'warning' },
  profit: { label: '利润品', color: 'success' },
  hot: { label: '爆品', color: 'info' },
  normal: { label: '平价', color: 'default' },
}

export function deriveProductType(row: Record<string, unknown>): string {
  const loss = row.lossPerUnit != null ? Number(row.lossPerUnit) : 0
  const profitPct = row.profitMarginPct != null ? Number(row.profitMarginPct) : 0
  const control = row.controlStrategy ? String(row.controlStrategy).trim() : ''
  const featured = row.featured != null ? Number(row.featured) : 0
  if (loss > 0) return 'loss'
  if (control) return 'control'
  if (profitPct >= 0.3) return 'profit'
  if (featured === 1) return 'hot'
  return 'normal'
}

export { formatMoney } from '@/utils/format'

export const LOCALE_TEXT = {
  MuiTablePagination: {
    labelRowsPerPage: '每页',
    labelDisplayedRows: ({ from, to, count }: { from: number; to: number; count: number }) =>
      `${from}-${to} / 共 ${count} 条`,
  },
  noRowsLabel: '暂无数据',
  toolbarColumns: '列',
  toolbarDensity: '行密度',
  toolbarDensityCompact: '紧凑',
  toolbarDensityStandard: '标准',
  toolbarDensityComfortable: '宽松',
  toolbarExport: '导出',
  toolbarExportCSV: '导出 CSV',
  toolbarExportPrint: '打印',
  columnMenuSortAsc: '升序',
  columnMenuSortDesc: '降序',
  columnMenuUnsort: '取消排序',
  columnMenuFilter: '筛选',
  columnMenuHideColumn: '隐藏列',
  columnMenuManageColumns: '管理列',
} as const
