package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

/**
 * 场次数据 + 上一场数据（用于环比展示）
 */
@Data
public class SessionDataWithCompareVO {
    private LiveSessionDataVO current;
    private LiveSessionDataVO previous;
}
