/**
 * 分块上传对话框组件
 * 提供文件选择、进度显示、暂停/恢复/取消等功能
 */

import React, { useState, useRef } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  LinearProgress,
  Box,
  Typography,
  Paper,
  Grid,
  IconButton,
  Alert,
  alpha,
  useTheme,
} from '@mui/material';
import {
  CloudUpload as CloudUploadIcon,
  Pause as PauseIcon,
  PlayArrow as PlayArrowIcon,
  Close as CloseIcon,
} from '@mui/icons-material';
import { useChunkedUpload } from '@/hooks/useChunkedUpload';
import { formatFileSize, formatSpeed, formatTime } from '@/utils/fileUtils';

export interface UploadDialogProps {
  open: boolean;
  onClose: () => void;
  onSuccess?: (fileUrl: string, storageKey: string) => void;
  onError?: (error: Error) => void;
  storageKey?: string; // 可选：预定义的存储路径
  module?: string; // 可选：模块标识
  acceptTypes?: string; // 可选：接受的文件类型（如 ".mp4,.avi"）
  maxFileSize?: number; // 可选：最大文件大小（字节），默认 10 GB
}

interface UploadStatistics {
  fileName: string;
  fileSize: string;
  totalChunks: number;
  uploadedChunks: number;
  failedChunks: number;
  progressPercent: number;
  uploadSpeed: string;
  remainingTime: string;
  uploadedSize: string;
  totalSize: string;
}

const UploadDialog: React.FC<UploadDialogProps> = ({
  open,
  onClose,
  onSuccess,
  onError,
  storageKey,
  module,
  acceptTypes = '*',
  maxFileSize = 10 * 1024 * 1024 * 1024, // 10 GB
}) => {
  const theme = useTheme();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadStats, setUploadStats] = useState<UploadStatistics | null>(null);
  const [isPaused, setIsPaused] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSecondUpload, setIsSecondUpload] = useState(false);
  const uploadSurface = alpha(theme.palette.text.primary, theme.palette.mode === 'dark' ? 0.07 : 0.04);
  const uploadSurfaceHover = alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.16 : 0.07);
  const uploadBorder = alpha(theme.palette.text.primary, theme.palette.mode === 'dark' ? 0.28 : 0.22);
  const uploadPrimaryColor = theme.palette.mode === 'dark' ? theme.palette.primary.light : theme.palette.primary.main;

  const { task, isUploading, startUpload, pauseUpload, resumeUpload, abortUpload } =
    useChunkedUpload({
      onProgress: (newStats) => {
        setUploadStats({
          fileName: selectedFile?.name || '',
          fileSize: formatFileSize(newStats.totalSize),
          totalChunks: newStats.totalChunks,
          uploadedChunks: newStats.uploadedChunks,
          failedChunks: newStats.failedChunks,
          progressPercent: newStats.progressPercent,
          uploadSpeed: formatSpeed(newStats.uploadSpeed),
          remainingTime: formatTime(newStats.remainingTime),
          uploadedSize: formatFileSize(newStats.uploadedSize),
          totalSize: formatFileSize(newStats.totalSize),
        });
      },
      onComplete: (result) => {
        setErrorMessage(null);
        const uploadResult = result as { fileUrl?: string; storageKey?: string };
        onSuccess?.(uploadResult.fileUrl ?? '', uploadResult.storageKey ?? '');
        handleClose();
      },
      onError: (error) => {
        setErrorMessage(error.message);
        onError?.(error);
      },
    });

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (!files || files.length === 0) return;

    const file = files[0];

    // 验证文件大小
    if (file.size > maxFileSize) {
      setErrorMessage(
        `文件过大。最大允许大小：${formatFileSize(maxFileSize)}，当前文件大小：${formatFileSize(file.size)}`
      );
      return;
    }

    setSelectedFile(file);
    setErrorMessage(null);
    setUploadStats(null);
  };

  const handleStartUpload = async () => {
    if (!selectedFile) return;

    try {
      setErrorMessage(null);
      setIsPaused(false);

      // 生成存储路径（如果未提供）
      let finalStorageKey = storageKey;
      if (!finalStorageKey) {
        const timestamp = Date.now();
        const ext = selectedFile.name.split('.').pop() || '';
        finalStorageKey = `uploads/${timestamp}.${ext}`;
      }

      // 启动上传
      await startUpload(selectedFile, finalStorageKey, module);

      // 检查是否是秒传
      if (task?.progressPercent === 100 && task?.totalChunks === 0) {
        setIsSecondUpload(true);
      }
    } catch (error) {
      const err = error instanceof Error ? error : new Error(String(error));
      setErrorMessage(err.message);
    }
  };

  const handlePauseResume = () => {
    if (isPaused) {
      setIsPaused(false);
      resumeUpload();
    } else {
      setIsPaused(true);
      pauseUpload();
    }
  };

  const handleCancel = async () => {
    await abortUpload();
    handleClose();
  };

  const handleClose = () => {
    setSelectedFile(null);
    setUploadStats(null);
    setErrorMessage(null);
    setIsPaused(false);
    setIsSecondUpload(false);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        <Box display="flex" justifyContent="space-between" alignItems="center">
          文件上传
          <IconButton size="small" onClick={handleClose} disabled={isUploading}>
            <CloseIcon />
          </IconButton>
        </Box>
      </DialogTitle>

      <DialogContent>
        <Box sx={{ pt: 2 }}>
          {/* 错误提示 */}
          {errorMessage && <Alert severity="error">{errorMessage}</Alert>}

          {/* 秒传成功提示 */}
          {isSecondUpload && (
            <Alert severity="success">
              该文件已存在，无需重新上传。上传已完成。
            </Alert>
          )}

          {/* 文件选择区 */}
          {!isUploading && !task && (
            <Paper
              data-testid="upload-dialog-dropzone-surface"
              data-dropzone-color={uploadPrimaryColor}
              sx={{
                p: 3,
                textAlign: 'center',
                border: `2px dashed ${uploadBorder}`,
                borderRadius: 2,
                cursor: 'pointer',
                transition: 'all 0.3s',
                backgroundColor: uploadSurface,
                '&:hover': {
                  borderColor: uploadPrimaryColor,
                  backgroundColor: uploadSurfaceHover,
                },
              }}
              onClick={() => fileInputRef.current?.click()}
            >
              <CloudUploadIcon data-testid="upload-dialog-icon-surface" sx={{ fontSize: 48, color: 'text.secondary', mb: 1 }} />
              <Typography variant="subtitle1">
                点击选择文件或拖拽文件到此处
              </Typography>
              <Typography variant="caption" color="textSecondary">
                最大文件大小：{formatFileSize(maxFileSize)}
              </Typography>
              <input
                ref={fileInputRef}
                type="file"
                hidden
                accept={acceptTypes}
                onChange={handleFileSelect}
              />
            </Paper>
          )}

          {/* 已选择文件显示 */}
          {selectedFile && !isUploading && (
            <Paper data-testid="upload-dialog-selected-file-surface" sx={{ p: 2, mt: 2, backgroundColor: uploadSurface, border: `1px solid ${theme.palette.divider}` }}>
              <Grid container spacing={1}>
                <Grid item xs={12}>
                  <Typography variant="subtitle2">已选择文件</Typography>
                </Grid>
                <Grid item xs={12}>
                  <Typography variant="body2" color="textSecondary">
                    文件名：{selectedFile.name}
                  </Typography>
                </Grid>
                <Grid item xs={12}>
                  <Typography variant="body2" color="textSecondary">
                    文件大小：{formatFileSize(selectedFile.size)}
                  </Typography>
                </Grid>
              </Grid>
            </Paper>
          )}

          {/* 上传进度显示 */}
          {(isUploading || task) && uploadStats && (
            <Box sx={{ mt: 2 }}>
              {/* 文件信息 */}
              <Paper data-testid="upload-dialog-progress-info-surface" sx={{ p: 2, mb: 2, backgroundColor: uploadSurface, border: `1px solid ${theme.palette.divider}` }}>
                <Grid container spacing={1}>
                  <Grid item xs={6}>
                    <Typography variant="caption" color="textSecondary">
                      文件名
                    </Typography>
                    <Typography variant="body2">{uploadStats.fileName}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="caption" color="textSecondary">
                      文件大小
                    </Typography>
                    <Typography variant="body2">{uploadStats.fileSize}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="caption" color="textSecondary">
                      已上传
                    </Typography>
                    <Typography variant="body2">
                      {uploadStats.uploadedSize} / {uploadStats.totalSize}
                    </Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="caption" color="textSecondary">
                      分块进度
                    </Typography>
                    <Typography variant="body2">
                      {uploadStats.uploadedChunks} / {uploadStats.totalChunks}
                    </Typography>
                  </Grid>
                </Grid>
              </Paper>

              {/* 进度条 */}
              <Box sx={{ mb: 2 }}>
                <Box
                  display="flex"
                  justifyContent="space-between"
                  alignItems="center"
                  mb={1}
                >
                  <Typography variant="subtitle2">上传进度</Typography>
                  <Typography variant="h6">{uploadStats.progressPercent}%</Typography>
                </Box>
                <LinearProgress
                  data-testid="upload-dialog-progress-surface"
                  variant="determinate"
                  value={uploadStats.progressPercent}
                  sx={{
                    height: 8,
                    borderRadius: 4,
                    backgroundColor: alpha(uploadPrimaryColor, theme.palette.mode === 'dark' ? 0.18 : 0.12),
                    '& .MuiLinearProgress-bar': {
                      backgroundColor: uploadPrimaryColor,
                    },
                  }}
                />
              </Box>

              {/* 上传速度与剩余时间 */}
              <Grid container spacing={2} mb={2}>
                <Grid item xs={6}>
                  <Typography variant="caption" color="textSecondary">
                    上传速度
                  </Typography>
                  <Typography variant="body2">{uploadStats.uploadSpeed}</Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="caption" color="textSecondary">
                    剩余时间
                  </Typography>
                  <Typography variant="body2">
                    {uploadStats.remainingTime === '0s' ? '即将完成' : uploadStats.remainingTime}
                  </Typography>
                </Grid>
              </Grid>

              {/* 失败分块提示 */}
              {uploadStats.failedChunks > 0 && (
                <Alert severity="warning" sx={{ mb: 2 }}>
                  {uploadStats.failedChunks} 个分块上传失败，已自动重试
                </Alert>
              )}
            </Box>
          )}

          {/* 上传完成状态 */}
          {task?.progressPercent === 100 && !isUploading && (
            <Alert severity="success">上传已完成</Alert>
          )}

          {/* 上传失败状态 */}
          {task?.status === 'FAILED' && (
            <Alert severity="error">
              上传失败：{task.error || '未知错误'}
            </Alert>
          )}
        </Box>
      </DialogContent>

      <DialogActions>
        {/* 文件选择阶段 */}
        {!isUploading && !task && (
          <>
            <Button onClick={handleClose} variant="outlined">
              取消
            </Button>
            <Button
              onClick={handleStartUpload}
              variant="contained"
              disabled={!selectedFile}
            >
              开始上传
            </Button>
          </>
        )}

        {/* 上传中 */}
        {isUploading && (
          <>
            <Button onClick={handleCancel} variant="outlined">
              取消上传
            </Button>
            <Button
              onClick={handlePauseResume}
              variant="contained"
              startIcon={isPaused ? <PlayArrowIcon /> : <PauseIcon />}
            >
              {isPaused ? '继续' : '暂停'}
            </Button>
          </>
        )}

        {/* 上传完成/失败 */}
        {task && !isUploading && (
          <Button onClick={handleClose} variant="contained">
            关闭
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};

export default UploadDialog;
