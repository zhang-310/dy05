package cn.gaifan.douyinOperations.module.storage.vo;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class SysFileVO {
    private Long id;
    private Long ownerId;
    private String originalName;
    private String storageName;
    private String storagePath;
    private String fileUrl;
    private String fileType;
    private String fileExt;
    private Long fileSize;
    private String module;
    private String provider;
    private Timestamp createTime;
}
