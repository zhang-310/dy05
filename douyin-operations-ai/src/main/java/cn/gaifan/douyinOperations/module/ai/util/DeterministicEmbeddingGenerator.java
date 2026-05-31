package cn.gaifan.douyinOperations.module.ai.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * 验收/staging 用确定性嵌入：不依赖 Ollama，保证 pgvector 检索可复现。
 */
public final class DeterministicEmbeddingGenerator {

    public static final int DEFAULT_DIMENSION = 768;

    private DeterministicEmbeddingGenerator() {
    }

    /** 与 seed-gaifan-kb-demo.sql 中 unit 向量一致（768 维，L2=1）。 */
    public static List<Float> unitVector(int dimension) {
        float v = (float) (1.0 / Math.sqrt(Math.max(1, dimension)));
        List<Float> out = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; i++) {
            out.add(v);
        }
        return out;
    }

    public static List<Float> generate(String text, int dimension) {
        if (text == null || text.isBlank()) {
            return unitVector(dimension);
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            List<Float> raw = new ArrayList<>(dimension);
            double sumSq = 0;
            for (int i = 0; i < dimension; i++) {
                int b = hash[i % hash.length] & 0xff;
                float val = (float) ((b / 255.0) * 2.0 - 1.0);
                raw.add(val);
                sumSq += val * val;
            }
            double norm = Math.sqrt(sumSq);
            if (norm < 1e-9) {
                return unitVector(dimension);
            }
            List<Float> normalized = new ArrayList<>(dimension);
            for (float val : raw) {
                normalized.add((float) (val / norm));
            }
            return normalized;
        } catch (Exception e) {
            return unitVector(dimension);
        }
    }
}
