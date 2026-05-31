import { useState } from 'react';
import { Alert, Box, Button, Card, CardContent, Chip, Divider, Grid, Stack, Tab, Tabs, Typography } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import RefreshIcon from '@mui/icons-material/Refresh';
import { useQuery } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { PageHeader, ErrorAlert } from '@/components/base';
import { benchmarkAnalysisApi, benchmarkVideoApi } from '@/api/benchmark';
import { shortvideoRoutes } from '@/constants/shortvideoRoutes';

const BENCHMARK_ANALYSIS_READY_ENDPOINTS = [
  '/benchmark/video/get',
  '/benchmark/analysis/get-by-video',
].join('|');

const BENCHMARK_ANALYSIS_UNSUPPORTED_ENDPOINTS = [
  '/benchmark/video/mock',
  '/benchmark/video/local-detail',
  '/benchmark/analysis/mock',
  '/benchmark/analysis/static-detail',
  '/benchmark/analysis/local-keyframes',
].join('|');

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
  const navigate = useNavigate();
  const [tabValue, setTabValue] = useState(0);
  const parsedVideoId = Number(videoId);

  const {
    data: video,
    isFetching: isVideoFetching,
    isError: isVideoError,
    error: videoError,
    refetch: refetchVideo,
  } = useQuery({
    queryKey: ['benchmarkVideo', videoId],
    queryFn: () => benchmarkVideoApi.get(parsedVideoId),
    enabled: Number.isFinite(parsedVideoId) && parsedVideoId > 0,
  });

  const {
    data: analysis,
    isFetching: isAnalysisFetching,
    isError: isAnalysisError,
    error: analysisError,
    refetch: refetchAnalysis,
  } = useQuery({
    queryKey: ['benchmarkAnalysis', videoId],
    queryFn: () => benchmarkAnalysisApi.getByVideo(parsedVideoId),
    enabled: Number.isFinite(parsedVideoId) && parsedVideoId > 0,
  });

  const isLoading = isVideoFetching || isAnalysisFetching;
  const keyFrames = parseKeyFrames(analysis?.keyFramesJson);
  const extractionIssues = [
    analysis?.transcriptText,
    analysis?.ocrText,
    analysis?.apiDescription,
    analysis?.sceneDescription,
    analysis?.aiSummary,
  ].filter((text): text is string => Boolean(text && /\[(ASR|OCR|API|AI|场景).*(失败|异常)/.test(text)));
  const hasTextSource = Boolean(analysis?.transcriptText || analysis?.ocrText || analysis?.apiDescription || analysis?.mergedContent);
  const hasCreative = Boolean(analysis?.hookStrategy || analysis?.contentStructure || analysis?.emotionalCurve || analysis?.pacingAnalysis);
  const hasViral = Boolean(analysis?.viralFactors || analysis?.strengths || analysis?.weaknesses || analysis?.replicableElements);
  const analysisErrorMessage = analysisError instanceof Error ? analysisError.message : '分析结果加载失败，请检查 /benchmark/analysis/get-by-video。';
  const videoErrorMessage = videoError instanceof Error ? videoError.message : '视频信息加载失败，请检查 /benchmark/video/get。';

  if (!Number.isFinite(parsedVideoId) || parsedVideoId <= 0) {
    return (
      <Box
        data-testid="benchmark-analysis-detail-page"
        data-contract-scope="benchmark-analysis-detail-invalid-route"
        data-ready-endpoints={BENCHMARK_ANALYSIS_READY_ENDPOINTS}
        data-unsupported-endpoints={BENCHMARK_ANALYSIS_UNSUPPORTED_ENDPOINTS}
        data-no-local-detail-fallback="true"
        data-no-static-analysis-fallback="true"
        sx={{ p: 3 }}
      >
        <ErrorAlert title="视频 ID 无效" message="当前地址没有有效的视频 ID，无法读取对标分析结果。" severity="warning" />
      </Box>
    );
  }

  return (
    <Box
      data-testid="benchmark-analysis-detail-page"
      data-contract-scope="benchmark-video-analysis-readonly"
      data-ready-endpoints={BENCHMARK_ANALYSIS_READY_ENDPOINTS}
      data-unsupported-endpoints={BENCHMARK_ANALYSIS_UNSUPPORTED_ENDPOINTS}
      data-no-local-detail-fallback="true"
      data-no-static-video-fallback="true"
      data-no-static-analysis-fallback="true"
      data-video-error={isVideoError ? 'true' : 'false'}
      data-analysis-error={isAnalysisError ? 'true' : 'false'}
      sx={{ p: 3, display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="视频深度分析"
        subtitle={video?.title || ''}
        actions={
          <Stack direction="row" spacing={1}>
            <Button variant="outlined" startIcon={<ArrowBackIcon />} onClick={() => navigate(shortvideoRoutes.benchmarkVideos)}>
              返回视频库
            </Button>
            <Button
              variant="outlined"
              startIcon={<RefreshIcon />}
              onClick={() => {
                void refetchVideo();
                void refetchAnalysis();
              }}
              disabled={isLoading}
            >
              刷新
            </Button>
          </Stack>
        }
      />

      {isVideoError && (
        <Box data-testid="benchmark-analysis-video-error" data-no-local-detail-fallback="true">
          <ErrorAlert title="视频信息加载失败" message={videoErrorMessage} onRetry={() => void refetchVideo()} />
        </Box>
      )}
      {isAnalysisError && (
        <Box data-testid="benchmark-analysis-result-error" data-no-static-analysis-fallback="true">
          <ErrorAlert title="分析结果加载失败" message={analysisErrorMessage} onRetry={() => void refetchAnalysis()} />
        </Box>
      )}

      {!isAnalysisError && !analysis && !isAnalysisFetching && (
        <Alert data-testid="benchmark-analysis-empty" data-no-static-analysis-fallback="true" severity="warning" variant="outlined">
          暂无分析结果。请先在对标视频库提交分析任务，ASR/OCR/API/LLM 任一链路失败时后端会保留错误信息，页面不会伪造拆解内容。
        </Alert>
      )}

      {analysis && extractionIssues.length > 0 && (
        <Alert data-testid="benchmark-analysis-downgrade" data-no-static-analysis-fallback="true" severity="warning" variant="outlined">
          分析链路存在降级：{extractionIssues.join('；')}。这些内容来自真实后端结果，页面不会用 mock 补齐缺失维度。
        </Alert>
      )}

      {analysis && (
        <Grid
          data-testid="benchmark-analysis-summary-contract"
          data-contract-scope="benchmark-analysis-real-result-summary"
          data-ready-endpoints="/benchmark/analysis/get-by-video"
          data-no-static-analysis-fallback="true"
          data-keyframe-count={keyFrames.length}
          container
          spacing={2}
        >
          <Grid item xs={12} md={3}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="caption" color="text.secondary">文本来源</Typography>
                <Typography variant="h5">{hasTextSource ? '已提取' : '缺失'}</Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={3}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="caption" color="text.secondary">关键帧</Typography>
                <Typography variant="h5">{keyFrames.length}</Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={3}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="caption" color="text.secondary">创意结构</Typography>
                <Typography variant="h5">{hasCreative ? '可用' : '缺失'}</Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={3}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="caption" color="text.secondary">可复刻要素</Typography>
                <Typography variant="h5">{hasViral ? '已生成' : '缺失'}</Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* 视频基本信息 */}
      <Card
        data-testid="benchmark-analysis-video-contract"
        data-contract-scope="benchmark-video-real-detail"
        data-ready-endpoints="/benchmark/video/get"
        data-no-local-detail-fallback="true"
        sx={{ mb: 3 }}
      >
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
                <Chip label={`播放 ${video?.viewCount?.toLocaleString() ?? '-'}`} />
                <Chip label={`点赞 ${video?.likeCount?.toLocaleString() ?? '-'}`} />
                <Chip label={`评论 ${video?.commentCount?.toLocaleString() ?? '-'}`} />
                <Chip label={`分享 ${video?.shareCount?.toLocaleString() ?? '-'}`} />
                <Chip label={`收藏 ${video?.favoriteCount?.toLocaleString() ?? video?.collectCount?.toLocaleString() ?? '-'}`} />
              </Stack>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mb: 2 }}>
                <Chip size="small" color={video?.isQualified ? 'success' : 'default'} label={video?.isQualified ? '符合采集阈值' : '未标记为优质'} />
                <Chip size="small" label={`本地文件 ${video?.localVideoPath || video?.localPath ? '已生成' : '缺失'}`} />
                <Chip size="small" label={`BOS ${video?.bosVideoUrl || video?.bosUrl ? '已上传' : '未上传'}`} />
                <Chip size="small" label={`分析状态 ${getStatusLabel(video?.analysisStatus)}`} />
              </Stack>
              <Typography variant="body2" color="text.secondary">
                {video?.description}
              </Typography>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* 分析结果 */}
      {analysis && <Card
        data-testid="benchmark-analysis-result-contract"
        data-contract-scope="benchmark-analysis-readonly-tabs"
        data-ready-endpoints="/benchmark/analysis/get-by-video"
        data-no-static-analysis-fallback="true"
      >
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
                  {keyFrames.map((frame, idx) => (
                    <Grid item xs={6} md={3} key={`${frame.framePath}-${idx}`}>
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
      </Card>}
    </Box>
  );
}

function getStatusLabel(status?: string) {
  switch (status) {
    case 'pending': return '待分析';
    case 'processing': return '分析中';
    case 'completed': return '已完成';
    case 'failed': return '失败';
    default: return status || '-';
  }
}

interface ParsedKeyFrame {
  framePath: string;
  startTime: number;
}

function parseKeyFrames(raw?: string): ParsedKeyFrame[] {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed
      .map((item) => {
        if (!item || typeof item !== 'object') return null;
        const frame = item as Record<string, unknown>;
        const framePath = readFrameString(frame, ['framePath', 'frameUrl', 'url', 'imageUrl', 'path']);
        const startTime = readFrameNumber(frame, ['startTime', 'time', 'timestamp', 'second', 'seconds']);
        if (!framePath || startTime == null) return null;
        return { framePath, startTime };
      })
      .filter((item): item is ParsedKeyFrame => Boolean(item));
  } catch {
    return [];
  }
}

function readFrameString(record: Record<string, unknown>, keys: string[]): string {
  for (const key of keys) {
    const value = record[key];
    if (value != null && String(value).trim()) return String(value);
  }
  return '';
}

function readFrameNumber(record: Record<string, unknown>, keys: string[]): number | null {
  for (const key of keys) {
    if (record[key] == null) continue;
    const value = Number(record[key]);
    if (Number.isFinite(value)) return value;
  }
  return null;
}
