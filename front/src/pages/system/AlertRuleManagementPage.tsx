import { useEffect, useState, useCallback } from 'react';
import {
  Box, Button, Card, CardContent, Chip,
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Select, MenuItem, FormControl, InputLabel,
  IconButton, Tooltip
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import { Add as AddIcon, Delete as DeleteIcon, Edit as EditIcon } from '@mui/icons-material';
import { PageHeader } from '@/components/base/PageHeader';
import { useToast } from '@/contexts/ToastContext';
import request from '@/utils/request';

interface AlertRule {
  id: number;
  name: string;
  metric: string;
  operator: string;
  threshold: number;
  level: string;
  enabled: boolean;
  notifyWecom: boolean;
}

const METRIC_OPTIONS = [
  { value: 'viewer_loss_rate', label: '观众流失率' },
  { value: 'negative_ratio', label: '负面弹幕比例' },
  { value: 'conversion_rate', label: '转化率' },
  { value: 'gmv_drop_rate', label: 'GMV下降率' },
];

const OPERATOR_OPTIONS = [
  { value: '>', label: '大于' },
  { value: '<', label: '小于' },
  { value: '>=', label: '大于等于' },
  { value: '<=', label: '小于等于' },
];

interface CustomToolbarProps {
  onAdd: () => void
}

function buildToolbar(onAdd: () => void) {
  return function ToolbarWrapper() {
    return <CustomToolbar onAdd={onAdd} />
  }
}

function CustomToolbar(props: CustomToolbarProps) {
  const { onAdd } = props;
  return (
    <Box sx={{ p: 1, display: 'flex', justifyContent: 'flex-end' }}>
      <Button variant="contained" size="small" startIcon={<AddIcon />} onClick={onAdd}>新建规则</Button>
    </Box>
  );
}

export default function AlertRuleManagementPage() {
  const toast = useToast();
  const [rules, setRules] = useState<AlertRule[]>([]);
  const [loading, setLoading] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editRule, setEditRule] = useState<Partial<AlertRule>>({});

  const loadRules = useCallback(async () => {
    setLoading(true);
    try {
      const data = await request.post('/system/alert/rules/list', {});
      setRules(Array.isArray(data) ? data : []);
    } catch {
      toast('加载告警规则失败', 'error');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => { loadRules(); }, [loadRules]);

  const handleSave = async () => {
    try {
      await request.post('/system/alert/rules/save', editRule);
      toast('保存成功', 'success');
      setDialogOpen(false);
      loadRules();
    } catch {
      toast('保存失败', 'error');
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await request.post('/system/alert/rules/delete', { id });
      toast('删除成功', 'success');
      loadRules();
    } catch {
      toast('删除失败', 'error');
    }
  };

  const columns: GridColDef[] = [
    { field: 'name', headerName: '规则名称', flex: 1 },
    { field: 'metric', headerName: '监控指标', width: 140, renderCell: (p) => {
      const opt = METRIC_OPTIONS.find(o => o.value === p.value);
      return opt ? opt.label : String(p.value ?? '');
    }},
    { field: 'operator', headerName: '条件', width: 80 },
    { field: 'threshold', headerName: '阈值', width: 100, renderCell: (p) => `${(Number(p.value) * 100).toFixed(0)}%` },
    { field: 'level', headerName: '告警级别', width: 100, renderCell: (p) => (
      <Chip size="small" label={String(p.value ?? '')} color={p.value === 'critical' ? 'error' : p.value === 'warning' ? 'warning' : 'info'} />
    )},
    { field: 'enabled', headerName: '状态', width: 80, renderCell: (p) => (
      <Chip size="small" label={p.value ? '启用' : '禁用'} color={p.value ? 'success' : 'default'} />
    )},
    { field: 'actions', headerName: '操作', width: 120, sortable: false, renderCell: (p) => (
      <Box>
        <Tooltip title="编辑"><IconButton size="small" onClick={() => { setEditRule(p.row); setDialogOpen(true); }}><EditIcon fontSize="small" /></IconButton></Tooltip>
        <Tooltip title="删除"><IconButton size="small" onClick={() => handleDelete(p.row.id)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
      </Box>
    )},
  ];

  const handleOpenCreate = useCallback(() => {
    setEditRule({ enabled: true, operator: '>', level: 'warning', threshold: 0.3 });
    setDialogOpen(true);
  }, []);

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader title="告警规则管理" subtitle="配置直播实时告警规则" />
      <Card>
        <CardContent>
          <DataGrid rows={rules} columns={columns} loading={loading} autoHeight
            slots={{ toolbar: buildToolbar(handleOpenCreate) }}
            slotProps={undefined}
            initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
            pageSizeOptions={[10, 25]} getRowId={(row) => row.id ?? `${row.metric ?? 'metric'}-${row.name ?? 'rule'}`} />
        </CardContent>
      </Card>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editRule.id ? '编辑规则' : '新建规则'}</DialogTitle>
        <DialogContent>
          <TextField fullWidth label="规则名称" margin="normal" value={editRule.name || ''} onChange={e => setEditRule({...editRule, name: e.target.value})} />
          <FormControl fullWidth margin="normal">
            <InputLabel>监控指标</InputLabel>
            <Select value={editRule.metric || ''} label="监控指标" onChange={e => setEditRule({...editRule, metric: e.target.value})}>
              {METRIC_OPTIONS.map(o => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
            </Select>
          </FormControl>
          <FormControl fullWidth margin="normal">
            <InputLabel>条件</InputLabel>
            <Select value={editRule.operator || '>'} label="条件" onChange={e => setEditRule({...editRule, operator: e.target.value})}>
              {OPERATOR_OPTIONS.map(o => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
            </Select>
          </FormControl>
          <TextField fullWidth label="阈值 (0-1)" type="number" margin="normal" value={editRule.threshold ?? 0.3}
            onChange={e => setEditRule({...editRule, threshold: parseFloat(e.target.value)})}
            inputProps={{ step: 0.05, min: 0, max: 1 }} />
          <FormControl fullWidth margin="normal">
            <InputLabel>告警级别</InputLabel>
            <Select value={editRule.level || 'warning'} label="告警级别" onChange={e => setEditRule({...editRule, level: e.target.value})}>
              <MenuItem value="info">提示</MenuItem>
              <MenuItem value="warning">警告</MenuItem>
              <MenuItem value="critical">严重</MenuItem>
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>取消</Button>
          <Button variant="contained" onClick={handleSave}>保存</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
