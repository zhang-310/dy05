package cn.gaifan.douyinOperations.contract.product;

/**
 * 产品互调计费模式。
 *
 * <p>同步能力可以直接扣费并写账本；数字人、真人口播、短剧和成片渲染等长任务必须先冻结积分，
 * 等工作流执行成功后再提交扣减，失败或超时时释放冻结额度。</p>
 */
public enum ProductIntegrationBillingMode {
    /** 立即完成扣费，适合短视频洞察、MCP 工具等同步或准同步能力。 */
    IMMEDIATE,
    /** 创建目标产品工作流任务并冻结积分，由任务状态机负责最终提交或释放。 */
    WORKFLOW_RESERVED
}
