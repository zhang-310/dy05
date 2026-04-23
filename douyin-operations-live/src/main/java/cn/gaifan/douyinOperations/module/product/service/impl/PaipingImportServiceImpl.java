package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.module.product.service.PaipingImportService;
import cn.gaifan.douyinOperations.module.product.vo.ProductSaveVO;
import cn.gaifan.douyinOperations.module.storage.service.BosStorageService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 排品表 Excel 导入实现
 * 格式：A=分类, B=SKU, C=名称, E=价格/规格, F=佣金, G~L=链接, O=控库存, Q=备注, R=效期
 * 支持提取嵌入图片并上传到 BOS，按行关联到产品
 */
@Service
public class PaipingImportServiceImpl implements PaipingImportService {

    private static final Logger log = LoggerFactory.getLogger(PaipingImportServiceImpl.class);
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d+\\.?\\d*)");

    @Resource
    private BosStorageService bosStorageService;

    @Override
    public List<ProductSaveVO> parseFromExcel(InputStream inputStream, Long userId) {
        List<ProductSaveVO> list = new ArrayList<>();
        Map<Integer, ProductSaveVO> rowToVo = new HashMap<>();
        try (Workbook wb = WorkbookFactory.create(inputStream)) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                ProductSaveVO vo = parseRow(row);
                if (vo != null) {
                    list.add(vo);
                    rowToVo.put(i, vo);
                }
            }
            if (userId != null && bosStorageService.isConfigured() && wb instanceof XSSFWorkbook) {
                extractAndUploadImages((XSSFSheet) sheet, rowToVo, userId);
            }
        } catch (Exception e) {
            log.error("解析排品表失败", e);
            throw new RuntimeException("解析排品表失败: " + e.getMessage());
        }
        return list;
    }

    private void extractAndUploadImages(XSSFSheet sheet, Map<Integer, ProductSaveVO> rowToVo, Long userId) {
        XSSFDrawing drawing = sheet.getDrawingPatriarch();
        if (drawing == null) return;
        int uploaded = 0;
        for (org.apache.poi.ss.usermodel.Shape shape : drawing.getShapes()) {
            if (!(shape instanceof XSSFPicture)) continue;
            XSSFPicture pic = (XSSFPicture) shape;
            ClientAnchor anchor = pic.getClientAnchor();
            if (anchor == null) continue;
            int row = anchor.getRow1();
            ProductSaveVO vo = rowToVo.get(row);
            if (vo == null) continue;
            byte[] data = pic.getPictureData().getData();
            if (data == null || data.length == 0) continue;
            String ext = pic.getPictureData().suggestFileExtension();
            if (ext == null || ext.isBlank()) ext = "png";
            String contentType = "image/png";
            if ("jpg".equalsIgnoreCase(ext) || "jpeg".equalsIgnoreCase(ext)) contentType = "image/jpeg";
            String key = userId + "/product/paiping/" + (vo.getSku() != null ? vo.getSku() : "img-" + row) + "." + ext;
            try {
                String url = bosStorageService.uploadBytes(key, data, contentType);
                if (url != null) {
                    vo.setImageUrl(url);
                    uploaded++;
                }
            } catch (Exception e) {
                log.warn("上传排品图片失败 row={} sku={}: {}", row, vo.getSku(), e.getMessage());
            }
        }
        if (uploaded > 0) log.info("排品表导入：已上传 {} 张产品图片", uploaded);
    }

    private ProductSaveVO parseRow(Row row) {
        String sku = getCellStr(row, 1);
        String name = getCellStr(row, 2);
        if (sku == null || sku.isBlank() || !sku.trim().startsWith("BT")) return null;
        if (name == null || name.isBlank()) return null;

        ProductSaveVO vo = new ProductSaveVO();
        vo.setProductName(name.trim());
        vo.setSku(sku.trim());

        Object priceRaw = getCellValue(row, 4);
        BigDecimal price = parsePrice(priceRaw);
        if (price == null) price = BigDecimal.ZERO;
        vo.setPrice(price);

        Object commission = getCellValue(row, 5);
        if (commission != null && !String.valueOf(commission).isBlank()) {
            parseCommission(commission, vo);
        }
        String category = normalizePaipingCategory(getCellStr(row, 0), vo.getProfitMarginPct(), vo.getLossPerUnit());
        vo.setProductCategory(category);
        String stockCtrl = getCellStr(row, 14);
        if (stockCtrl != null && !stockCtrl.isBlank()) {
            vo.setControlStrategy(stockCtrl.trim());
        }
        StringBuilder desc = new StringBuilder();
        if (priceRaw != null) {
            String spec = String.valueOf(priceRaw).replace("\n", " ").trim();
            if (spec.length() > 20) desc.append("规格: ").append(spec).append("\n");
        }
        String remark = getCellStr(row, 16);
        if (remark != null && !remark.isBlank()) {
            desc.append("备注: ").append(remark).append("\n");
        }
        String expiry = getCellStr(row, 17);
        if (expiry != null && !expiry.isBlank()) {
            desc.append("效期: ").append(expiry).append("\n");
        }
        String link1 = getCellStr(row, 6);
        if (link1 != null && link1.contains("http")) desc.append("线上1: ").append(link1.trim()).append("\n");
        String link2 = getCellStr(row, 7);
        if (link2 != null && link2.contains("http")) desc.append("线上2: ").append(link2.trim()).append("\n");
        String link3 = getCellStr(row, 8);
        if (link3 != null && link3.contains("http")) desc.append("线上3: ").append(link3.trim()).append("\n");
        String off1 = getCellStr(row, 9);
        if (off1 != null && off1.contains("http")) desc.append("线下1: ").append(off1.trim()).append("\n");
        String off2 = getCellStr(row, 10);
        if (off2 != null && off2.contains("http")) desc.append("线下2: ").append(off2.trim()).append("\n");
        String off3 = getCellStr(row, 11);
        if (off3 != null && off3.contains("http")) desc.append("线下3: ").append(off3.trim()).append("\n");

        if (desc.length() > 0) {
            vo.setDescription(desc.toString().trim());
        }

        List<String> tags = new ArrayList<>();
        tags.add(category);
        tags.add("护肤品");
        tags.add("直播");
        vo.setTags(String.join(",", tags));

        vo.setStatus(1);
        vo.setFeatured(0);
        vo.setInventory(0L);
        return vo;
    }

    private String getCellStr(Row row, int col) {
        Cell c = row.getCell(col);
        if (c == null) return null;
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(c)) yield null;
                double v = c.getNumericCellValue();
                if (v == (long) v) yield String.valueOf((long) v);
                yield String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(c.getNumericCellValue());
                } catch (Exception e) {
                    yield c.toString();
                }
            }
            default -> c.toString();
        };
    }

    private Object getCellValue(Row row, int col) {
        Cell c = row.getCell(col);
        if (c == null) return null;
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(c)) yield null;
                double v = c.getNumericCellValue();
                if (v == (long) v) yield (long) v;
                yield v;
            }
            case BOOLEAN -> c.getBooleanCellValue();
            case FORMULA -> {
                try {
                    yield c.getNumericCellValue();
                } catch (Exception e) {
                    yield c.toString();
                }
            }
            default -> c.toString();
        };
    }

    /** 严格执行 B/R/F/C/K：B=爆款 R=利润 F/C=福利 K=亏品；其他（面膜、面霜、空等）按利润推断 */
    private String normalizePaipingCategory(String raw, BigDecimal profitMarginPct, BigDecimal lossPerUnit) {
        if (raw != null && !raw.isBlank()) {
            String c = raw.trim().toUpperCase();
            if ("B".equals(c) || "R".equals(c) || "F".equals(c) || "C".equals(c) || "K".equals(c)) {
                return c;
            }
        }
        if (lossPerUnit != null && lossPerUnit.compareTo(BigDecimal.ZERO) > 0) return "K";
        if (profitMarginPct != null && profitMarginPct.compareTo(new BigDecimal("0.30")) >= 0) return "R";
        return "B";
    }

    /** 正数=利润百分比，负数=每单亏多少钱(元)；写入 profitMarginPct 或 lossPerUnit */
    private void parseCommission(Object raw, ProductSaveVO vo) {
        if (raw == null) return;
        try {
            double v = raw instanceof Number ? ((Number) raw).doubleValue() : Double.parseDouble(String.valueOf(raw).trim());
            if (v >= 0) {
                vo.setProfitMarginPct(java.math.BigDecimal.valueOf(v));
                vo.setLossPerUnit(null);
            } else {
                vo.setProfitMarginPct(null);
                vo.setLossPerUnit(java.math.BigDecimal.valueOf(-v));
            }
        } catch (Exception ignored) {
        }
    }

    private BigDecimal parsePrice(Object raw) {
        if (raw == null) return null;
        String s = String.valueOf(raw).replace("\n", " ").replace("\r", " ").trim();
        Matcher m = PRICE_PATTERN.matcher(s);
        if (m.find()) {
            try {
                return new BigDecimal(m.group(1));
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
