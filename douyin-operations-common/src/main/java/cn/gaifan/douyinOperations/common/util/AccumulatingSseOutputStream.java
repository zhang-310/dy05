package cn.gaifan.douyinOperations.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 包装 OutputStream，在转发 SSE 写入的同时累积 chunk 事件的 content 字段，供流式结束后获取完整内容
 */
public class AccumulatingSseOutputStream extends FilterOutputStream {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final StringBuilder contentBuilder = new StringBuilder();
    private byte[] lineBuffer = new byte[4096];
    private int lineLen = 0;
    private String currentEvent = null;
    private boolean sseErrorSeen;

    public AccumulatingSseOutputStream(OutputStream delegate) {
        super(delegate);
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        appendAndParse((byte) b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        for (int i = 0; i < len; i++) {
            appendAndParse(b[off + i]);
        }
    }

    private void appendAndParse(byte b) {
        if (lineLen >= lineBuffer.length) {
            byte[] next = new byte[lineBuffer.length * 2];
            System.arraycopy(lineBuffer, 0, next, 0, lineLen);
            lineBuffer = next;
        }
        lineBuffer[lineLen++] = b;
        if (b == '\n') {
            flushLine();
        }
    }

    private void flushLine() {
        if (lineLen == 0) return;
        String line = new String(lineBuffer, 0, lineLen, StandardCharsets.UTF_8).trim();
        lineLen = 0;
        if (line.startsWith("event:")) {
            currentEvent = line.substring(6).trim();
        } else if (line.startsWith("data:") && currentEvent != null) {
            String data = line.substring(5).trim();
            if ("error".equals(currentEvent)) {
                sseErrorSeen = true;
            } else if ("chunk".equals(currentEvent)) {
                try {
                    JsonNode node = MAPPER.readTree(data);
                    JsonNode content = node.path("content");
                    if (!content.isMissingNode() && content.isTextual()) {
                        contentBuilder.append(content.asText());
                    }
                } catch (Exception ignored) { /* ignore parse error */ }
            }
            currentEvent = null;
        }
    }

    @Override
    public void close() throws IOException {
        if (lineLen > 0) flushLine();
        super.close();
    }

    /** 获取已累积的 chunk content */
    public String getAccumulatedContent() {
        if (lineLen > 0) flushLine();
        return contentBuilder.toString();
    }

    /** 是否出现过 SSE event:error（模型或链路失败） */
    public boolean isSseErrorSeen() {
        if (lineLen > 0) flushLine();
        return sseErrorSeen;
    }
}
