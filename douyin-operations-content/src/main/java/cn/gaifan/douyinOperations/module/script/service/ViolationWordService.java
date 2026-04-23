package cn.gaifan.douyinOperations.module.script.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.script.vo.*;

import java.util.List;
import java.util.Map;

public interface ViolationWordService {
    PageResultVO<ViolationWordVO> search(ViolationWordSearchVO vo);
    ViolationWordVO getById(Long id);
    long save(ViolationWordSaveVO vo);
    void delete(Long id);
    List<ViolationWordVO> listActive();
    /** 违规检测：合并公共+个人库，按 scope 过滤，大小写不敏感。scope: all/live/video；userId 为空时仅用公共库 */
    ViolationCheckResultVO check(String text, String scope, Long userId);

    /** 批量违规检测：复用单文本逻辑，词库只加载一次 */
    ViolationCheckBatchResultVO checkBatch(ViolationCheckBatchVO vo, Long userId);

    /**
     * AI 推荐违规词替换建议
     */
    ViolationReplacementResultVO suggestReplacement(ViolationReplacementRequestVO vo);

    /** 批量导入违规词（CSV），返回 imported/skipped/errors */
    Map<String, Object> importFromCsv(byte[] csvBytes);

    /** 导出违规词为 CSV */
    byte[] exportToCsv();
}
