import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Grid,
  TextField,
  MenuItem,
  Chip,
  Paper,
  List,
  ListItem,
  ListItemText,
  Divider,
  Tab,
  Tabs,
} from '@mui/material';
import {
  Search as SearchIcon,
  AutoAwesome as AutoAwesomeIcon,
  TrendingUp as TrendingUpIcon,
  Schedule as ScheduleIcon,
} from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import { useSnackbar } from 'notistack';
import { PageHeader } from '@/components/base/PageHeader';
import { benchmarkScriptRecommendationApi } from '@/api/benchmark';
import type { BenchmarkScriptSimilarityVO, SmartRecommendVO } from '@/types/benchmark';

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

export default function BenchmarkScriptRecommendationPage() {
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const [tabValue, setTabValue] = useState(0);

  // 需求推荐
  const [requirement, setRequirement] = useState('');
  const [requirementResults, setRequirementResults] = useState<BenchmarkScriptSimilarityVO[]>([]);

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

  // 热门脚本
  const { data: popularScripts } = useQuery({
    queryKey: ['popularScripts'],
    queryFn: () => benchmarkScriptRecommendationApi.getPopularScripts(10),
  });

  // 最新高质量脚本
  const { data: latestScripts } = useQuery({
    queryKey: ['latestQualityScripts'],
    queryFn: () => benchmarkScriptRecommendationApi.getLatestQualityScripts(10, 70),
  });

  const handleRequirementSearch = async () => {
    if (!requirement.trim()) {
      enqueueSnackbar('请输入需求描述', { variant: 'warning' });
      return;
    }

    try {
      const results = await benchmarkScriptRecommendationApi.recommendByRequirement({
        requirement,
        topK: 10,
      });
      setRequirementResults(results);
      enqueueSnackbar(`找到 ${results.length} 个推荐脚本`, { variant: 'success' });
    } catch (error) {
      enqueueSnackbar('推荐失败', { variant: 'error' });
    }
  };

  const handleSmartRecommend = async () => {
    try {
      const results = await benchmarkScriptRecommendationApi.smartRecommend(smartFilters);
      setSmartResults(results);
      enqueueSnackbar(`找到 ${results.length} 个推荐脚本`, { variant: 'success' });
    } catch (error) {
      enqueueSnackbar('推荐失败', { variant: 'error' });
    }
  };

  const renderScriptList = (scripts: BenchmarkScriptSimilarityVO[] | undefined) => {
    if (!scripts || scripts.length === 0) {
      return (
        <Typography variant="body2" color="text.secondary" align="center" sx={{ py: 4 }}>
          暂无推荐结果
        </Typography>
      );
    }

    return (
      <List>
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
            onClick={() => navigate(`/benchmark/quality-script/${script.scriptId}`)}
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

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader title="脚本推荐引擎" subtitle="基于语义相似度和质量评分的智能推荐" />

      <Card>
        <CardContent>
          <Tabs value={tabValue} onChange={(_, v) => setTabValue(v)}>
            <Tab icon={<SearchIcon />} label="需求推荐" iconPosition="start" />
            <Tab icon={<AutoAwesomeIcon />} label="智能推荐" iconPosition="start" />
            <Tab icon={<TrendingUpIcon />} label="热门脚本" iconPosition="start" />
            <Tab icon={<ScheduleIcon />} label="最新高质量" iconPosition="start" />
          </Tabs>

          <TabPanel value={tabValue} index={0}>
            <Paper sx={{ p: 2, mb: 3, bgcolor: 'grey.50' }}>
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
              <Button variant="contained" startIcon={<SearchIcon />} onClick={handleRequirementSearch}>
                搜索推荐
              </Button>
            </Paper>

            {renderScriptList(requirementResults)}
          </TabPanel>

          <TabPanel value={tabValue} index={1}>
            <Paper sx={{ p: 2, mb: 3, bgcolor: 'grey.50' }}>
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

              <Button variant="contained" startIcon={<AutoAwesomeIcon />} onClick={handleSmartRecommend}>
                智能推荐
              </Button>
            </Paper>

            {renderScriptList(smartResults)}
          </TabPanel>

          <TabPanel value={tabValue} index={2}>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              基于引用次数和质量评分的热门脚本
            </Typography>
            <Divider sx={{ my: 2 }} />
            {renderScriptList(popularScripts)}
          </TabPanel>

          <TabPanel value={tabValue} index={3}>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              最近入库的高质量脚本（质量评分 ≥ 70）
            </Typography>
            <Divider sx={{ my: 2 }} />
            {renderScriptList(latestScripts)}
          </TabPanel>
        </CardContent>
      </Card>
    </Box>
  );
}
