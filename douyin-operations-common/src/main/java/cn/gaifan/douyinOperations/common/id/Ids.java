package cn.gaifan.douyinOperations.common.id;

import java.util.UUID;

public final class Ids {
    private Ids() {
    }

    public static String compactUuid(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
