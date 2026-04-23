package cn.gaifan.douyinOperations.module.live.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 直播产品类型时长配置，外置到 application-ai.yml
 * <pre>
 * live:
 *   product-type-duration:
 *     hot:
 *       min: 60
 *       max: 300
 *       word-min: 180
 *       word-max: 900
 *     profit:
 *       min: 30
 *       max: 60
 *       word-min: 90
 *       word-max: 180
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "live")
public class LivePromptConfig {

    private Map<String, ProductTypeDuration> productTypeDuration;

    public Map<String, ProductTypeDuration> getProductTypeDuration() {
        return productTypeDuration;
    }

    public void setProductTypeDuration(Map<String, ProductTypeDuration> productTypeDuration) {
        this.productTypeDuration = productTypeDuration;
    }

    /**
     * 根据产品类型获取时长配置，无配置则返回 null
     */
    public ProductTypeDuration getDuration(String type) {
        if (productTypeDuration == null || type == null) return null;
        return productTypeDuration.get(type);
    }

    public static class ProductTypeDuration {
        private int min = 30;
        private int max = 60;
        private int wordMin = 90;
        private int wordMax = 180;

        public int getMin() { return min; }
        public void setMin(int min) { this.min = min; }
        public int getMax() { return max; }
        public void setMax(int max) { this.max = max; }
        public int getWordMin() { return wordMin; }
        public void setWordMin(int wordMin) { this.wordMin = wordMin; }
        public int getWordMax() { return wordMax; }
        public void setWordMax(int wordMax) { this.wordMax = wordMax; }
    }
}
