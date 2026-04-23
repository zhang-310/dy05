package cn.gaifan.douyinOperations.module.product.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品上播准备度检测结果
 */
public class ProductReadinessVO {
    private Long productId;
    private String productName;
    private Integer overallScore;
    private Boolean readyForLive;
    private List<ReadinessItem> items = new ArrayList<>();

    public static class ReadinessItem {
        private String dimension;
        private String status; // ok, warn, error
        private String message;
        private Integer score;

        public ReadinessItem() {}

        public ReadinessItem(String dimension, String status, String message, Integer score) {
            this.dimension = dimension;
            this.status = status;
            this.message = message;
            this.score = score;
        }

        public String getDimension() { return dimension; }
        public void setDimension(String dimension) { this.dimension = dimension; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getOverallScore() { return overallScore; }
    public void setOverallScore(Integer overallScore) { this.overallScore = overallScore; }
    public Boolean getReadyForLive() { return readyForLive; }
    public void setReadyForLive(Boolean readyForLive) { this.readyForLive = readyForLive; }
    public List<ReadinessItem> getItems() { return items; }
    public void setItems(List<ReadinessItem> items) { this.items = items; }
}
