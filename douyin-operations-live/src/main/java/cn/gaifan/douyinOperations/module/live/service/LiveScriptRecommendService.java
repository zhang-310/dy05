package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.vo.ScriptRecommendRequestVO;
import cn.gaifan.douyinOperations.module.live.vo.ScriptRecommendVO;
import java.util.List;

/**
 * P1-2: 多场景话术智能推荐服务。
 * 根据当前商品、时段、在线人数等上下文，从历史高效话术中推荐最优选项。
 */
public interface LiveScriptRecommendService {

    /**
     * 推荐适合当前场景的话术
     */
    List<ScriptRecommendVO> recommend(ScriptRecommendRequestVO req, Long userId);
}
