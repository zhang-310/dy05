package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectQueueHealthVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectQueueRepairVO;

public interface AccountCollectQueueOpsService {

    AccountCollectQueueHealthVO health(Long userId);

    AccountCollectQueueRepairVO repair(Long userId);
}
