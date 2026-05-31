import React, { useMemo } from 'react'
import {
  Box,
  Paper,
  Typography,
  CircularProgress,
  Alert,
  Chip,
  Divider,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Button,
} from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'
import { alpha, type Theme } from '@mui/material/styles'

/**
 * 版本差异数据结构
 */
interface VersionDiffData {
  added: string[]
  removed: string[]
  changed: string[]
}

/**
 * 组件 Props
 */
interface VersionDiffViewProps {
  scriptId: number
  versionA: number
  versionB: number
  contentA?: string
  contentB?: string
  onClose?: () => void
  loading?: boolean
  error?: string | null
}

/**
 * 行差异项
 */
interface DiffLine {
  type: 'added' | 'removed' | 'same'
  lineNumber: number
  content: string
}

function getLineBackgroundColor(theme: Theme, type: 'added' | 'removed' | 'same'): string {
  switch (type) {
    case 'added':
      return alpha(theme.palette.success.main, theme.palette.mode === 'dark' ? 0.22 : 0.14)
    case 'removed':
      return alpha(theme.palette.error.main, theme.palette.mode === 'dark' ? 0.22 : 0.14)
    case 'same':
      return 'transparent'
  }
}

function getOperatorColor(theme: Theme, type: 'added' | 'removed' | 'same'): string {
  switch (type) {
    case 'added':
      return theme.palette.success.main
    case 'removed':
      return theme.palette.error.main
    case 'same':
      return theme.palette.text.secondary
  }
}

/**
 * VersionDiffView 组件
 * 展示两个版本的差异对比
 *
 * 功能：
 * - 左右两列对比显示版本 A 和版本 B
 * - 新增行绿色标记，删除行红色标记
 * - 支持虚拟滚动处理大量内容
 * - 响应式设计
 *
 * @example
 * <VersionDiffView
 *   scriptId={123}
 *   versionA={1}
 *   versionB={2}
 *   contentA="原始内容"
 *   contentB="更新内容"
 * />
 */
export const VersionDiffView: React.FC<VersionDiffViewProps> = ({
  scriptId,
  versionA,
  versionB,
  contentA = '',
  contentB = '',
  onClose,
  loading = false,
  error = null,
}) => {
  // 计算差异
  const diffData = useMemo<VersionDiffData>(() => {
    if (!contentA || !contentB) {
      return { added: [], removed: [], changed: [] }
    }

    const linesA = contentA.split('\n')
    const linesB = contentB.split('\n')

    const setA = new Set(linesA)
    const setB = new Set(linesB)

    const added: string[] = []
    const removed: string[] = []

    // 计算新增行
    linesB.forEach((line) => {
      if (!setA.has(line) && line.trim()) {
        added.push(line)
      }
    })

    // 计算删除行
    linesA.forEach((line) => {
      if (!setB.has(line) && line.trim()) {
        removed.push(line)
      }
    })

    return { added, removed, changed: [] }
  }, [contentA, contentB])

  // 生成差异行列表（左列：A 版本）
  const leftLines = useMemo<DiffLine[]>(() => {
    const lines = contentA.split('\n')
    return lines.map((line, idx) => ({
      type: diffData.removed.includes(line) ? 'removed' : 'same',
      lineNumber: idx + 1,
      content: line,
    }))
  }, [contentA, diffData.removed])

  // 生成差异行列表（右列：B 版本）
  const rightLines = useMemo<DiffLine[]>(() => {
    const lines = contentB.split('\n')
    return lines.map((line, idx) => ({
      type: diffData.added.includes(line) ? 'added' : 'same',
      lineNumber: idx + 1,
      content: line,
    }))
  }, [contentB, diffData.added])

  const maxLines = Math.max(leftLines.length, rightLines.length)

  // 获取操作符
  const getOperator = (type: 'added' | 'removed' | 'same'): string => {
    switch (type) {
      case 'added':
        return '+'
      case 'removed':
        return '−'
      case 'same':
        return ' '
    }
  }

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    )
  }

  if (error) {
    return <Alert severity="error">{error}</Alert>
  }

  return (
    <Box data-testid="version-diff-view" data-diff-tone="semantic" sx={{ p: 2 }}>
      {/* 顶部标题和关闭按钮 */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Box>
          <Typography variant="h6">版本对比</Typography>
          <Typography variant="body2" color="textSecondary">
            脚本 #{scriptId} - 版本 {versionA} vs 版本 {versionB}
          </Typography>
        </Box>
        {onClose && (
          <Button
            startIcon={<CloseIcon />}
            onClick={onClose}
            variant="outlined"
            size="small"
          >
            关闭
          </Button>
        )}
      </Box>

      {/* 统计信息 */}
      <Box sx={{ display: 'flex', gap: 1, mb: 2, flexWrap: 'wrap' }}>
        <Chip
          label={`新增: ${diffData.added.length} 行`}
          color="success"
          variant="outlined"
          size="small"
        />
        <Chip
          label={`删除: ${diffData.removed.length} 行`}
          color="error"
          variant="outlined"
          size="small"
        />
        <Chip
          label={`共 ${maxLines} 行`}
          variant="outlined"
          size="small"
        />
      </Box>

      <Divider sx={{ my: 2 }} />

      {/* 差异表格 */}
      <TableContainer component={Paper} sx={{ maxHeight: 600, overflow: 'auto' }}>
        <Table stickyHeader size="small">
          <TableHead>
            <TableRow
              data-testid="version-diff-header-row"
              data-diff-tone="header"
              sx={(theme) => ({ backgroundColor: theme.palette.action.hover })}
            >
              <TableCell align="center" sx={{ width: '40px', fontWeight: 'bold' }}>
                行号
              </TableCell>
              <TableCell align="center" sx={{ width: '30px', fontWeight: 'bold' }}>
                操作
              </TableCell>
              <TableCell sx={{ flex: 1, fontWeight: 'bold' }}>
                版本 {versionA}
              </TableCell>
              <TableCell align="center" sx={{ width: '30px', fontWeight: 'bold' }}>
                操作
              </TableCell>
              <TableCell sx={{ flex: 1, fontWeight: 'bold' }}>
                版本 {versionB}
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {Array.from({ length: maxLines }).map((_, idx) => {
              const leftLine = leftLines[idx]
              const rightLine = rightLines[idx]

              return (
                <TableRow key={idx} sx={{ height: 32 }}>
                  {/* 左边行号 */}
                  <TableCell
                    data-diff-line-tone={leftLine?.type ?? 'empty'}
                    align="center"
                    sx={(theme) => ({
                      fontSize: '0.75rem',
                      backgroundColor: leftLine ? getLineBackgroundColor(theme, leftLine.type) : 'transparent',
                    })}
                  >
                    {leftLine ? leftLine.lineNumber : ''}
                  </TableCell>

                  {/* 左边操作符 */}
                  <TableCell
                    data-diff-line-tone={leftLine?.type ?? 'empty'}
                    align="center"
                    sx={(theme) => ({
                      fontSize: '0.8rem',
                      fontWeight: 'bold',
                      backgroundColor: leftLine ? getLineBackgroundColor(theme, leftLine.type) : 'transparent',
                      color: leftLine ? getOperatorColor(theme, leftLine.type) : theme.palette.text.secondary,
                    })}
                  >
                    {leftLine ? getOperator(leftLine.type) : ''}
                  </TableCell>

                  {/* 左边内容 */}
                  <TableCell
                    data-diff-line-tone={leftLine?.type ?? 'empty'}
                    sx={{
                      fontSize: '0.85rem',
                      fontFamily: 'monospace',
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-all',
                      backgroundColor: (theme) => leftLine ? getLineBackgroundColor(theme, leftLine.type) : 'transparent',
                    }}
                  >
                    {leftLine ? leftLine.content : ''}
                  </TableCell>

                  {/* 右边操作符 */}
                  <TableCell
                    data-diff-line-tone={rightLine?.type ?? 'empty'}
                    align="center"
                    sx={(theme) => ({
                      fontSize: '0.8rem',
                      fontWeight: 'bold',
                      backgroundColor: rightLine ? getLineBackgroundColor(theme, rightLine.type) : 'transparent',
                      color: rightLine ? getOperatorColor(theme, rightLine.type) : theme.palette.text.secondary,
                    })}
                  >
                    {rightLine ? getOperator(rightLine.type) : ''}
                  </TableCell>

                  {/* 右边内容 */}
                  <TableCell
                    data-diff-line-tone={rightLine?.type ?? 'empty'}
                    sx={{
                      fontSize: '0.85rem',
                      fontFamily: 'monospace',
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-all',
                      backgroundColor: (theme) => rightLine ? getLineBackgroundColor(theme, rightLine.type) : 'transparent',
                    }}
                  >
                    {rightLine ? rightLine.content : ''}
                  </TableCell>
                </TableRow>
              )
            })}
          </TableBody>
        </Table>
      </TableContainer>

      {/* 空状态：无差异行或两边内容均为空 */}
      {(maxLines === 0 || (!contentA.trim() && !contentB.trim())) && (
        <Alert severity="info" sx={{ mt: 2 }}>
          两个版本内容完全相同
        </Alert>
      )}
    </Box>
  )
}

export default VersionDiffView
