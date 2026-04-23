import { useState } from 'react';
import { Box, Card, CardContent, Chip, Divider, Grid, Stack, Tab, Tabs, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { PageHeader } from '@/components/base/PageHeader';
import { benchmarkAnalysisApi, benchmarkVideoApi } from '@/api/benchmark';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div role="tabpanel" hidden={value !== index} {...other}>
      {value === index && <Box sx={{ py: 3 }}>{children}</Box>}
    </div>
  );
}

export default function BenchmarkAnalysisDetailPage() {
  const { videoId } = useParams<{ videoId: string }>();
  const [tabValue, setTabValue] = useState(0);

  // 查询视频信息
  const { data: video } = useQuery({
    queryKey: ['benchmarkVideo', videoId],
    queryFn: () => benchmarkVideoApi.get(Number(videoId)),
    enabled: !!videoId,
  });

  // 查询分析结果
  const { data: analysis, isLoading } = useQuery({
    queryKey: ['benchmarkAnalysis', videoId],
    queryFn: () => benchmarkAnalysisApi.getByVideo(Number(videoId)),
    enabled: !!videoId,
  });

  if (isLoading) {
    return <Box sx={{ p: 3 }}>加载中...</Box>;
  }

  if (!analysis) {
    return <Box sx={{ p: 3 }}>暂无分析结果</Box>;
  }

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader
        title="视频深度分析"
        subtitle={video?.title || ''}
      />

      {/* 视频基本信息 */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={3}>
            <Grid item xs={12} md={4}>
              {video?.coverUrl && (
                <img
                  src={video.coverUrl}
                  alt=""
                  style={{ width: '100%', borderRadius: 8 }}
                />
              )}
            </Grid>
            <Grid item xs={12} md={8}>
              <Typography variant="h6" gutterBottom>
                {video?.title}
              </Typography>
              <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
                <Chip label={`点赞 ${video?.likeCount?.toLocaleString()}`} />
                <Chip label={`评论 ${video?.commentCount?.toLocaleString()}`} />
                <Chip label={`分享 ${video?.shareCount?.toLocaleString()}`} />
              </Stack>
              <Typography variant="body2" color="text.secondary">
                {video?.description}
              </Typography>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* 分析结果 */}
      <Card>
        <Tabs value={tabValue} onChange={(_, v) => setTabValue(v)}>
          <Tab label="文案内容" />
          <Tab label="场景分析" />
          <Tab label="创意分析" />
          <Tab label="爆款因素" />
          <Tab label="竞品对比" />
          <Tab label="AI深度分析" />
        </Tabs>

        <TabPanel value={tabValue} index={0}>
          <Stack spacing={3}>
            <Box>
              <Typography variant="subtitle1" gutterBottom>ASR 语音识别</Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {analysis.transcriptText || '暂无数据'}
              </Typography>
            </Box>
            <Divider />
            <Box>
              <Typography variant="subtitle1" gutterBottom>OCR 字幕识别</Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {analysis.ocrText || '暂无数据'}
              </Typography>
            </Box>
            <Divider />
            <Box>
              <Typography variant="subtitle1" gutterBottom>抖音API获取</Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {analysis.apiDescription || '暂无数据'}
              </Typography>
            </Box>
            <Divider />
            <Box>
              <Typography variant="subtitle1" gutterBottom>合并后文案</Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', fontWeight: 'bold' }}>
                {analysis.mergedContent || '暂无数据'}
              </Typography>
            </Box>
          </Stack>
        </TabPanel>

        <TabPanel value={tabValue} index={1}>
          <Stack spacing={3}>
            <Box>
              <Typography variant="subtitle1" gutterBottom>场景数量</Typography>
              <Typography variant="h4">{analysis.sceneCount || 0} 个场景</Typography>
            </Box>
            <Divider />
            <Box>
              <Typography variant="subtitle1" gutterBottom>场景描述</Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {analysis.sceneDescription || '暂无数据'}
              </Typography>
            </Box>
            <Divider />
            <Box>
              <Typography variant="subtitle1" gutterBottom>关键帧</Typography>
              {analysis.keyFramesJson ? (
                <Grid container spacing={2}>
                  {JSON.parse(analysis.keyFramesJson).map((frame: { framePath: string; startTime: number }, idx: number) => (
                    <Grid item xs={6} md={3} key={idx}>
                      <Box>
                        <img src={frame.framePath} alt={`关键帧 ${idx + 1}`} style={{ width: '100%', borderRadius: 4 }} />
                        <Typography variant="caption" display="block" textAlign="center">
                          {frame.startTime.toFixed(2)}s
                        </Typography>
                      </Box>
                    </Grid>
                  ))}
                </Grid>
              ) : (
                <Typography variant="body2">暂无数据</Typography>
              )}
            </Box>
          </Stack>
        </TabPanel>

        <TabPanel value={tabValue} index={2}>
          <Stack spacing={3}>
            <Box>
              <Typography variant="subtitle1" gutterBottom>创意类型</Typography>
              <Typography variant="body2">{analysis.creativeType || '暂无数据'}</Typography>
            </Box>
            <Divider />
            <Box>
              <Typography variant="subtitle1" gutterBottom>钩子策略</Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {analysis.hookStrategy || '暂无数据'}
              </Typography>
            </Box>
            <Divider />
            <Box>
              <Typography variant="subtitle1" gutterBottom>内容结构</Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {analysis.contentStructure || '暂无数据'}
              </Typography>
            </Box>
            <Divider />
            <Box>
              <Typography variant="subtitle1" gutterBottom>情绪曲线</Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {analysis.emotionalCurve || '暂无数据'}
              </Typography>
            </Box>
            <Divider />
            <Box>
              <Typography variant="subtitle1" gutterBottom>节奏分析</Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {analysis.pacingAnalysis || '暂无数据'}
              </Typography>
            </Box>
          </Stack>
        </TabPanel>

        <TabPanel value={tabValue} index={3}>
          <Stack spacing={3}>
            <Box>
              <Typography variant="subtitle1" gutterBottom sx={{ color: 'warning.main' }}>
                爆款因素
              </Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {analysis.viralFactors || '暂无数据'}
              </Typography>
            </Box>
            <Divider />
            <Box>
              <Typography variant="subtitle1" gutterBottom sx={{ color: 'success.main' }}>
                优势
              </Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {analysis.strengths || '暂无数据'}
              </Typography>
            </Box>
            <Divider />
            <Box>
              <Typography variant="subtitle1" gutterBottom sx={{ color: 'error.main' }}>
                弊端
              </Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {analysis.weaknesses || '暂无数据'}
              </Typography>
            </Box>
            <Divider />
            <Box>
              <Typography variant="subtitle1" gutterBottom sx={{ color: 'primary.main' }}>
                可复制要素
              </Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {analysis.replicableElements || '暂无数据'}
              </Typography>
            </Box>
          </Stack>
        </TabPanel>

        <TabPanel value={tabValue} index={4}>
          <Stack spacing={3}>
            <Box>
              <Typography variant="subtitle1" gutterBottom>竞品对比报告</Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {analysis.comparisonReport || '暂无数据'}
              </Typography>
            </Box>
            <Divider />
            <Box>
              <Typography variant="subtitle1" gutterBottom>差异化要点</Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {analysis.differentiationPoints || '暂无数据'}
              </Typography>
            </Box>
          </Stack>
        </TabPanel>

        <TabPanel value={tabValue} index={5}>
          <Stack spacing={3}>
            <Box>
              <Typography variant="subtitle1" gutterBottom>AI 综合分析</Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {analysis.aiSummary || '暂无数据'}
              </Typography>
            </Box>
            <Divider />
            <Box>
              <Typography variant="subtitle1" gutterBottom>话术拆解</Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {analysis.scriptBreakdown || '暂无数据'}
              </Typography>
            </Box>
            <Divider />
            <Box>
              <Typography variant="subtitle1" gutterBottom>改进建议</Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {analysis.improvementSuggestions || '暂无数据'}
              </Typography>
            </Box>
            <Divider />
            <Box>
              <Typography variant="subtitle1" gutterBottom>目标受众</Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {analysis.targetAudience || '暂无数据'}
              </Typography>
            </Box>
            <Divider />
            <Box>
              <Stack direction="row" spacing={3}>
                <Box>
                  <Typography variant="caption" color="text.secondary">AI 模型</Typography>
                  <Typography variant="body2">{analysis.aiModelUsed || '-'}</Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">Token 消耗</Typography>
                  <Typography variant="body2">{analysis.tokensUsed?.toLocaleString() || '-'}</Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">分析耗时</Typography>
                  <Typography variant="body2">
                    {analysis.analysisDurationMs ? `${(analysis.analysisDurationMs / 1000).toFixed(2)}s` : '-'}
                  </Typography>
                </Box>
              </Stack>
            </Box>
          </Stack>
        </TabPanel>
      </Card>
    </Box>
  );
}
