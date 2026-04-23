package cn.gaifan.douyinOperations.module.benchmark.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 抖音扫码登录：启动会话返回（含二维码截图 Base64，不含 data: 前缀）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DouyinQrLoginStartResultVO {

    private String sessionId;

    /** PNG 的 Base64 */
    private String qrImageBase64;

    private String message;
}
