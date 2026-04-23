/**
 * W-07: EvolutionOpportunitiesPanel 组件
 * 进化机会展示面板（高效话术入库、低效淘汰、去重候选等）
 */

import {
  Box,
  Paper,
  Typography,
  Button,
  Card,
  CardContent,
  Chip,
  Grid,
  Collapse,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
} from '@mui/material'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import CancelIcon from '@mui/icons-material/Cancel'
import ErrorIcon from '@mui/icons-material/Error'
import React, { useState } from 'react'
import type { EvolutionOpportunityVO } from '@/types/evolution'
import { useToast } from '@/contexts/ToastContext'

interface EvolutionOpportunitiesPanelProps {
  opportunities: EvolutionOpportunityVO[]
  loading?: boolean
  onRefresh?: () => void
  onApply?: (id: number) => void
  onReject?: (id: number) => void
}

interface OpportunityCardProps {
  opportunity: EvolutionOpportunityVO
  onApply: (id: number, comment?: string) => void
  onReject: (id: number, reason?: string) => void
}

/**
 * 单个进化机会卡片
 */
function OpportunityCard({ opportunity, onApply, onReject }: OpportunityCardProps) {
  const [expanded, setExpanded] = useState(false)
  const [applyDialogOpen, setApplyDialogOpen] = useState(false)
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false)
  const [comment, setComment] = useState('')
  const [reason, setReason] = useState('')

  const typeIcons: Record<string, { icon: React.ReactNode; color: 'success' | 'warning' | 'error' | 'info' }> = {
    high_effectiveness: { icon: <CheckCircleIcon />, color: 'success' },
    low_effectiveness: { icon: <CancelIcon />, color: 'warning' },
    duplicate_candidate: { icon: <ErrorIcon />, color: 'warning' },
    quality_gap: { icon: <ErrorIcon />, color: 'error' },
    timeliness: { icon: <ErrorIcon />, color: 'warning' },
    cross_domain: { icon: <CheckCircleIcon />, color: 'info' },
  }

  const typeIcon = typeIcons[opportunity.type] || typeIcons.quality_gap
  const priorityColors: Record<string, 'error' | 'warning' | 'success'> = {
    high: 'error',
    medium: 'warning',
    low: 'success',
  }

  return (
    <>
      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'start', justifyContent: 'space-between' }}>
            <Box sx={{ flex: 1 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                <Box sx={{ color: 'primary.main' }}>{typeIcon.icon}</Box>
                <Typography variant="h6" sx={{ fontWeight: 600 }}>
                  {opportunity.typeLabel}
                </Typography>
                <Chip label={opportunity.priority.toUpperCase()} color={priorityColors[opportunity.priority]} size="small" />
                <Chip
                  label={opportunity.confidenceScore + '%'}
                  variant="outlined"
                  size="small"
                  sx={{ ml: 'auto' }}
                />
              </Box>

              <Typography variant="body2" color="textSecondary" sx={{ mb: 1 }}>
                {opportunity.description}
              </Typography>

              {opportunity.scriptContent && (
                <Box
                  sx={{
                    backgroundColor: '#f5f5f5',
                    p: 1,
                    borderRadius: 1,
                    mb: 1,
                    maxHeight: 80,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                  }}
                >
                  <Typography variant="caption" sx={{ fontFamily: 'monospace', whiteSpace: 'pre-wrap' }}>
                    {opportunity.scriptContent.substring(0, 200)}
                    {opportunity.scriptContent.length > 200 && '...'}
                  </Typography>
                </Box>
              )}

              <Box sx={{ display: 'flex', gap: 1, mt: 1 }}>
                <Typography variant="caption" color="textSecondary">
                  <strong>操作:</strong> {opportunity.actionRequired}
                </Typography>
              </Box>

              <Box sx={{ display: 'flex', gap: 1, mt: 1 }}>
                <Typography variant="caption" color="textSecondary">
                  <strong>预期效果:</strong> {opportunity.estimatedImpact}
                </Typography>
              </Box>
            </Box>

            <IconButton size="small" onClick={() => setExpanded(!expanded)} sx={{ ml: 1 }}>
              <ExpandMoreIcon
                sx={{
                  transform: expanded ? 'rotate(180deg)' : 'rotate(0deg)',
                  transition: 'transform 0.3s',
                }}
              />
            </IconButton>
          </Box>

          <Collapse in={expanded}>
            <Box sx={{ mt: 2, pt: 2, borderTop: '1px solid #eee' }}>
              {opportunity.relatedScripts && opportunity.relatedScripts.length > 0 && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="subtitle2" sx={{ fontWeight: 600, mb: 1 }}>
                    相关话术 ({opportunity.relatedScripts.length}):
                  </Typography>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                    {opportunity.relatedScripts.map((script, idx) => (
                      <Chip key={idx} label={script} size="small" variant="outlined" />
                    ))}
                  </Box>
                </Box>
              )}

              <Box sx={{ display: 'flex', gap: 1, justifyContent: 'flex-end', mt: 2 }}>
                {opportunity.status === 'pending' && (
                  <>
                    <Button
                      variant="contained"
                      color="success"
                      size="small"
                      onClick={() => setApplyDialogOpen(true)}
                    >
                      应用
                    </Button>
                    <Button
                      variant="outlined"
                      color="error"
                      size="small"
                      onClick={() => setRejectDialogOpen(true)}
                    >
                      拒绝
                    </Button>
                  </>
                )}
                {opportunity.status === 'completed' && (
                  <Chip label="已完成" color="success" size="small" />
                )}
                {opportunity.status === 'rejected' && (
                  <Chip label="已拒绝" color="error" size="small" />
                )}
              </Box>
            </Box>
          </Collapse>
        </CardContent>
      </Card>

      {/* 应用对话框 */}
      <Dialog open={applyDialogOpen} onClose={() => setApplyDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>应用进化机会</DialogTitle>
        <DialogContent sx={{ pt: 2 }}>
          <Typography variant="body2" color="textSecondary" sx={{ mb: 2 }}>
            确认应用此进化机会？
          </Typography>
          <TextField
            label="备注（可选）"
            multiline
            rows={3}
            fullWidth
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="输入应用原因或备注..."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setApplyDialogOpen(false)}>取消</Button>
          <Button
            variant="contained"
            color="success"
            onClick={() => {
              onApply(opportunity.id, comment)
              setApplyDialogOpen(false)
              setComment('')
            }}
          >
            应用
          </Button>
        </DialogActions>
      </Dialog>

      {/* 拒绝对话框 */}
      <Dialog open={rejectDialogOpen} onClose={() => setRejectDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>拒绝进化机会</DialogTitle>
        <DialogContent sx={{ pt: 2 }}>
          <Typography variant="body2" color="textSecondary" sx={{ mb: 2 }}>
            请说明拒绝理由：
          </Typography>
          <TextField
            label="拒绝理由"
            multiline
            rows={3}
            fullWidth
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="输入拒绝原因..."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectDialogOpen(false)}>取消</Button>
          <Button
            variant="contained"
            color="error"
            onClick={() => {
              onReject(opportunity.id, reason)
              setRejectDialogOpen(false)
              setReason('')
            }}
          >
            确认拒绝
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
}

/**
 * 进化机会面板
 */
export function EvolutionOpportunitiesPanel({
  opportunities,
  loading = false,
  onRefresh,
  onApply,
  onReject,
}: EvolutionOpportunitiesPanelProps) {
  const toast = useToast()

  const handleApply = (id: number, _comment?: string) => {
    if (onApply) {
      onApply(id)
    } else {
      toast('未配置 onApply：机会应用请接入 POST /ai/knowledge-evolution/auto-optimize 等业务接口', 'info')
    }
  }

  const handleReject = (id: number, _reason?: string) => {
    if (onReject) {
      onReject(id)
    } else {
      toast('未配置 onReject：旧版 /ai/evolution/opportunities 路径后端未实现', 'warning')
    }
  }

  const opportunitiesByType = opportunities.reduce(
    (acc, opp) => {
      if (!acc[opp.type]) acc[opp.type] = []
      acc[opp.type].push(opp)
      return acc
    },
    {} as Record<string, EvolutionOpportunityVO[]>,
  )

  return (
    <Paper sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h6" sx={{ fontWeight: 600 }}>
          进化机会 ({opportunities.length})
        </Typography>
        <Button variant="outlined" size="small" onClick={onRefresh} disabled={loading}>
          刷新
        </Button>
      </Box>

      {opportunities.length === 0 ? (
        <Typography color="textSecondary" sx={{ textAlign: 'center', py: 4 }}>
          暂无进化机会
        </Typography>
      ) : (
        <>
          {/* 统计摘要 */}
          <Grid container spacing={2} sx={{ mb: 3 }}>
            {Object.entries(opportunitiesByType).map(([type, items]) => (
              <Grid item xs={12} sm={6} md={4} key={type}>
                <Card sx={{ backgroundColor: '#fafafa' }}>
                  <CardContent>
                    <Typography variant="body2" color="textSecondary" sx={{ mb: 1 }}>
                      {items[0]?.typeLabel || type}
                    </Typography>
                    <Typography variant="h6" sx={{ fontWeight: 600 }}>
                      {items.length} 项
                    </Typography>
                    <Box sx={{ display: 'flex', gap: 1, mt: 1, flexWrap: 'wrap' }}>
                      <Chip
                        label={`高: ${items.filter((o) => o.priority === 'high').length}`}
                        size="small"
                        color="error"
                        variant="outlined"
                      />
                      <Chip
                        label={`中: ${items.filter((o) => o.priority === 'medium').length}`}
                        size="small"
                        color="warning"
                        variant="outlined"
                      />
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>

          {/* 机会列表 */}
          <Box>
            {opportunities.map((opp) => (
              <OpportunityCard
                key={opp.id}
                opportunity={opp}
                onApply={handleApply}
                onReject={handleReject}
              />
            ))}
          </Box>
        </>
      )}
    </Paper>
  )
}
