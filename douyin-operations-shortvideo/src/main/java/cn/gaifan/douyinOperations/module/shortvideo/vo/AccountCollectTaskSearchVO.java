package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 账号采集任务查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AccountCollectTaskSearchVO extends ShortVideoBasicQueryDto {

    private String status;

    private String accountName;
}
