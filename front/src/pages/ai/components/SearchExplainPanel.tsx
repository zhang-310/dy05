import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Box,
  Chip,
  Typography,
} from '@mui/material'
import {
  ExpandMore as ExpandMoreIcon,
  BugReport as DebugIcon,
} from '@mui/icons-material'

interface SearchExplainPanelProps {
  explain: string | null | undefined
  score: number
}

/** 解析 explain 字符串中的数值片段 */
function parseExplain(explain: string) {
  const parts: { label: string; value: string; numericValue?: number }[] = []

  // 向量榜位=1 RRF分量=0.01639
  const vectorMatch = explain.match(/向量榜位=(\d+)\s*RRF分量=([\d.]+)/)
  if (vectorMatch) {
    parts.push({ label: '向量排名', value: `#${vectorMatch[1]}`, numericValue: parseInt(vectorMatch[1]) })
    parts.push({ label: '向量 RRF 分量', value: parseFloat(vectorMatch[2]).toFixed(5), numericValue: parseFloat(vectorMatch[2]) })
  }

  // BM25榜位=2 RRF分量=0.01613
  const bm25Match = explain.match(/BM25榜位=(\d+)\s*RRF分量=([\d.]+)/)
  if (bm25Match) {
    parts.push({ label: 'BM25 排名', value: `#${bm25Match[1]}`, numericValue: parseInt(bm25Match[1]) })
    parts.push({ label: 'BM25 RRF 分量', value: parseFloat(bm25Match[2]).toFixed(5), numericValue: parseFloat(bm25Match[2]) })
  }

  // 融合RRF=0.03252
  const rrfMatch = explain.match(/融合RRF=([\d.]+)/)
  if (rrfMatch) {
    parts.push({ label: '融合 RRF 分', value: parseFloat(rrfMatch[1]).toFixed(5), numericValue: parseFloat(rrfMatch[1]) })
  }

  // 通道=hybrid|vector|fulltext
  const channelMatch = explain.match(/通道=(\S+?)(?:;|$)/)
  if (channelMatch) {
    parts.push({ label: '检索通道', value: channelMatch[1] })
  }

  // 重排分=0.85432
  const rerankMatch = explain.match(/重排分=([\d.]+)/)
  if (rerankMatch) {
    parts.push({ label: '重排分数', value: parseFloat(rerankMatch[1]).toFixed(5), numericValue: parseFloat(rerankMatch[1]) })
  }

  // 个性化偏移=+0.06
  const personalizationMatch = explain.match(/个性化偏移=([+-]?[\d.]+)/)
  if (personalizationMatch) {
    parts.push({ label: '个性化偏移', value: personalizationMatch[1], numericValue: parseFloat(personalizationMatch[1]) })
  }

  return parts
}

/** 计算向量 vs BM25 占比用于条形图 */
function getWeightRatio(explain: string): { vector: number; keyword: number } | null {
  const vectorMatch = explain.match(/向量榜位=\d+\s*RRF分量=([\d.]+)/)
  const bm25Match = explain.match(/BM25榜位=\d+\s*RRF分量=([\d.]+)/)
  if (!vectorMatch && !bm25Match) return null

  const v = vectorMatch ? parseFloat(vectorMatch[1]) : 0
  const k = bm25Match ? parseFloat(bm25Match[1]) : 0
  const total = v + k
  if (total === 0) return null
  return { vector: (v / total) * 100, keyword: (k / total) * 100 }
}

function channelColor(channel: string): 'success' | 'info' | 'warning' | 'default' {
  if (channel === 'hybrid') return 'success'
  if (channel === 'vector') return 'info'
  if (channel === 'fulltext' || channel === 'keyword') return 'warning'
  if (channel === 'reranked') return 'success'
  return 'default'
}

export function SearchExplainPanel({ explain, score }: SearchExplainPanelProps) {
  if (!explain) return null

  const parts = parseExplain(explain)
  const ratio = getWeightRatio(explain)

  return (
    <Accordion
      disableGutters
      elevation={0}
      sx={{
        border: '1px solid',
        borderColor: 'divider',
        borderRadius: 1,
        '&:before': { display: 'none' },
        bgcolor: 'action.hover',
      }}
    >
      <AccordionSummary expandIcon={<ExpandMoreIcon />} sx={{ minHeight: 36, py: 0 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <DebugIcon fontSize="small" sx={{ color: 'text.secondary' }} />
          <Typography variant="caption" color="text.secondary">
            检索解释 (得分: {(score * 100).toFixed(1)}%)
          </Typography>
        </Box>
      </AccordionSummary>
      <AccordionDetails sx={{ pt: 0, pb: 1 }}>
        {/* 分值详情 */}
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mb: 1 }}>
          {parts.map((p, i) => (
            <Chip
              key={i}
              size="small"
              variant="outlined"
              label={`${p.label}: ${p.value}`}
              color={p.label === '检索通道' ? channelColor(p.value) : 'default'}
              sx={{ fontSize: '0.7rem', height: 22 }}
            />
          ))}
        </Box>

        {/* 向量 vs 关键词比例条 */}
        {ratio && (
          <Box sx={{ mt: 1 }}>
            <Typography variant="caption" color="text.secondary" sx={{ mb: 0.5, display: 'block' }}>
              向量 ({ratio.vector.toFixed(0)}%) vs 关键词 ({ratio.keyword.toFixed(0)}%)
            </Typography>
            <Box sx={{ display: 'flex', height: 8, borderRadius: 1, overflow: 'hidden' }}>
              <Box sx={{ width: `${ratio.vector}%`, bgcolor: 'info.main', transition: 'width 0.3s' }} />
              <Box sx={{ width: `${ratio.keyword}%`, bgcolor: 'warning.main', transition: 'width 0.3s' }} />
            </Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 0.25 }}>
              <Typography variant="caption" sx={{ color: 'info.main', fontSize: '0.65rem' }}>向量</Typography>
              <Typography variant="caption" sx={{ color: 'warning.main', fontSize: '0.65rem' }}>关键词</Typography>
            </Box>
          </Box>
        )}

        {/* 原始 explain 文本 */}
        <Typography
          variant="caption"
          sx={{
            display: 'block',
            mt: 1,
            p: 0.5,
            bgcolor: 'background.paper',
            borderRadius: 0.5,
            fontFamily: 'monospace',
            fontSize: '0.65rem',
            color: 'text.secondary',
            wordBreak: 'break-all',
          }}
        >
          {explain}
        </Typography>
      </AccordionDetails>
    </Accordion>
  )
}
