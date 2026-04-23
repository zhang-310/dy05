/**
 * 搜索筛选器面板组件
 * W-06: 脚本类型、风格、评分范围、日期范围
 * @author Claude Code
 * @since 2026-03-06
 */

import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  FormGroup,
  FormControlLabel,
  Checkbox,
  Slider,
  TextField,
  Button,
  Stack,
  Divider,
  Chip,
  Collapse,
  IconButton,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ClearIcon from '@mui/icons-material/Clear';
import type { SearchFiltersVO } from '@/types/search';

interface SearchFiltersPanelProps {
  onFiltersChange?: (filters: SearchFiltersVO) => void;
  onReset?: () => void;
  defaultFilters?: SearchFiltersVO;
  expandable?: boolean;
}

/**
 * 脚本类型选项
 */
const scriptTypeOptions = [
  { value: 'product', label: '商品话术' },
  { value: 'shortvideo', label: '短视频' },
  { value: 'live', label: '直播话术' },
  { value: 'event', label: '活动话术' },
];

/**
 * 风格选项
 */
const styleOptions = [
  { value: 'professional', label: '专业' },
  { value: 'casual', label: '轻松' },
  { value: 'emotional', label: '情感' },
  { value: 'humorous', label: '幽默' },
  { value: 'persuasive', label: '有说服力' },
];

export const SearchFiltersPanel: React.FC<SearchFiltersPanelProps> = ({
  onFiltersChange,
  onReset,
  defaultFilters = {},
  expandable = false,
}) => {
  const [expanded, setExpanded] = useState(!expandable);
  const [scriptTypes, setScriptTypes] = useState<string[]>(
    defaultFilters.scriptType ? [defaultFilters.scriptType] : []
  );
  const [styles, setStyles] = useState<string[]>(defaultFilters.style || []);
  const [scoreRange, setScoreRange] = useState<[number, number]>(
    defaultFilters.scoreRange || [0, 100]
  );
  const [dateRange, setDateRange] = useState<[string, string]>(
    defaultFilters.dateRange || ['', '']
  );
  const [minUsageCount, setMinUsageCount] = useState<number>(
    defaultFilters.minUsageCount || 0
  );

  const hasActiveFilters =
    scriptTypes.length > 0 ||
    styles.length > 0 ||
    scoreRange[0] > 0 ||
    scoreRange[1] < 100 ||
    dateRange[0] ||
    dateRange[1] ||
    minUsageCount > 0;

  const handleScriptTypeChange = (type: string) => {
    const newTypes = scriptTypes.includes(type)
      ? scriptTypes.filter((t) => t !== type)
      : [type]; // 单选
    setScriptTypes(newTypes);
    emitFiltersChange({
      scriptType: newTypes[0] as 'product' | 'shortvideo' | 'live' | 'event' | undefined,
      style: styles,
      scoreRange,
      dateRange,
      minUsageCount
    });
  };

  const handleStyleChange = (style: string) => {
    const newStyles = styles.includes(style)
      ? styles.filter((s) => s !== style)
      : [...styles, style];
    setStyles(newStyles);
    emitFiltersChange({
      scriptType: scriptTypes[0] as 'product' | 'shortvideo' | 'live' | 'event' | undefined,
      style: newStyles,
      scoreRange,
      dateRange,
      minUsageCount
    });
  };

  const handleScoreRangeChange = (_: Event, newValue: number | number[]) => {
    const range = newValue as [number, number];
    setScoreRange(range);
    emitFiltersChange({
      scriptType: scriptTypes[0] as 'product' | 'shortvideo' | 'live' | 'event' | undefined,
      style: styles,
      scoreRange: range,
      dateRange,
      minUsageCount,
    });
  };

  const handleDateChange = (type: 'from' | 'to', value: string) => {
    const newRange: [string, string] = type === 'from' ? [value, dateRange[1]] : [dateRange[0], value];
    setDateRange(newRange);
    emitFiltersChange({
      scriptType: scriptTypes[0] as 'product' | 'shortvideo' | 'live' | 'event' | undefined,
      style: styles,
      scoreRange,
      dateRange: newRange,
      minUsageCount,
    });
  };

  const handleUsageCountChange = (value: number) => {
    setMinUsageCount(value);
    emitFiltersChange({
      scriptType: scriptTypes[0] as 'product' | 'shortvideo' | 'live' | 'event' | undefined,
      style: styles,
      scoreRange,
      dateRange,
      minUsageCount: value,
    });
  };

  const emitFiltersChange = (filters: SearchFiltersVO) => {
    onFiltersChange?.(filters as SearchFiltersVO);
  };

  const handleReset = () => {
    setScriptTypes([]);
    setStyles([]);
    setScoreRange([0, 100]);
    setDateRange(['', '']);
    setMinUsageCount(0);
    onReset?.();
  };

  return (
    <Card sx={{ mb: 3 }}>
      <CardContent>
        {expandable && (
          <Box
            sx={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              cursor: 'pointer',
              mb: 2,
            }}
            onClick={() => setExpanded(!expanded)}
          >
            <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
              筛选器
              {hasActiveFilters && <Chip label={`已激活`} size="small" sx={{ ml: 1 }} />}
            </Typography>
            <IconButton size="small">
              <ExpandMoreIcon
                sx={{
                  transform: expanded ? 'rotate(180deg)' : 'rotate(0)',
                  transition: 'transform 0.3s ease',
                }}
              />
            </IconButton>
          </Box>
        )}

        <Collapse in={expanded}>
          <Stack spacing={2.5}>
            {/* 脚本类型 */}
            <Box>
              <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1.5 }}>
                脚本类型
              </Typography>
              <FormGroup>
                {scriptTypeOptions.map((option) => (
                  <FormControlLabel
                    key={option.value}
                    control={
                      <Checkbox
                        checked={scriptTypes.includes(option.value)}
                        onChange={() => handleScriptTypeChange(option.value)}
                      />
                    }
                    label={option.label}
                  />
                ))}
              </FormGroup>
            </Box>

            <Divider />

            {/* 风格 */}
            <Box>
              <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1.5 }}>
                风格标签
              </Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {styleOptions.map((option) => (
                  <Chip
                    key={option.value}
                    label={option.label}
                    onClick={() => handleStyleChange(option.value)}
                    variant={styles.includes(option.value) ? 'filled' : 'outlined'}
                    color={styles.includes(option.value) ? 'primary' : 'default'}
                  />
                ))}
              </Box>
            </Box>

            <Divider />

            {/* 评分范围 */}
            <Box>
              <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1.5 }}>
                效果评分范围: {scoreRange[0]} - {scoreRange[1]}
              </Typography>
              <Slider
                value={scoreRange}
                onChange={handleScoreRangeChange}
                min={0}
                max={100}
                step={5}
                marks={[
                  { value: 0, label: '0' },
                  { value: 50, label: '50' },
                  { value: 100, label: '100' },
                ]}
              />
            </Box>

            <Divider />

            {/* 日期范围 */}
            <Box>
              <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1 }}>
                创建日期范围
              </Typography>
              <Stack direction="row" spacing={2}>
                <TextField
                  label="起始日期"
                  type="date"
                  value={dateRange[0]}
                  onChange={(e) => handleDateChange('from', e.target.value)}
                  InputLabelProps={{ shrink: true }}
                  size="small"
                />
                <TextField
                  label="结束日期"
                  type="date"
                  value={dateRange[1]}
                  onChange={(e) => handleDateChange('to', e.target.value)}
                  InputLabelProps={{ shrink: true }}
                  size="small"
                />
              </Stack>
            </Box>

            <Divider />

            {/* 最少使用次数 */}
            <Box>
              <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1.5 }}>
                最少使用次数: {minUsageCount}
              </Typography>
              <Slider
                value={minUsageCount}
                onChange={(_, value) => handleUsageCountChange(value as number)}
                min={0}
                max={100}
                step={10}
                marks={[
                  { value: 0, label: '0' },
                  { value: 50, label: '50' },
                  { value: 100, label: '100+' },
                ]}
              />
            </Box>

            {/* 重置按钮 */}
            {hasActiveFilters && (
              <Button
                startIcon={<ClearIcon />}
                onClick={handleReset}
                fullWidth
                variant="outlined"
              >
                重置筛选
              </Button>
            )}
          </Stack>
        </Collapse>
      </CardContent>
    </Card>
  );
};
