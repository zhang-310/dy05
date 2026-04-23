package cn.gaifan.douyinOperations.module.storage.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 存储文件项（列表/展示用），含 CDN 访问地址
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageFileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 对象 key（路径） */
    private String key;
    /** 大小（字节） */
    private Long size;
    /** 最后修改时间 */
    private Date lastModified;
    /** 公网/CDN 访问 URL（可直接用于外链） */
    private String url;
    /** 是否为目录占位（BOS 无真实目录，仅前缀） */
    private Boolean directory;
}
