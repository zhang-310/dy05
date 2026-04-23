package cn.gaifan.douyinOperations.module.ai.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 高光话术生成请求
 */
@Data
public class HuashuGenerateRequestVO {

    /** 主播名，用于生成话术的人设适配 */
    @NotBlank(message = "hostName 不能为空")
    @Size(max = 64)
    private String hostName;

    /** 可选：本地目录路径，导入到 huashu 后再检索生成 */
    @Size(max = 1024)
    private String sourcePath;

    /** 可选：RAG 检索关键词，默认使用 hostName + 高光话术 开场 种草 转化 留人 */
    @Size(max = 256)
    private String query;
}
