package cn.gaifan.douyinOperations.contract.product;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductCodeTest {

    @Test
    void allProductsDefined() {
        assertNotNull(ProductCode.DOUYIN_OPS);
        assertNotNull(ProductCode.VIDEO_INSIGHT);
        assertNotNull(ProductCode.KNOWLEDGE_BASE);
        assertNotNull(ProductCode.DRAMA_AI);
        assertNotNull(ProductCode.DIGITAL_HUMAN);
        assertNotNull(ProductCode.PHOTO_AVATAR_VIDEO);
        assertNotNull(ProductCode.SHORTVIDEO_MAKER);
    }

    @Test
    void codesAreKebabCase() {
        assertTrue(ProductCode.DOUYIN_OPS.contains("-"));
        assertFalse(ProductCode.DOUYIN_OPS.contains("_"));
    }
}

class FeatureCodeTest {

    @Test
    void allFeaturesUseDotNotation() {
        assertTrue(FeatureCode.DOUYIN_ACCOUNT_MGMT.contains("."));
        assertTrue(FeatureCode.AI_CHAT.contains("."));
    }

    @Test
    void aiFeaturesDefined() {
        assertNotNull(FeatureCode.AI_CHAT);
        assertNotNull(FeatureCode.AI_GENERATION);
        assertNotNull(FeatureCode.AI_MCP_TOOL);
    }
}
