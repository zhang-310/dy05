package cn.gaifan.douyinOperations.module.douyin.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.douyin.vo.*;

/**
 * 抖音账号服务接口
 */
public interface DouyinAccountService {

    /**
     * 分页查询账号
     */
    PageResultVO<DouyinAccountVO> search(DouyinAccountSearchVO vo);

    /**
     * 获取账号详情
     */
    DouyinAccountVO getAccount(Long id);

    /**
     * 保存账号
     */
    long saveAccount(DouyinAccountSaveVO vo);

    /**
     * 删除账号
     */
    void deleteAccount(Long id);

    /**
     * 获取账号统计信息
     */
    DouyinAccountStatisticsVO getAccountStatistics(Long id);
}
