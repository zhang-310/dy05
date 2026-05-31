import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Grid,
  Chip,
  Divider,
  Paper,
  List,
  ListItem,
  ListItemText,
  Tab,
  Tabs,
  Alert,
  Stack,
} from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  Refresh as RefreshIcon,
  Star as StarIcon,
  Visibility as VisibilityIcon,
  ThumbUp as ThumbUpIcon,
} from '@mui/icons-material';
import { alpha } from '@mui/material/styles';
import { useQuery } from '@tanstack/react-query';
import { PageHeader, ErrorAlert } from '@/components/base';
import { benchmarkQualityScriptApi, benchmarkScriptSimilarityApi } from '@/api/benchmark';
import { shortvideoBenchmarkQualityScriptPath, shortvideoRoutes } from '@/constants/shortvideoRoutes';
import type { BenchmarkScriptSimilarityVO } from '@/types/benchmark';

const QUALITY_SCRIPT_DETAIL_ROUTE = '/admin/shortvideo/benchmark/quality-scripts/:id';
const QUALITY_SCRIPT_DETAIL_ENDPOINTS = {
  detail: '/benchmark/quality-script/get',
  similar: '/benchmark/script-similarity/find-similar',
} as const;
const QUALITY_SCRIPT_DETAIL_READY_ENDPOINTS = Object.values(QUALITY_SCRIPT_DETAIL_ENDPOINTS).join('|');
const QUALITY_SCRIPT_DETAIL_READY_ROUTES = [
  shortvideoRoutes.benchmarkQualityScripts,
  QUALITY_SCRIPT_DETAIL_ROUTE,
].join('|');
const QUALITY_SCRIPT_DETAIL_SUPPORTED_ACTIONS = [
  'refresh-quality-script-detail',
  'view-similar-quality-scripts',
  'navigate-back-to-quality-scripts',
].join('|');
const QUALITY_SCRIPT_DETAIL_UNSUPPORTED_ENDPOINTS = [
  '/benchmark/script-similarity/generate-embedding',
  '/benchmark/script-similarity/batch-generate-embeddings',
  '/benchmark/script-similarity/index-to-milvus',
  '/benchmark/script-similarity/batch-index-to-milvus',
  '/benchmark/script-similarity/find-similar-by-text',
  '/benchmark/quality-script/save',
  '/benchmark/quality-script/delete',
].join('|');
const QUALITY_SCRIPT_DETAIL_UNSUPPORTED_ACTIONS = [
  'local-similar-script-fallback',
  'auto-vector-generation',
  'auto-milvus-indexing',
  'detail-page-script-mutation',
  'local-before-after-recommendation',
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

function errorText(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback;
}

function detailContext(scriptId: number) {
  return `route=${QUALITY_SCRIPT_DETAIL_ROUTE}; endpoint=${QUALITY_SCRIPT_DETAIL_ENDPOINTS.detail}; scriptId=${scriptId}`;
}

function similarContext(scriptId: number, topK = 10, minScore = 0.7) {
  return `route=${QUALITY_SCRIPT_DETAIL_ROUTE}; endpoint=${QUALITY_SCRIPT_DETAIL_ENDPOINTS.similar}; scriptId=${scriptId}; topK=${topK}; minScore=${minScore}`;
}

export default function BenchmarkQualityScriptDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [tabValue, setTabValue] = useState(0);

  const scriptId = Number(id);

  const {
    data: script,
    isFetching: isScriptFetching,
    isError: isScriptError,
    error: scriptError,
    refetch: refetchScript,
  } = useQuery({
    queryKey: ['benchmarkQualityScript', scriptId],
    queryFn: () => benchmarkQualityScriptApi.get(scriptId),
    enabled: Number.isFinite(scriptId) && scriptId > 0,
  });

  const {
    data: similarScripts,
    isFetching: isSimilarFetching,
    isError: isSimilarError,
    error: similarError,
    refetch: refetchSimilar,
  } = useQuery({
    queryKey: ['similarScripts', scriptId],
    queryFn: () => benchmarkScriptSimilarityApi.findSimilar({ scriptId, topK: 10, minScore: 0.7 }),
    enabled: Number.isFinite(scriptId) && scriptId > 0,
  });

  const scriptErrorMessage = `${errorText(scriptError, '质量脚本详情加载失败')}（${detailContext(scriptId)}）`;
  const similarErrorMessage = `${errorText(similarError, '相似脚本加载失败，请检查向量嵌入和 Milvus 索引')}（${similarContext(scriptId)}）`;

  if (!Number.isFinite(scriptId) || scriptId <= 0) {
    return (
      <Box
        sx={{ p: 3 }}
        data-testid="benchmark-quality-script-detail-invalid-id"
        data-contract-scope="benchmark-quality-script-detail-readonly"
        data-no-local-similar-script-fallback="true"
      >
        <ErrorAlert title="脚本 ID 无效" message="当前地址没有有效的质量脚本 ID。" severity="warning" />
      </Box>
    );
  }

  return (
    <Box
      sx={{ p: 3, display: 'flex', flexDirection: 'column', gap: 2 }}
      data-testid="benchmark-quality-script-detail-workbench"
      data-contract-scope="benchmark-quality-script-detail-readonly"
      data-ready-endpoints={QUALITY_SCRIPT_DETAIL_READY_ENDPOINTS}
      data-ready-routes={QUALITY_SCRIPT_DETAIL_READY_ROUTES}
      data-supported-actions={QUALITY_SCRIPT_DETAIL_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={QUALITY_SCRIPT_DETAIL_UNSUPPORTED_ENDPOINTS}
      data-unsupported-actions={QUALITY_SCRIPT_DETAIL_UNSUPPORTED_ACTIONS}
      data-script-id={scriptId}
      data-detail-loaded={String(Boolean(script))}
      data-detail-error={String(isScriptError)}
      data-similar-error={String(isSimilarError)}
      data-similar-count={similarScripts?.length ?? 0}
      data-no-local-similar-script-fallback="true"
      data-no-auto-vector-generation="true"
      data-no-auto-milvus-indexing="true"
      data-no-detail-page-script-mutation="true"
    >
      <PageHeader
        title="质量脚本详情"
        subtitle={script ? `ID: ${script.id}` : '读取质量脚本详情'}
        actions={
          <Stack direction="row" spacing={1}>
            <Button variant="outlined" startIcon={<ArrowBackIcon />} onClick={() => navigate(shortvideoRoutes.benchmarkQualityScripts)} data-testid="benchmark-quality-script-detail-back-button" data-target-route={shortvideoRoutes.benchmarkQualityScripts}>
              返回列表
            </Button>
            <Button
              variant="outlined"
              startIcon={<RefreshIcon />}
              onClick={() => {
                void refetchScript();
                void refetchSimilar();
              }}
              disabled={isScriptFetching || isSimilarFetching}
              data-testid="benchmark-quality-script-detail-refresh-button"
              data-source-endpoints={QUALITY_SCRIPT_DETAIL_READY_ENDPOINTS}
            >
              刷新
            </Button>
          </Stack>
        }
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="benchmark-quality-script-detail-source-contract"
        data-contract-source={QUALITY_SCRIPT_DETAIL_READY_ENDPOINTS}
        data-unsupported-endpoints={QUALITY_SCRIPT_DETAIL_UNSUPPORTED_ENDPOINTS}
        data-no-auto-vector-generation="true"
        data-no-local-similar-script-fallback="true"
        data-supported-actions={QUALITY_SCRIPT_DETAIL_SUPPORTED_ACTIONS}
      >
        详情页只读取质量脚本详情和相似脚本检索结果；向量生成、批量索引、脚本保存和删除均不在详情页执行。
      </Alert>

      {isScriptError && (
        <Box
          data-testid="benchmark-quality-script-detail-error"
          data-contract-source={QUALITY_SCRIPT_DETAIL_ENDPOINTS.detail}
          data-script-id={scriptId}
          data-no-local-detail-fallback="true"
        >
          <ErrorAlert title="质量脚本详情加载失败" message={scriptErrorMessage} onRetry={() => void refetchScript()} />
        </Box>
      )}
      {isSimilarError && (
        <Box
          data-testid="benchmark-quality-script-similar-error"
          data-contract-source={QUALITY_SCRIPT_DETAIL_ENDPOINTS.similar}
          data-script-id={scriptId}
          data-no-local-similar-script-fallback="true"
          data-no-auto-vector-generation="true"
          data-no-auto-milvus-indexing="true"
        >
          <ErrorAlert title="相似脚本加载失败" message={similarErrorMessage} onRetry={() => void refetchSimilar()} severity="warning" />
        </Box>
      )}

      {!isScriptError && !script && !isScriptFetching && (
        <Alert
          severity="warning"
          variant="outlined"
          data-testid="benchmark-quality-script-detail-empty"
          data-contract-source={QUALITY_SCRIPT_DETAIL_ENDPOINTS.detail}
          data-no-local-detail-fallback="true"
        >
          未找到该质量脚本。它可能已被删除，或当前用户没有归属权限。
        </Alert>
      )}

      {script && (
      <Grid
        container
        spacing={3}
        data-testid="benchmark-quality-script-detail-contract"
        data-contract-source={QUALITY_SCRIPT_DETAIL_ENDPOINTS.detail}
        data-script-id={script.id}
        data-quality-score={script.qualityScore}
        data-has-embedding={String(Boolean(script.embeddingVector))}
        data-no-detail-page-script-mutation="true"
      >
        {/* 左侧：脚本详情 */}
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6" sx={{ flex: 1 }}>
                  脚本内容
                </Typography>
                <Chip
                  label={`质量评分: ${script.qualityScore.toFixed(1)}`}
                  color={script.qualityScore >= 80 ? 'success' : script.qualityScore >= 70 ? 'primary' : 'default'}
                  icon={<StarIcon />}
                />
              </Box>

              <Paper
                data-testid="benchmark-quality-script-content-surface"
                sx={(theme) => ({
                  p: 2,
                  mb: 3,
                  bgcolor: theme.palette.mode === 'dark' ? alpha(theme.palette.common.white, 0.04) : alpha(theme.palette.common.black, 0.025),
                  border: '1px solid',
                  borderColor: 'divider',
                })}
              >
                <Typography variant="body1" sx={{ whiteSpace: 'pre-wrap' }}>
                  {script.scriptContent}
                </Typography>
              </Paper>

              <Divider sx={{ my: 2 }} />

              <Grid container spacing={2}>
                <Grid item xs={6} md={3}>
                  <Typography variant="caption" color="text.secondary">
                    脚本类型
                  </Typography>
                  <Typography variant="body2">{script.scriptType || '-'}</Typography>
                </Grid>
                <Grid item xs={6} md={3}>
                  <Typography variant="caption" color="text.secondary">
                    行业
                  </Typography>
                  <Typography variant="body2">{script.industry || '-'}</Typography>
                </Grid>
                <Grid item xs={6} md={3}>
                  <Typography variant="caption" color="text.secondary">
                    场景类型
                  </Typography>
                  <Typography variant="body2">{script.sceneType || '-'}</Typography>
                </Grid>
                <Grid item xs={6} md={3}>
                  <Typography variant="caption" color="text.secondary">
                    视频时长
                  </Typography>
                  <Typography variant="body2">{script.videoDuration ? `${script.videoDuration}s` : '-'}</Typography>
                </Grid>
              </Grid>

              <Divider sx={{ my: 2 }} />

              <Typography variant="subtitle2" gutterBottom>
                互动数据
              </Typography>
              <Grid container spacing={2}>
                <Grid item xs={6} md={3}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <VisibilityIcon fontSize="small" color="action" />
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        播放量
                      </Typography>
                      <Typography variant="body2">{script.viewsCount?.toLocaleString() || '-'}</Typography>
                    </Box>
                  </Box>
                </Grid>
                <Grid item xs={6} md={3}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <ThumbUpIcon fontSize="small" color="action" />
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        点赞数
                      </Typography>
                      <Typography variant="body2">{script.likesCount?.toLocaleString() || '-'}</Typography>
                    </Box>
                  </Box>
                </Grid>
                <Grid item xs={6} md={3}>
                  <Typography variant="caption" color="text.secondary">
                    评论数
                  </Typography>
                  <Typography variant="body2">{script.commentsCount?.toLocaleString() || '-'}</Typography>
                </Grid>
                <Grid item xs={6} md={3}>
                  <Typography variant="caption" color="text.secondary">
                    分享数
                  </Typography>
                  <Typography variant="body2">{script.sharesCount?.toLocaleString() || '-'}</Typography>
                </Grid>
              </Grid>

              <Divider sx={{ my: 2 }} />

              <Typography variant="subtitle2" gutterBottom>
                质量指标
              </Typography>
              <Grid container spacing={2}>
                <Grid item xs={6} md={3}>
                  <Typography variant="caption" color="text.secondary">
                    互动率
                  </Typography>
                  <Typography variant="body2">{script.engagementRate ? `${script.engagementRate.toFixed(2)}%` : '-'}</Typography>
                </Grid>
                <Grid item xs={6} md={3}>
                  <Typography variant="caption" color="text.secondary">
                    传播力
                  </Typography>
                  <Typography variant="body2">{script.viralScore ? script.viralScore.toFixed(1) : '-'}</Typography>
                </Grid>
                <Grid item xs={6} md={3}>
                  <Typography variant="caption" color="text.secondary">
                    完成率
                  </Typography>
                  <Typography variant="body2">{script.completionRate ? `${script.completionRate.toFixed(2)}%` : '-'}</Typography>
                </Grid>
                <Grid item xs={6} md={3}>
                  <Typography variant="caption" color="text.secondary">
                    AI 评分
                  </Typography>
                  <Typography variant="body2">{script.aiRating ? script.aiRating.toFixed(1) : '-'}</Typography>
                </Grid>
              </Grid>

              {script.hookStrategy && (
                <>
                  <Divider sx={{ my: 2 }} />
                  <Typography variant="subtitle2" gutterBottom>
                    钩子策略
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {script.hookStrategy}
                  </Typography>
                </>
              )}

              {script.contentStructure && (
                <>
                  <Divider sx={{ my: 2 }} />
                  <Typography variant="subtitle2" gutterBottom>
                    内容结构
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {script.contentStructure}
                  </Typography>
                </>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* 右侧：相似脚本推荐 */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Tabs value={tabValue} onChange={(_, v) => setTabValue(v)}>
                <Tab label="相似脚本" />
                <Tab label="统计信息" />
              </Tabs>

              <TabPanel value={tabValue} index={0}>
                <Box
                  data-testid="benchmark-quality-script-similar-contract"
                  data-contract-source={QUALITY_SCRIPT_DETAIL_ENDPOINTS.similar}
                  data-script-id={scriptId}
                  data-top-k="10"
                  data-min-score="0.7"
                  data-similar-count={similarScripts?.length ?? 0}
                  data-no-local-similar-script-fallback="true"
                  data-no-auto-vector-generation="true"
                  data-no-auto-milvus-indexing="true"
                >
                {similarScripts && similarScripts.length > 0 ? (
                  <List>
                    {similarScripts.map((item: BenchmarkScriptSimilarityVO) => (
                      <ListItem
                        key={item.scriptId}
                        sx={{
                          border: 1,
                          borderColor: 'divider',
                          borderRadius: 1,
                          mb: 1,
                          cursor: 'pointer',
                          '&:hover': { bgcolor: 'action.hover' },
                        }}
                        onClick={() => navigate(shortvideoBenchmarkQualityScriptPath(item.scriptId))}
                        data-testid="benchmark-quality-script-similar-item"
                        data-script-id={item.scriptId}
                        data-similarity-score={item.similarityScore ?? ''}
                      >
                        <ListItemText
                          primary={
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                              <Typography variant="body2" noWrap sx={{ flex: 1 }}>
                                {item.scriptContent}
                              </Typography>
                              <Chip label={`${((item.similarityScore || 0) * 100).toFixed(0)}%`} size="small" color="primary" />
                            </Box>
                          }
                          secondary={
                            <Box sx={{ display: 'flex', gap: 1, mt: 0.5 }}>
                              <Chip label={`质量: ${item.qualityScore.toFixed(1)}`} size="small" />
                              {item.industry && <Chip label={item.industry} size="small" variant="outlined" />}
                            </Box>
                          }
                        />
                      </ListItem>
                    ))}
                  </List>
                ) : (
                  <Typography
                    variant="body2"
                    color="text.secondary"
                    align="center"
                    sx={{ py: 4 }}
                    data-testid="benchmark-quality-script-similar-empty"
                    data-no-local-similar-script-fallback="true"
                    data-no-auto-vector-generation="true"
                  >
                    {isSimilarFetching ? '正在检索相似脚本...' : '暂无相似脚本；请确认该脚本已生成向量并索引到 Milvus'}
                  </Typography>
                )}
                </Box>
              </TabPanel>

              <TabPanel value={tabValue} index={1}>
                <List>
                  <ListItem>
                    <ListItemText primary="引用次数" secondary={script.referenceCount || 0} />
                  </ListItem>
                  <ListItem>
                    <ListItemText
                      primary="最后引用时间"
                      secondary={script.lastReferencedAt || '从未引用'}
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText primary="创建时间" secondary={script.createTime} />
                  </ListItem>
                  <ListItem>
                    <ListItemText primary="更新时间" secondary={script.updateTime} />
                  </ListItem>
                </List>
              </TabPanel>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
      )}
    </Box>
  );
}
