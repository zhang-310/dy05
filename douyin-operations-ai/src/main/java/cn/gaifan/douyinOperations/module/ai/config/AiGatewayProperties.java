package cn.gaifan.douyinOperations.module.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * AI 网关成本与定价配置（与产品扣费单价对齐时可读 gf_feature）。
 */
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiGatewayProperties {

    private String defaultProvider = "mock";
    private BigDecimal retailMarkup = new BigDecimal("1.5");
    private BigDecimal chatUnitCostCny = new BigDecimal("0.01");
    private BigDecimal generationUnitCostCny = new BigDecimal("0.05");

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public BigDecimal getRetailMarkup() {
        return retailMarkup;
    }

    public void setRetailMarkup(BigDecimal retailMarkup) {
        this.retailMarkup = retailMarkup;
    }

    public BigDecimal getChatUnitCostCny() {
        return chatUnitCostCny;
    }

    public void setChatUnitCostCny(BigDecimal chatUnitCostCny) {
        this.chatUnitCostCny = chatUnitCostCny;
    }

    public BigDecimal getGenerationUnitCostCny() {
        return generationUnitCostCny;
    }

    public void setGenerationUnitCostCny(BigDecimal generationUnitCostCny) {
        this.generationUnitCostCny = generationUnitCostCny;
    }
}
