import {
  Typography,
  Card,
  Button,
  Grid,
  TextField,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  Paper,
} from '@mui/material'
import DownloadIcon from '@mui/icons-material/Download'
import PreviewIcon from '@mui/icons-material/Visibility'
import { useTranslation } from 'react-i18next'
import type { GridColDef } from '@mui/x-data-grid'
import { useMemo } from 'react'
import { StandardDataGrid } from '@/components/base'
import type { CockpitSessionRowVO } from '@/types/dashboard'
import { formatCurrencyYuan, parseMoney } from '@/types/dashboard'

function sessionStatusLabel(status: number | undefined, t: (k: string) => string): string {
  if (status === 0) return t('kpi.statusPrepare')
  if (status === 1) return t('kpi.statusLive')
  if (status === 2) return t('kpi.statusEnded')
  return String(status ?? '-')
}

interface AccountOption {
  id?: number
  accountName?: string
  nickname?: string
}

export interface KpiFilterBarProps {
  dateFrom: string
  dateTo: string
  accountId: number | ''
  sessionStatus: number | ''
  hourBucket: string
  productCategory: string
  exporting: boolean
  previewLoading: boolean
  previewRows: CockpitSessionRowVO[]
  accountOptions: AccountOption[]
  onDateFromChange: (v: string) => void
  onDateToChange: (v: string) => void
  onAccountIdChange: (v: number | '') => void
  onSessionStatusChange: (v: number | '') => void
  onHourBucketChange: (v: string) => void
  onProductCategoryChange: (v: string) => void
  onPreview: () => void
  onExportCsv: () => void
}

export function KpiFilterBar({
  dateFrom,
  dateTo,
  accountId,
  sessionStatus,
  hourBucket,
  productCategory,
  exporting,
  previewLoading,
  previewRows,
  accountOptions,
  onDateFromChange,
  onDateToChange,
  onAccountIdChange,
  onSessionStatusChange,
  onHourBucketChange,
  onProductCategoryChange,
  onPreview,
  onExportCsv,
}: KpiFilterBarProps) {
  const { t } = useTranslation()

  const previewColumns = useMemo<GridColDef<CockpitSessionRowVO>[]>(
    () => [
      {
        field: 'sessionId',
        headerName: t('kpi.colSessionId'),
        width: 110,
        valueFormatter: (v) => (v != null && v !== '' ? String(v) : '-'),
      },
      {
        field: 'liveTitle',
        headerName: t('kpi.colTitle'),
        flex: 1,
        minWidth: 140,
        valueFormatter: (v) => (v ? String(v) : t('kpi.untitled')),
      },
      {
        field: 'status',
        headerName: t('kpi.colStatus'),
        width: 100,
        valueGetter: (_v, row) => sessionStatusLabel(row.status, t),
      },
      {
        field: 'startTime',
        headerName: t('kpi.colStart'),
        width: 160,
        valueFormatter: (v) => (v ? String(v) : '-'),
      },
      {
        field: 'endTime',
        headerName: t('kpi.colEnd'),
        width: 160,
        valueFormatter: (v) => (v ? String(v) : '-'),
      },
      {
        field: 'gmv',
        headerName: t('kpi.colGmv'),
        width: 120,
        align: 'right',
        headerAlign: 'right',
        sortable: false,
        valueGetter: (_v, row) => formatCurrencyYuan(parseMoney(row.gmv)),
      },
      {
        field: 'productLineCount',
        headerName: t('kpi.colLines'),
        width: 100,
        type: 'number',
        align: 'right',
        headerAlign: 'right',
        valueFormatter: (v) => (v != null ? Number(v).toLocaleString() : '0'),
      },
    ],
    [t],
  )

  return (
    <Card variant="outlined" sx={{ mb: 3, p: 2 }}>
      <Typography variant="subtitle2" fontWeight={600} sx={{ mb: 1.5 }}>
        {t('kpi.cockpitTitle')}
      </Typography>
      <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 2 }}>
        {t('kpi.cockpitHint')}
      </Typography>
      <Grid container spacing={2} alignItems="flex-end">
        <Grid item xs={12} sm={6} md={3}>
          <TextField
            label={t('kpi.dateFrom')}
            type="date"
            size="small"
            fullWidth
            InputLabelProps={{ shrink: true }}
            value={dateFrom}
            onChange={(e) => onDateFromChange(e.target.value)}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <TextField
            label={t('kpi.dateTo')}
            type="date"
            size="small"
            fullWidth
            InputLabelProps={{ shrink: true }}
            value={dateTo}
            onChange={(e) => onDateToChange(e.target.value)}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <FormControl size="small" fullWidth>
            <InputLabel>{t('kpi.account')}</InputLabel>
            <Select
              label={t('kpi.account')}
              value={accountId === '' ? '' : accountId}
              onChange={(e) => {
                const v = e.target.value
                onAccountIdChange(v === '' ? '' : Number(v))
              }}
            >
              <MenuItem value="">{t('kpi.accountAll')}</MenuItem>
              {accountOptions.map((a) => (
                <MenuItem key={a.id} value={a.id!}>
                  {a.accountName || `账号 ${a.id}`}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <FormControl size="small" fullWidth>
            <InputLabel>{t('kpi.status')}</InputLabel>
            <Select
              label={t('kpi.status')}
              value={sessionStatus === '' ? '' : sessionStatus}
              onChange={(e) => {
                const v = e.target.value
                onSessionStatusChange(v === '' ? '' : Number(v))
              }}
            >
              <MenuItem value="">{t('kpi.statusAll')}</MenuItem>
              <MenuItem value={0}>{t('kpi.statusPrepare')}</MenuItem>
              <MenuItem value={1}>{t('kpi.statusLive')}</MenuItem>
              <MenuItem value={2}>{t('kpi.statusEnded')}</MenuItem>
            </Select>
          </FormControl>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <FormControl size="small" fullWidth>
            <InputLabel>{t('kpi.hourBucket')}</InputLabel>
            <Select label={t('kpi.hourBucket')} value={hourBucket} onChange={(e) => onHourBucketChange(e.target.value)}>
              <MenuItem value="all">{t('kpi.hourAll')}</MenuItem>
              <MenuItem value="morning">{t('kpi.hourMorning')}</MenuItem>
              <MenuItem value="afternoon">{t('kpi.hourAfternoon')}</MenuItem>
              <MenuItem value="evening">{t('kpi.hourEvening')}</MenuItem>
              <MenuItem value="night">{t('kpi.hourNight')}</MenuItem>
            </Select>
          </FormControl>
        </Grid>
        <Grid item xs={12} sm={6} md={6}>
          <TextField
            label={t('kpi.category')}
            size="small"
            fullWidth
            placeholder={t('kpi.categoryPh')}
            value={productCategory}
            onChange={(e) => onProductCategoryChange(e.target.value)}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Button
            variant="outlined"
            startIcon={<PreviewIcon />}
            disabled={previewLoading}
            fullWidth
            onClick={onPreview}
          >
            {previewLoading ? t('kpi.previewLoading') : t('kpi.preview')}
          </Button>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Button
            variant="contained"
            startIcon={<DownloadIcon />}
            disabled={exporting}
            fullWidth
            onClick={onExportCsv}
          >
            {exporting ? t('kpi.exporting') : t('kpi.exportCsv')}
          </Button>
        </Grid>
      </Grid>

      <Paper variant="outlined" sx={{ mt: 2, overflow: 'hidden' }}>
        <StandardDataGrid
          rows={previewRows}
          columns={previewColumns}
          loading={previewLoading}
          getRowId={(r) => String(r.sessionId ?? `row-${r.startTime ?? ''}-${r.liveTitle ?? ''}`)}
          hideFooter
          autoHeight
          disableColumnMenu
          sx={{ minHeight: 200 }}
        />
      </Paper>
    </Card>
  )
}
