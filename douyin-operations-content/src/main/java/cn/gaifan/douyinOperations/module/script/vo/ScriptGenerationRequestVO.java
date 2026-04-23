package cn.gaifan.douyinOperations.module.script.vo;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public class ScriptGenerationRequestVO {
    @NotBlank(message = "产品名称不能为空")
    @Size(max = 100, message = "产品名称不超过100字")
    private String productName;

    @NotNull(message = "产品价格不能为空")
    @DecimalMin("0.01")
    private BigDecimal productPrice;

    @NotEmpty(message = "核心卖点不能为空")
    private List<String> keyFeatures;

    @NotNull(message = "时长不能为空")
    @Min(15)
    @Max(300)
    private Integer duration;

    @NotBlank(message = "风格不能为空")
    private String style;

    @Min(1)
    @Max(5)
    private Integer variants = 3;

    // Getters and Setters
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public BigDecimal getProductPrice() { return productPrice; }
    public void setProductPrice(BigDecimal productPrice) { this.productPrice = productPrice; }

    public List<String> getKeyFeatures() { return keyFeatures; }
    public void setKeyFeatures(List<String> keyFeatures) { this.keyFeatures = keyFeatures; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public Integer getVariants() { return variants; }
    public void setVariants(Integer variants) { this.variants = variants; }
}
