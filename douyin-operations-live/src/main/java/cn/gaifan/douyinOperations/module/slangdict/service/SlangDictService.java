package cn.gaifan.douyinOperations.module.slangdict.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.slangdict.vo.SdEntrySearchVO;
import cn.gaifan.douyinOperations.module.slangdict.vo.SdEntrySaveVO;
import cn.gaifan.douyinOperations.module.slangdict.vo.SdEntryVO;

import java.util.List;

public interface SlangDictService {

    PageResultVO<SdEntryVO> search(SdEntrySearchVO vo);

    SdEntryVO getById(Long id);

    Long save(SdEntrySaveVO vo, Long userId);

    void delete(Long id);

    List<SdEntryVO> getByProductId(Long productId, Long userId);

    void bindProduct(Long entryId, Long productId, Long userId);

    void unbindProduct(Long entryId, Long productId, Long userId);

    /** AI 为产品生成候选梗 */
    List<String> aiGeneratePhrases(Long productId, Long userId, int count);

    /** 供 LivePromptBuilder 调用，输出梗库 prompt 片段 */
    String buildSlangContextForPrompt(Long productId, Long userId);
}
