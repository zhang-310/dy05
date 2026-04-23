package cn.gaifan.douyinOperations.module.shortvideo.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 短视频模块基础查询 DTO，分页 rows 上限 100
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class ShortVideoBasicQueryDto extends BasicQueryDto {

    private static final int MAX_ROWS = 100;

    @Override
    public void validateParams() {
        super.validateParams();
        if (getRows() != null && getRows() > MAX_ROWS) {
            setRows(MAX_ROWS);
        }
    }
}
