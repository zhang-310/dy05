package cn.gaifan.douyinOperations.module.shortvideo.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * 感知哈希 dHash（8×8=64bit → 16 位十六进制），用于图片近似去重（A-6）
 */
public final class ImageDHashUtil {

    private ImageDHashUtil() {
    }

    public static String dHashHex(BufferedImage src) {
        if (src == null) {
            return null;
        }
        BufferedImage small = toGray9x8(src);
        int[][] lum = new int[8][9];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 9; x++) {
                int rgb = small.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                lum[y][x] = (r * 30 + g * 59 + b * 11) / 100;
            }
        }
        long bits = 0;
        int bitPos = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (lum[y][x] > lum[y][x + 1]) {
                    bits |= (1L << bitPos);
                }
                bitPos++;
            }
        }
        return String.format("%016x", bits);
    }

    public static int hammingHex64(String a, String b) {
        if (a == null || b == null || a.length() != 16 || b.length() != 16) {
            return 999;
        }
        try {
            long la = Long.parseUnsignedLong(a, 16);
            long lb = Long.parseUnsignedLong(b, 16);
            return Long.bitCount(la ^ lb);
        } catch (Exception e) {
            return 999;
        }
    }

    private static BufferedImage toGray9x8(BufferedImage src) {
        BufferedImage out = new BufferedImage(9, 8, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        Image scaled = src.getScaledInstance(9, 8, Image.SCALE_SMOOTH);
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return out;
    }
}
