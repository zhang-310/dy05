package cn.gaifan.douyinOperations.module.benchmark.service;

import java.util.List;

/**
 * OCR文字识别服务
 */
public interface OcrService {

    /**
     * 从视频中识别文字
     * @param videoPath 视频文件路径
     * @return OCR识别的文字内容（带时间轴）
     */
    String recognizeTextFromVideo(String videoPath);

    /**
     * 从图片中识别文字
     * @param imagePath 图片文件路径
     * @return 识别的文字内容
     */
    String recognizeTextFromImage(String imagePath);

    /**
     * 批量识别图片文字
     * @param imagePaths 图片路径列表
     * @return 识别结果列表
     */
    List<OcrResult> batchRecognize(List<String> imagePaths);

    /**
     * OCR识别结果
     */
    class OcrResult {
        private String imagePath;
        private String text;
        private Double confidence;
        private Long timestamp; // 视频时间戳（毫秒）

        public OcrResult() {}

        public OcrResult(String imagePath, String text, Double confidence, Long timestamp) {
            this.imagePath = imagePath;
            this.text = text;
            this.confidence = confidence;
            this.timestamp = timestamp;
        }

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Double getConfidence() {
            return confidence;
        }

        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
