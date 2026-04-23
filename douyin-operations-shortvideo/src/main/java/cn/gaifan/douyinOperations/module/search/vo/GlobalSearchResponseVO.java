package cn.gaifan.douyinOperations.module.search.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSearchResponseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<GlobalSearchHitVO> hits;
    private long tookMs;
}
