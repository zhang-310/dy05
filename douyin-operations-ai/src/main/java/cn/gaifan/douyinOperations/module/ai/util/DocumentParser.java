package cn.gaifan.douyinOperations.module.ai.util;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 知识库文档解析：支持 .md / .txt / .doc / .docx / .pdf。
 * 含 magic bytes 校验、加密/扫描件检测、元数据提取。
 */
public final class DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(DocumentParser.class);

    private DocumentParser() {
    }

    /**
     * 根据文件路径解析，提取纯文本（先做 magic bytes 校验）
     */
    public static String parse(Path file) throws IOException {
        validateFileType(file);
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".docx")) {
            return parseDocx(file);
        } else if (name.endsWith(".doc")) {
            return parseDoc(file);
        } else if (name.endsWith(".pdf")) {
            return parsePdf(file);
        } else {
            return Files.readString(file, StandardCharsets.UTF_8);
        }
    }

    /**
     * 从 InputStream 解析（用于上传场景），fileType 为小写扩展名如 "docx","pdf"
     */
    public static String parse(InputStream is, String fileType) throws IOException {
        if (fileType == null) fileType = "";
        String ft = fileType.toLowerCase();
        if (ft.equals("docx")) {
            return parseDocx(is);
        } else if (ft.equals("doc")) {
            return parseDoc(is);
        } else if (ft.equals("pdf")) {
            return parsePdf(is);
        } else {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 解析结果：文本 + 元数据（标题、作者、关键词、页数等）
     */
    public static ParseResult parseWithMetadata(Path file) throws IOException {
        validateFileType(file);
        String name = file.getFileName().toString().toLowerCase();
        String text;
        Map<String, String> metadata = new HashMap<>();

        if (name.endsWith(".docx")) {
            text = parseDocxWithMetadata(file, metadata);
        } else if (name.endsWith(".doc")) {
            text = parseDocWithMetadata(file, metadata);
        } else if (name.endsWith(".pdf")) {
            text = parsePdfWithMetadata(file, metadata);
        } else {
            text = Files.readString(file, StandardCharsets.UTF_8);
        }
        return new ParseResult(text, metadata);
    }

    /**
     * 根据扩展名检测文件类型字符串（md/docx/doc/pdf/text）
     */
    public static String detectFileType(String filename) {
        if (filename == null) return "text";
        String name = filename.toLowerCase();
        if (name.endsWith(".md")) return "md";
        if (name.endsWith(".docx")) return "docx";
        if (name.endsWith(".doc")) return "doc";
        if (name.endsWith(".pdf")) return "pdf";
        return "text";
    }

    // ---------- DOCX ----------

    private static String parseDocx(Path file) throws IOException {
        try (InputStream is = Files.newInputStream(file)) {
            return parseDocx(is);
        }
    }

    private static String parseDocx(InputStream is) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(is)) {
            return extractDocxText(doc);
        } catch (EncryptedDocumentException e) {
            throw new BusinessException(ErrorCode.AI_DOC_ENCRYPTED,
                "文件已加密，请先解除密码保护后再导入");
        }
    }

    private static String parseDocxWithMetadata(Path file, Map<String, String> metadata) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(file))) {
            var core = doc.getProperties().getCoreProperties();
            if (core != null) {
                if (core.getTitle() != null) metadata.put("title", core.getTitle());
                if (core.getCreator() != null) metadata.put("author", core.getCreator());
                if (core.getDescription() != null) metadata.put("description", core.getDescription());
                if (core.getKeywords() != null) metadata.put("keywords", core.getKeywords());
            }
            var ext = doc.getProperties().getExtendedProperties();
            if (ext != null && ext.getUnderlyingProperties() != null) {
                metadata.put("pageCount", String.valueOf(ext.getUnderlyingProperties().getPages()));
            }
            return extractDocxText(doc);
        } catch (EncryptedDocumentException e) {
            throw new BusinessException(ErrorCode.AI_DOC_ENCRYPTED,
                "文件已加密，请先解除密码保护后再导入");
        }
    }

    private static String extractDocxText(XWPFDocument doc) {
        StringBuilder sb = new StringBuilder();
        for (XWPFParagraph para : doc.getParagraphs()) {
            String t = para.getText();
            if (t != null && !t.isBlank()) sb.append(t.trim()).append("\n");
        }
        for (XWPFTable table : doc.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    String t = cell.getText();
                    if (t != null && !t.isBlank()) sb.append(t.trim()).append("\t");
                }
                sb.append("\n");
            }
        }
        return sb.toString().trim();
    }

    // ---------- DOC (旧版二进制) ----------

    private static String parseDoc(Path file) throws IOException {
        try (InputStream is = Files.newInputStream(file)) {
            return parseDoc(is);
        }
    }

    private static String parseDoc(InputStream is) throws IOException {
        try (HWPFDocument doc = new HWPFDocument(is)) {
            return doc.getDocumentText();
        } catch (EncryptedDocumentException e) {
            throw new BusinessException(ErrorCode.AI_DOC_ENCRYPTED,
                "文件已加密，请先解除密码保护后再导入");
        }
    }

    private static String parseDocWithMetadata(Path file, Map<String, String> metadata) throws IOException {
        try (HWPFDocument doc = new HWPFDocument(Files.newInputStream(file))) {
            SummaryInformation si = doc.getSummaryInformation();
            if (si != null) {
                if (si.getTitle() != null) metadata.put("title", si.getTitle());
                if (si.getAuthor() != null) metadata.put("author", si.getAuthor());
                if (si.getKeywords() != null) metadata.put("keywords", si.getKeywords());
                metadata.put("pageCount", String.valueOf(si.getPageCount()));
            }
            return doc.getDocumentText();
        } catch (EncryptedDocumentException e) {
            throw new BusinessException(ErrorCode.AI_DOC_ENCRYPTED,
                "文件已加密，请先解除密码保护后再导入");
        }
    }

    // ---------- PDF ----------

    private static String parsePdf(Path file) throws IOException {
        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBufferedFile(file.toFile()))) {
            return extractPdfText(doc, file.getFileName().toString());
        } catch (InvalidPasswordException e) {
            throw new BusinessException(ErrorCode.AI_DOC_ENCRYPTED,
                "PDF 文件需要密码，请先解除密码保护后再导入");
        }
    }

    private static String parsePdf(InputStream is) throws IOException {
        byte[] bytes = is.readAllBytes();
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            if (doc.isEncrypted()) {
                throw new BusinessException(ErrorCode.AI_DOC_ENCRYPTED,
                    "PDF 文件已加密，请先解除密码保护后再导入");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc).trim();
        } catch (InvalidPasswordException e) {
            throw new BusinessException(ErrorCode.AI_DOC_ENCRYPTED,
                "PDF 文件需要密码，请先解除密码保护后再导入");
        }
    }

    private static String parsePdfWithMetadata(Path file, Map<String, String> metadata) throws IOException {
        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBufferedFile(file.toFile()))) {
            if (doc.isEncrypted()) {
                throw new BusinessException(ErrorCode.AI_DOC_ENCRYPTED,
                    "PDF 文件已加密，请先解除密码保护后再导入");
            }
            PDDocumentInformation info = doc.getDocumentInformation();
            if (info != null) {
                if (info.getTitle() != null) metadata.put("title", info.getTitle());
                if (info.getAuthor() != null) metadata.put("author", info.getAuthor());
                if (info.getKeywords() != null) metadata.put("keywords", info.getKeywords());
                if (info.getSubject() != null) metadata.put("subject", info.getSubject());
                if (info.getCreator() != null) metadata.put("creator", info.getCreator());
            }
            metadata.put("pageCount", String.valueOf(doc.getNumberOfPages()));
            return extractPdfText(doc, file.getFileName().toString());
        } catch (InvalidPasswordException e) {
            throw new BusinessException(ErrorCode.AI_DOC_ENCRYPTED,
                "PDF 文件需要密码，请先解除密码保护后再导入");
        }
    }

    private static String extractPdfText(PDDocument doc, String fileName) throws IOException {
        if (doc.isEncrypted()) {
            throw new BusinessException(ErrorCode.AI_DOC_ENCRYPTED,
                "PDF 文件已加密，请先解除密码保护后再导入: " + fileName);
        }
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(doc).trim();
        int pages = doc.getNumberOfPages();

        if (text.length() < 20 && pages > 0) {
            log.warn("PDF 疑似扫描件（图片型），提取文字仅 {} 字符，共 {} 页: {}",
                text.length(), pages, fileName);
            throw new BusinessException(ErrorCode.AI_DOC_SCAN_ONLY,
                "PDF 疑似扫描件（图片型），无法提取文字。建议先用 OCR 工具转换为文字版: " + fileName);
        }
        if (pages > 1 && text.length() < pages * 50) {
            log.warn("PDF 文字密度过低（平均每页 {} 字符），可能含扫描页: {}",
                text.length() / pages, fileName);
        }
        return text;
    }

    /**
     * Magic bytes 校验，防止伪装文件（如 .exe 重命名为 .pdf）
     */
    public static void validateFileType(Path file) throws IOException {
        String name = file.getFileName().toString().toLowerCase();
        byte[] header = new byte[8];
        try (InputStream is = Files.newInputStream(file)) {
            int read = is.read(header);
            if (read < 4) {
                throw new BusinessException(ErrorCode.AI_DOC_FAKE_TYPE, "文件过小或为空: " + name);
            }
        }

        if (name.endsWith(".pdf")) {
            if (header[0] != 0x25 || header[1] != 0x50 || header[2] != 0x44 || header[3] != 0x46) {
                throw new BusinessException(ErrorCode.AI_DOC_FAKE_TYPE,
                    "文件扩展名为 .pdf 但内容不是有效的 PDF 格式: " + name);
            }
        } else if (name.endsWith(".doc") || name.endsWith(".docx")) {
            boolean isOle2 = (header[0] & 0xFF) == 0xD0 && (header[1] & 0xFF) == 0xCF
                && (header[2] & 0xFF) == 0x11 && (header[3] & 0xFF) == 0xE0;
            boolean isZip = header[0] == 0x50 && header[1] == 0x4B
                && header[2] == 0x03 && header[3] == 0x04;
            if (!isOle2 && !isZip) {
                throw new BusinessException(ErrorCode.AI_DOC_FAKE_TYPE,
                    "文件扩展名与内容格式不符，不是有效的 Word 文档: " + name);
            }
        }
    }

    public record ParseResult(String text, Map<String, String> metadata) {
        public ParseResult(String text, Map<String, String> metadata) {
            this.text = text;
            this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        }
    }
}
