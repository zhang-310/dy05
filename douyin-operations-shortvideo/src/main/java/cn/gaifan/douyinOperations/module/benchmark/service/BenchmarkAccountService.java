package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.*;

import java.util.List;

/**
 * 对标账号管理服务
 */
public interface BenchmarkAccountService {

    /**
     * 分页查询账号
     */
    PageResultVO<BenchmarkAccountVO> search(BenchmarkAccountSearchVO searchVO, Long ownerId);

    /**
     * 根据ID获取账号
     */
    BenchmarkAccountVO getById(Long id, Long ownerId);

    /**
     * 保存账号（新增或更新）
     */
    BenchmarkAccountVO save(BenchmarkAccountSaveVO saveVO, Long ownerId);

    /**
     * 删除账号
     */
    void delete(Long id, Long ownerId);

    /**
     * 按关键词搜索抖音账号
     * @param searchVO 搜索参数
     * @param ownerId 用户ID
     * @return 搜索到的账号列表
     */
    List<BenchmarkAccountVO> searchByKeyword(SearchAccountByKeywordVO searchVO, Long ownerId);

    /**
     * 按URL分析账号
     * @param analyzeVO 分析参数
     * @param ownerId 用户ID
     * @return 账号信息
     */
    BenchmarkAccountVO analyzeByUrl(AnalyzeAccountByUrlVO analyzeVO, Long ownerId);
}
