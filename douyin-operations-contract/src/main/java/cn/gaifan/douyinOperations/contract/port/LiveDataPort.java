package cn.gaifan.douyinOperations.contract.port;

import cn.gaifan.douyinOperations.contract.payment.SessionGmvSummary;
import cn.gaifan.douyinOperations.contract.payment.SessionCompletionRate;
import java.util.List;

/**
 * 直播数据 Port — live ↔ payment/analytics 去耦
 */
public interface LiveDataPort {
    SessionGmvSummary getSessionGmv(Long sessionId);
    List<SessionGmvSummary> getTopSessionsByGmv(int limit);
    SessionCompletionRate getCompletionRate(Long sessionId);
}
