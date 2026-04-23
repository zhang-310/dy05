package cn.gaifan.douyinOperations.module.tianapi.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 每日简报单项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulletinItemVO {
    private String title;
    private String digest;
    private String mtime;
}
