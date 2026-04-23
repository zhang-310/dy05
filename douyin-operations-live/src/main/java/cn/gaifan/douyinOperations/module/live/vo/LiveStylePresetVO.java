package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import java.util.List;

@Data
public class LiveStylePresetVO {

    private String styleKey;
    private String label;
    private String groupName;

    @Data
    public static class GroupVO {
        private String label;
        private List<StyleItem> styles;

        @Data
        public static class StyleItem {
            private String value;
            private String label;
        }
    }
}
