import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Alert,
  Grid,
  TextField,
  MenuItem,
  Chip,
  Paper,
  List,
  ListItem,
  ListItemText,
  Divider,
  Stack,
  Tab,
  Tabs,
} from '@mui/material';
import { alpha } from '@mui/material/styles';
import {
  Search as SearchIcon,
  AutoAwesome as AutoAwesomeIcon,
  TrendingUp as TrendingUpIcon,
  Schedule as ScheduleIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import { useSnackbar } from 'notistack';
import { PageHeader, ErrorAlert } from '@/components/base';
import { benchmarkScriptRecommendationApi } from '@/api/benchmark';
import { shortvideoBenchmarkQualityScriptPath } from '@/constants/shortvideoRoutes';
import type { BenchmarkScriptSimilarityVO, SmartRecommendVO } from '@/types/benchmark';

const RECOMMENDATION_ROUTE = '/admin/shortvideo/benchmark/recommendation';
const RECOMMENDATION_ENDPOINTS = {
  requirement: '/benchmark/script-recommendation/recommend-by-requirement',
  smart: '/benchmark/script-recommendation/smart-recommend',
  popular: '/benchmark/script-recommendation/get-popular-scripts',
  latest: '/benchmark/script-recommendation/get-latest-quality-scripts',
} as const;

const RECOMMENDATION_READY_ENDPOINTS = Object.values(RECOMMENDATION_ENDPOINTS).join('|');
const RECOMMENDATION_UNSUPPORTED_ENDPOINTS = [
  '/benchmark/script-recommendation/mock',
  '/benchmark/script-recommendation/local-list',
  '/benchmark/script-recommendation/static-scripts',
  '/benchmark/script-recommendation/local-vector-search',
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

function requirementContext(requirement: string, topK = 10) {
  return `route=${RECOMMENDATION_ROUTE}; endpoint=${RECOMMENDATION_ENDPOINTS.requirement}; requirementLength=${requirement.trim().length}; topK=${topK}`;
}

function smartContext(filters: SmartRecommendVO) {
  const f = filters.filters ?? {};
  return `route=${RECOMMENDATION_ROUTE}; endpoint=${RECOMMENDATION_ENDPOINTS.smart}; industry=${f.industry || '全部'}; sceneType=${f.sceneType || '全部'}; scriptType=${f.scriptType || '全部'}; minQualityScore=${f.minQualityScore ?? '未设置'}; referenceTextLength=${filters.referenceText?.trim().length ?? 0}; topK=${filters.topK ?? 10}`;
}

export default function BenchmarkScriptRecommendationPage() {
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const [tabValue, setTabValue] = useState(0);

  // 需求推荐
  const [requirement, setRequirement] = useState('');
  const [requirementResults, setRequirementResults] = useState<BenchmarkScriptSimilarityVO[]>([]);
  const [requirementError, setRequirementError] = useState('');
  const [requirementLoading, setRequirementLoading] = useState(false);

  // 智能推荐
  const [smartFilters, setSmartFilters] = useState<SmartRecommendVO>({
    filters: {
      industry: '',
      sceneType: '',
      scriptType: '',
      minQualityScore: 70,
    },
    referenceText: '',
    topK: 10,
  });
  const [smartResults, setSmartResults] = useState<BenchmarkScriptSimilarityVO[]>([]);
  const [smartError, setSmartError] = useState('');
  const [smartLoading, setSmartLoading] = useState(false);

  const {
    data: popularScripts,
    isFetching: popularFetching,
    isError: popularIsError,
    error: popularError,
    refetch: refetchPopular,
  } = useQuery({
    queryKey: ['popularScripts'],
    queryFn: () => benchmarkScriptRecommendationApi.getPopularScripts(10),
  });

  const {
    data: latestScripts,
    isFetching: latestFetching,
    isError: latestIsError,
    error: latestError,
    refetch: refetchLatest,
  } = useQuery({
    queryKey: ['latestQualityScripts'],
    queryFn: () => benchmarkScriptRecommendationApi.getLatestQualityScripts(10, 70),
  });

  const handleRequirementSearch = async () => {
    if (!requirement.trim()) {
      enqueueSnackbar('请输入需求描述', { variant: 'warning' });
      return;
    }

    try {
      setRequirementLoading(true);
      setRequirementError('');
      const results = await benchmarkScriptRecommendationApi.recommendByRequirement({
        requirement,
        topK: 10,
      });
      setRequirementResults(results);
      enqueueSnackbar(`找到 ${results.length} 个推荐脚本`, { variant: 'success' });
    } catch (error) {
      const message = errorText(error, '推荐失败，请检查向量嵌入、Milvus 索引或质量脚本库是否为空。');
      setRequirementError(`${message}（${requirementContext(requirement)}）`);
      enqueueSnackbar('推荐失败', { variant: 'error' });
    } finally {
      setRequirementLoading(false);
    }
  };

  const handleSmartRecommend = async () => {
    try {
      setSmartLoading(true);
      setSmartError('');
      const results = await benchmarkScriptRecommendationApi.smartRecommend(smartFilters);
      setSmartResults(results);
      enqueueSnackbar(`找到 ${results.length} 个推荐脚本`, { variant: 'success' });
    } catch (error) {
      const message = errorText(error, '智能推荐失败，请检查筛选条件、向量索引和质量脚本库。');
      setSmartError(`${message}（${smartContext(smartFilters)}）`);
      enqueueSnackbar('推荐失败', { variant: 'error' });
    } finally {
      setSmartLoading(false);
    }
  };

  const renderScriptList = (scripts: BenchmarkScriptSimilarityVO[] | undefined, loading = false) => {
    if (!scripts || scripts.length === 0) {
      return (
        <Typography
          data-testid="benchmark-recommendation-empty"
          data-no-static-script-fallback="true"
          variant="body2"
          color="text.secondary"
          align="center"
          sx={{ py: 4 }}
        >
          {loading ? '正在读取推荐结果...' : '暂无推荐结果；请确认质量脚本已入库并完成向量嵌入/索引'}
        </Typography>
      );
    }

    return (
      <List
        data-testid="benchmark-recommendation-result-list"
        data-contract-scope="benchmark-script-recommendation-real-results"
        data-ready-endpoints={RECOMMENDATION_READY_ENDPOINTS}
        data-no-static-script-fallback="true"
        data-result-count={scripts.length}
      >
        {scripts.map((script) => (
          <ListItem
            key={script.scriptId}
            sx={{
              border: 1,
              borderColor: 'divider',
              borderRadius: 1,
              mb: 2,
              cursor: 'pointer',
              '&:hover': { bgcolor: 'action.hover' },
            }}
            onClick={() => navigate(shortvideoBenchmarkQualityScriptPath(script.scriptId))}
          >
            <ListItemText
              primary={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                  <Typography variant="body1" sx={{ flex: 1 }}>
                    {script.scriptContent}
                  </Typography>
                  <Chip
                    label={`质量: ${script.qualityScore.toFixed(1)}`}
                    color={script.qualityScore >= 80 ? 'success' : 'primary'}
                    size="small"
                  />
                  {script.similarityScore && (
                    <Chip label={`相似度: ${(script.similarityScore * 100).toFixed(0)}%`} size="small" variant="outlined" />
                  )}
                </Box>
              }
              secondary={
                <Box>
                  <Box sx={{ display: 'flex', gap: 1, mb: 1 }}>
                    {script.industry && <Chip label={script.industry} size="small" />}
                    {script.sceneType && <Chip label={script.sceneType} size="small" />}
                    {script.scriptType && <Chip label={script.scriptType} size="small" />}
                  </Box>
                  <Grid container spacing={2}>
                    <Grid item xs={3}>
                      <Typography variant="caption" color="text.secondary">
                        播放: {script.viewsCount?.toLocaleString() || '-'}
                      </Typography>
                    </Grid>
                    <Grid item xs={3}>
                      <Typography variant="caption" color="text.secondary">
                        点赞: {script.likesCount?.toLocaleString() || '-'}
                      </Typography>
                    </Grid>
                    <Grid item xs={3}>
                      <Typography variant="caption" color="text.secondary">
                        互动率: {script.engagementRate ? `${script.engagementRate.toFixed(2)}%` : '-'}
                      </Typography>
                    </Grid>
                    <Grid item xs={3}>
                      <Typography variant="caption" color="text.secondary">
                        传播力: {script.viralScore ? script.viralScore.toFixed(1) : '-'}
                      </Typography>
                    </Grid>
                  </Grid>
                </Box>
              }
            />
          </ListItem>
        ))}
      </List>
    );
  };

  const popularErrorMessage = `${errorText(popularError, '热门脚本加载失败')}（route=${RECOMMENDATION_ROUTE}; endpoint=${RECOMMENDATION_ENDPOINTS.popular}; topK=10）`;
  const latestErrorMessage = `${errorText(latestError, '最新高质量脚本加载失败')}（route=${RECOMMENDATION_ROUTE}; endpoint=${RECOMMENDATION_ENDPOINTS.latest}; topK=10; minQualityScore=70）`;
  const popularCount = popularScripts?.length ?? 0;
  const latestCount = latestScripts?.length ?? 0;
  const totalReady = popularCount + latestCount;

  return (
    <Box
      data-testid="benchmark-script-recommendation-page"
      data-contract-scope="benchmark-script-vector-recommendation"
      data-ready-endpoints={RECOMMENDATION_READY_ENDPOINTS}
      data-unsupported-endpoints={RECOMMENDATION_UNSUPPORTED_ENDPOINTS}
      data-no-local-recommendation-fallback="true"
      data-no-static-script-fallback="true"
      data-popular-error={popularIsError ? 'true' : 'false'}
      data-latest-error={latestIsError ? 'true' : 'false'}
      sx={{ p: 3 }}
    >
      <PageHeader
        title="脚本推荐引擎"
        subtitle="基于质量脚本库、向量嵌入和 Milvus 相似检索推荐可复用脚本。"
        actions={
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => {
              void refetchPopular();
              void refetchLatest();
            }}
            disabled={popularFetching || latestFetching}
          >
            刷新
          </Button>
        }
      />

      {(popularIsError || latestIsError) && (
        <Stack
          data-testid="benchmark-recommendation-inventory-error"
          data-no-local-recommendation-fallback="true"
          data-no-static-script-fallback="true"
          spacing={1}
          sx={{ mb: 2 }}
        >
          {popularIsError && <ErrorAlert title="热门脚本加载失败" message={popularErrorMessage} onRetry={() => void refetchPopular()} />}
          {latestIsError && <ErrorAlert title="最新脚本加载失败" message={latestErrorMessage} onRetry={() => void refetchLatest()} />}
        </Stack>
      )}

      <Alert severity={totalReady > 0 ? 'info' : 'warning'} variant="outlined" sx={{ mb: 2 }}>
        当前可直接展示的推荐样本 {totalReady} 条；若为 0，优先检查质量脚本入库、embedding 生成和 Milvus 索引任务。
      </Alert>

      <Card
        data-testid="benchmark-recommendation-workbench"
        data-contract-scope="benchmark-recommendation-tabs"
        data-ready-endpoints={RECOMMENDATION_READY_ENDPOINTS}
        data-no-static-script-fallback="true"
      >
        <CardContent>
          <Tabs value={tabValue} onChange={(_, v) => setTabValue(v)}>
            <Tab icon={<SearchIcon />} label="需求推荐" iconPosition="start" />
            <Tab icon={<AutoAwesomeIcon />} label="智能推荐" iconPosition="start" />
            <Tab icon={<TrendingUpIcon />} label="热门脚本" iconPosition="start" />
            <Tab icon={<ScheduleIcon />} label="最新高质量" iconPosition="start" />
          </Tabs>

          <TabPanel value={tabValue} index={0}>
            <Paper
              data-testid="benchmark-requirement-panel-surface"
              data-contract-scope="benchmark-recommendation-requirement"
              data-ready-endpoints={RECOMMENDATION_ENDPOINTS.requirement}
              data-no-local-recommendation-fallback="true"
              data-input-retained={requirementError ? 'true' : 'false'}
              sx={(theme) => ({
                p: 2,
                mb: 3,
                bgcolor: theme.palette.mode === 'dark' ? alpha(theme.palette.common.white, 0.04) : alpha(theme.palette.common.black, 0.025),
                border: '1px solid',
                borderColor: 'divider',
              })}
            >
              <Typography variant="subtitle2" gutterBottom>
                描述您的需求
              </Typography>
              <TextField
                fullWidth
                multiline
                rows={4}
                placeholder="例如：我需要一个护肤品直播脚本，重点突出产品功效，适合30-40岁女性..."
                value={requirement}
                onChange={(e) => setRequirement(e.target.value)}
                sx={{ mb: 2 }}
              />
              {requirementError && (
                <Box data-testid="benchmark-requirement-error" data-input-retained="true" data-no-static-script-fallback="true">
                  <ErrorAlert title="需求推荐失败" message={requirementError} severity="warning" />
                </Box>
              )}
              <Button variant="contained" startIcon={<SearchIcon />} onClick={handleRequirementSearch} disabled={requirementLoading}>
                {requirementLoading ? '推荐中...' : '搜索推荐'}
              </Button>
            </Paper>

            {renderScriptList(requirementResults, requirementLoading)}
          </TabPanel>

          <TabPanel value={tabValue} index={1}>
            <Paper
              data-testid="benchmark-smart-panel-surface"
              data-contract-scope="benchmark-recommendation-smart"
              data-ready-endpoints={RECOMMENDATION_ENDPOINTS.smart}
              data-no-local-recommendation-fallback="true"
              data-input-retained={smartError ? 'true' : 'false'}
              sx={(theme) => ({
                p: 2,
                mb: 3,
                bgcolor: theme.palette.mode === 'dark' ? alpha(theme.palette.common.white, 0.04) : alpha(theme.palette.common.black, 0.025),
                border: '1px solid',
                borderColor: 'divider',
              })}
            >
              <Typography variant="subtitle2" gutterBottom>
                设置筛选条件
              </Typography>
              <Grid container spacing={2} sx={{ mb: 2 }}>
                <Grid item xs={12} md={3}>
                  <TextField
                    fullWidth
                    size="small"
                    select
                    label="行业"
                    value={smartFilters.filters?.industry || ''}
                    onChange={(e) =>
                      setSmartFilters((prev) => ({
                        ...prev,
                        filters: { ...prev.filters, industry: e.target.value },
                      }))
                    }
                  >
                    <MenuItem value="">全部</MenuItem>
                    <MenuItem value="护肤">护肤</MenuItem>
                    <MenuItem value="彩妆">彩妆</MenuItem>
                    <MenuItem value="服饰">服饰</MenuItem>
                    <MenuItem value="食品">食品</MenuItem>
                  </TextField>
                </Grid>
                <Grid item xs={12} md={3}>
                  <TextField
                    fullWidth
                    size="small"
                    select
                    label="场景"
                    value={smartFilters.filters?.sceneType || ''}
                    onChange={(e) =>
                      setSmartFilters((prev) => ({
                        ...prev,
                        filters: { ...prev.filters, sceneType: e.target.value },
                      }))
                    }
                  >
                    <MenuItem value="">全部</MenuItem>
                    <MenuItem value="直播">直播</MenuItem>
                    <MenuItem value="短视频">短视频</MenuItem>
                    <MenuItem value="图文">图文</MenuItem>
                  </TextField>
                </Grid>
                <Grid item xs={12} md={3}>
                  <TextField
                    fullWidth
                    size="small"
                    type="number"
                    label="最低质量评分"
                    value={smartFilters.filters?.minQualityScore || 70}
                    onChange={(e) =>
                      setSmartFilters((prev) => ({
                        ...prev,
                        filters: { ...prev.filters, minQualityScore: Number(e.target.value) },
                      }))
                    }
                  />
                </Grid>
                <Grid item xs={12} md={3}>
                  <TextField
                    fullWidth
                    size="small"
                    type="number"
                    label="推荐数量"
                    value={smartFilters.topK || 10}
                    onChange={(e) => setSmartFilters((prev) => ({ ...prev, topK: Number(e.target.value) }))}
                  />
                </Grid>
              </Grid>

              <TextField
                fullWidth
                multiline
                rows={3}
                placeholder="参考文本（可选）：输入您想要参考的脚本内容..."
                value={smartFilters.referenceText || ''}
                onChange={(e) => setSmartFilters((prev) => ({ ...prev, referenceText: e.target.value }))}
                sx={{ mb: 2 }}
              />

              {smartError && (
                <Box data-testid="benchmark-smart-error" data-input-retained="true" data-no-static-script-fallback="true">
                  <ErrorAlert title="智能推荐失败" message={smartError} severity="warning" />
                </Box>
              )}
              <Button variant="contained" startIcon={<AutoAwesomeIcon />} onClick={handleSmartRecommend} disabled={smartLoading}>
                {smartLoading ? '推荐中...' : '智能推荐'}
              </Button>
            </Paper>

            {renderScriptList(smartResults, smartLoading)}
          </TabPanel>

          <TabPanel value={tabValue} index={2}>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              基于引用次数和质量评分的热门脚本
            </Typography>
            <Divider sx={{ my: 2 }} />
            {renderScriptList(popularScripts, popularFetching)}
          </TabPanel>

          <TabPanel value={tabValue} index={3}>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              最近入库的高质量脚本（质量评分 ≥ 70）
            </Typography>
            <Divider sx={{ my: 2 }} />
            {renderScriptList(latestScripts, latestFetching)}
          </TabPanel>
        </CardContent>
      </Card>
    </Box>
  );
}
