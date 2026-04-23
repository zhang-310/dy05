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
} from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  Star as StarIcon,
  Visibility as VisibilityIcon,
  ThumbUp as ThumbUpIcon,
} from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import { PageHeader } from '@/components/base/PageHeader';
import { benchmarkQualityScriptApi, benchmarkScriptSimilarityApi } from '@/api/benchmark';
import type { BenchmarkScriptSimilarityVO } from '@/types/benchmark';

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

export default function BenchmarkQualityScriptDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [tabValue, setTabValue] = useState(0);

  const scriptId = Number(id);

  // 查询脚本详情
  const { data: script, isLoading } = useQuery({
    queryKey: ['benchmarkQualityScript', scriptId],
    queryFn: () => benchmarkQualityScriptApi.get(scriptId),
    enabled: !!scriptId,
  });

  // 查询相似脚本
  const { data: similarScripts } = useQuery({
    queryKey: ['similarScripts', scriptId],
    queryFn: () => benchmarkScriptSimilarityApi.findSimilar({ scriptId, topK: 10, minScore: 0.7 }),
    enabled: !!scriptId,
  });

  if (isLoading || !script) {
    return <Box sx={{ p: 3 }}>加载中...</Box>;
  }

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader
        title="质量脚本详情"
        subtitle={`ID: ${script.id}`}
        actions={
          <Button variant="outlined" startIcon={<ArrowBackIcon />} onClick={() => navigate('/benchmark/quality-script')}>
            返回列表
          </Button>
        }
      />

      <Grid container spacing={3}>
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

              <Paper sx={{ p: 2, bgcolor: 'grey.50', mb: 3 }}>
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
                        onClick={() => navigate(`/benchmark/quality-script/${item.scriptId}`)}
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
                  <Typography variant="body2" color="text.secondary" align="center" sx={{ py: 4 }}>
                    暂无相似脚本
                  </Typography>
                )}
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
    </Box>
  );
}
