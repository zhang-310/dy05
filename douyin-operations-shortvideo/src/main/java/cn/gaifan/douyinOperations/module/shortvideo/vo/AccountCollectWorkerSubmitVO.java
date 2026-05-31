package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Data
public class AccountCollectWorkerSubmitVO {
    private Long taskId;
    private String workerId;
    private String accountName;
    private String secUid;
    private List<CollectedVideoVO> videos = new ArrayList<>();

    @Data
    public static class CollectedVideoVO {
        private String videoId;
        private String videoUrl;
        private String title;
        private String coverUrl;
        private Long viewCount;
        private Long likeCount;
        private Long commentCount;
        private Long shareCount;
        private Long favoriteCount;
        private Integer duration;
        private Timestamp publishTime;
    }
}
