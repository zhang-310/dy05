package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerClaimVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerFailVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerHeartbeatVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerSubmitResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerSubmitVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerTaskVO;

public interface AccountCollectWorkerGatewayService {

    AccountCollectWorkerTaskVO claim(AccountCollectWorkerClaimVO vo);

    boolean heartbeat(AccountCollectWorkerHeartbeatVO vo);

    AccountCollectWorkerSubmitResultVO submit(AccountCollectWorkerSubmitVO vo);

    AccountCollectWorkerSubmitResultVO fail(AccountCollectWorkerFailVO vo);
}
