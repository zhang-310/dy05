package cn.gaifan.douyinOperations.contract.payment;

/**
 * 单场 GMV 聚合结果
 *
 * 实现 vision 中 P0 优先级的支付↔场次关联。
 */
public record SessionGmvSummary(
        Long sessionId,
        String sessionTitle,
        Long totalOrderAmount,
        Long refundAmount,
        Long netGmv,
        int orderCount,
        int refundCount,
        Long accountId,
        String liveTitle
) {
    public double netGmvInWan() {
        return netGmv / 10000.0;
    }
}
