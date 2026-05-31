package cn.gaifan.douyinOperations.module.drama.vo;

import lombok.Data;

@Data
public class DramaUpdateVO {
    private Long id;
    private String title;
    private String description;
    private String genre;
    private String script;
    private Integer episodeCount;
}
