package cn.gaifan.douyinOperations.module.abtest.vo;

import lombok.Data;

/**
 * 话术风格实验转化记录请求
 */
@Data
public class ScriptStyleConversionRequest {
    private Long experimentId;
    private Long variantId;
    /** 用于去重，可选 */
    private String userFingerprint;
}
