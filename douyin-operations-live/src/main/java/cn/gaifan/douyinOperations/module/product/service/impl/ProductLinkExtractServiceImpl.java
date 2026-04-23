package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.product.service.PlaywrightHtmlFetcher;
import cn.gaifan.douyinOperations.module.product.service.ProductLinkExtractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从商品链接抓取页面 meta、文本、产品图片 + AI 提炼卖点。
 * 提炼卖点基于：页面文本（标题、描述、body 文本、内嵌 JSON）和产品图片 Vision 识别。
 * 可选：app.product.use-playwright=true 启用无头浏览器渲染 SPA 页面。
 */
@Service
public class ProductLinkExtractServiceImpl implements ProductLinkExtractService {

    private static final Logger log = LoggerFactory.getLogger(ProductLinkExtractServiceImpl.class);

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int FETCH_TIMEOUT_MS = 10_000;
    private static final int MAX_HTML_LEN = 100_000;

    // meta property="og:xxx" content="..." 或 content="..." property="og:xxx"
    private static final Pattern OG_TITLE = Pattern.compile("<meta[^>]+property=\"og:title\"[^>]+content=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_TITLE_ALT = Pattern.compile("<meta[^>]+content=\"([^\"]+)\"[^>]+property=\"og:title\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_IMAGE = Pattern.compile("<meta[^>]+property=\"og:image\"[^>]+content=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_IMAGE_ALT = Pattern.compile("<meta[^>]+content=\"([^\"]+)\"[^>]+property=\"og:image\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_DESC = Pattern.compile("<meta[^>]+property=\"og:description\"[^>]+content=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_DESC_ALT = Pattern.compile("<meta[^>]+content=\"([^\"]+)\"[^>]+property=\"og:description\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern META_DESC = Pattern.compile("<meta[^>]+name=\"description\"[^>]+content=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern META_DESC_ALT = Pattern.compile("<meta[^>]+content=\"([^\"]+)\"[^>]+name=\"description\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE_TAG = Pattern.compile("<title[^>]*>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#x([0-9a-fA-F]+);|&#(\\d+);");
    // JSON-LD 商品结构化数据
    private static final Pattern JSON_LD = Pattern.compile("<script[^>]*type=\"application/ld\\+json\"[^>]*>([\\s\\S]*?)</script>", Pattern.CASE_INSENSITIVE);
    // SPA 内嵌数据：__NEXT_DATA__ 或 script 中的 JSON
    private static final Pattern NEXT_DATA = Pattern.compile("<script[^>]*id=\"__NEXT_DATA__\"[^>]*type=\"application/json\"[^>]*>([\\s\\S]*?)</script>", Pattern.CASE_INSENSITIVE);
    // 商品相关 key 在 HTML 中的模式（用于从任意 script/JSON 中提取）
    private static final Pattern EMBEDDED_TITLE = Pattern.compile("[\"'](?:title|name|productName|product_name)[\"']\\s*:\\s*[\"']([^\"']{1,200})[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMBEDDED_DESC = Pattern.compile("[\"'](?:description|desc|subTitle|sub_title)[\"']\\s*:\\s*[\"']([^\"']{1,500})[\"']", Pattern.CASE_INSENSITIVE);
    // body 内可见文本（去除 script/style）
    private static final Pattern BODY = Pattern.compile("<body[^>]*>([\\s\\S]*?)</body>", Pattern.CASE_INSENSITIVE);
    private static final int MAX_BODY_TEXT = 3000;
    private static final int MAX_LLM_RAW = 8000;
    private static final int MIN_MEANINGFUL_LEN = 15;
    /** 仅含此类词时视为无效，不调用 LLM */
    private static final Set<String> NOISE_WORDS = Set.of("prefetch", "preload", "loading", "undefined", "null", "script", "style");

    private static final String MSG_INSUFFICIENT = """
        抱歉，当前页面解析到的商品信息不足（如仅有 "Prefetch" 等占位内容），无法提炼有效的直播带货卖点。

        请补充以下信息后重新提交：
        1. **完整商品标题**
        2. **商品详情描述**（功能、材质、规格等）
        3. **商品价格/优惠信息**
        4. **产品主图或详情图**
        5. **商品链接**（可提供其他可解析的链接）

        信息越完整，卖点提炼越精准、越适合直播口播。
        """.trim();

    @Resource
    private AiModelRepository aiModelRepository;
    @Resource
    private AiTaskModelConfigRepository taskModelConfigRepository;
    @Resource
    private LlmClient llmClient;

    /** 提炼卖点使用文案处理任务配置（copy_processing），与文案库 AI 写文案共用 Claude 等模型 */
    private static final String TASK_CODE = "copy_processing";
    @Resource
    private DyProductRepository dyProductRepository;
    @Autowired(required = false)
    private PlaywrightHtmlFetcher playwrightHtmlFetcher;

    private final RestTemplate restTemplate = createRestTemplate();

    private static RestTemplate createRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory f =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        f.setConnectTimeout(FETCH_TIMEOUT_MS);
        f.setReadTimeout(FETCH_TIMEOUT_MS);
        return new RestTemplate(f);
    }

    @Override
    public Map<String, String> extractFromLink(String productLink) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("productName", "");
        result.put("imageUrl", "");
        result.put("description", "");
        result.put("aiSellingPoints", "");

        if (productLink == null || productLink.isBlank()) {
            return result;
        }

        String link = productLink.trim();
        if (!link.startsWith("http://") && !link.startsWith("https://")) {
            link = "https://" + link;
        }

        // 1. 抓取页面
        String html = fetchHtml(link);
        if (html == null || html.isBlank()) {
            log.warn("抓取链接失败或为空: {}", link);
            return result;
        }

        if (html.length() > MAX_HTML_LEN) {
            html = html.substring(0, MAX_HTML_LEN);
        }

        // 2. 解析 meta（title 优先 og:title，否则 <title>）
        String title = extractMeta(html, OG_TITLE, OG_TITLE_ALT);
        if (title == null || title.isBlank()) {
            Matcher tm = TITLE_TAG.matcher(html);
            if (tm.find()) title = decodeHtml(tm.group(1));
        }
        String imageUrl = extractMeta(html, OG_IMAGE, OG_IMAGE_ALT);
        String desc = extractMeta(html, OG_DESC, OG_DESC_ALT);
        if (desc == null || desc.isBlank()) {
            desc = extractMeta(html, META_DESC, META_DESC_ALT);
        }

        // 相对图片 URL 转绝对
        if (imageUrl != null && !imageUrl.isBlank() && !imageUrl.startsWith("http")) {
            try {
                URI base = URI.create(link);
                imageUrl = base.resolve(imageUrl).toString();
            } catch (Exception ignored) {
            }
        }

        result.put("productName", title != null ? title : "");
        result.put("imageUrl", imageUrl != null ? imageUrl : "");
        result.put("description", desc != null ? desc : "");

        // 2b. 补充提取：页面文本（JSON-LD、内嵌 JSON、body 文本）和产品图片
        String bodyText = extractBodyText(html);
        String embeddedText = extractEmbeddedProductInfo(html);
        if (title == null || title.isBlank()) {
            String fromEmbed = extractTitleFromEmbedded(embeddedText);
            if (fromEmbed != null && !fromEmbed.isBlank()) {
                title = fromEmbed;
                result.put("productName", title);
            }
        }

        // 3. AI 提炼卖点（基于页面文本 + 产品图片 Vision 识别）
        String raw = combineForLlm(title, desc, imageUrl, bodyText, embeddedText);
        boolean hasImage = imageUrl != null && !imageUrl.isBlank();
        boolean hasMeaningfulText = !raw.isBlank() && isMeaningfulForLlm(raw);
        if (hasMeaningfulText || hasImage) {
            List<String> imageUrls = hasImage ? List.of(imageUrl) : List.of();
            String aiPoints = extractSellingPointsWithLlm(raw, imageUrls);
            result.put("aiSellingPoints", aiPoints != null ? aiPoints : "");
            log.info("提炼卖点: title={}, descLen={}, bodyLen={}, imageUrl={}, vision={}, aiPointsLen={}",
                    title, desc != null ? desc.length() : 0, bodyText != null ? bodyText.length() : 0,
                    hasImage, hasImage, aiPoints != null ? aiPoints.length() : 0);
        } else {
            result.put("aiSellingPoints", MSG_INSUFFICIENT);
            log.warn("提炼卖点跳过: 无有效文本且无产品图片。link={}", link);
        }

        return result;
    }

    private String fetchHtml(String url) {
        if (playwrightHtmlFetcher != null && playwrightHtmlFetcher.isAvailable()) {
            String html = playwrightHtmlFetcher.fetch(url);
            if (html != null && !html.isBlank()) {
                log.debug("Playwright 渲染 SPA 成功: {}", url);
                return html;
            }
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            headers.setAccept(List.of(MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML));
            ResponseEntity<String> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return resp.getBody();
            }
        } catch (Exception e) {
            log.warn("抓取链接失败: {} - {}", url, e.getMessage());
        }
        return null;
    }

    private String extractMeta(String html, Pattern p1, Pattern p2) {
        Matcher m = p1.matcher(html);
        if (m.find()) return decodeHtml(m.group(1));
        m = p2.matcher(html);
        if (m.find()) return decodeHtml(m.group(1));
        return null;
    }

    /** 提取 body 内可见文本（去除 script/style 标签） */
    private String extractBodyText(String html) {
        Matcher m = BODY.matcher(html);
        if (!m.find()) return null;
        String body = m.group(1);
        body = body.replaceAll("(?i)<script[^>]*>[\\s\\S]*?</script>", " ");
        body = body.replaceAll("(?i)<style[^>]*>[\\s\\S]*?</style>", " ");
        body = body.replaceAll("<[^>]+>", " ");
        body = body.replaceAll("\\s+", " ").trim();
        return body.length() > 0 ? decodeHtml(body) : null;
    }

    /** 提取内嵌 JSON 中的商品信息（JSON-LD、__NEXT_DATA__、script 内 title/description 等） */
    private String extractEmbeddedProductInfo(String html) {
        StringBuilder sb = new StringBuilder();
        // JSON-LD
        Matcher jld = JSON_LD.matcher(html);
        while (jld.find()) {
            String json = jld.group(1).trim();
            String extracted = extractProductFromJsonLd(json);
            if (extracted != null && !extracted.isBlank()) sb.append(extracted).append(" ");
        }
        // __NEXT_DATA__
        Matcher nd = NEXT_DATA.matcher(html);
        if (nd.find()) {
            String extracted = extractProductFromGenericJson(nd.group(1).trim());
            if (extracted != null && !extracted.isBlank()) sb.append(extracted).append(" ");
        }
        // 从整页 HTML 中匹配商品相关 key（适配 __INITIAL_STATE__ 等 SPA 内嵌数据）
        String fromHtml = extractFromHtmlByPatterns(html);
        if (fromHtml != null && !fromHtml.isBlank()) sb.append(fromHtml);
        return sb.length() > 0 ? sb.toString().trim() : null;
    }

    private String extractFromHtmlByPatterns(String html) {
        StringBuilder sb = new StringBuilder();
        Matcher m1 = EMBEDDED_TITLE.matcher(html);
        if (m1.find()) sb.append("标题:").append(decodeHtml(m1.group(1))).append(" ");
        Matcher m2 = EMBEDDED_DESC.matcher(html);
        if (m2.find()) sb.append("描述:").append(decodeHtml(m2.group(1)));
        return sb.length() > 0 ? sb.toString().trim() : null;
    }

    private String extractProductFromJsonLd(String json) {
        try {
            if (json.contains("\"@type\"") && (json.contains("Product") || json.contains("product"))) {
                String name = extractJsonString(json, "name");
                String desc = extractJsonString(json, "description");
                String img = extractJsonString(json, "image");
                String imgs = extractJsonArrayFirst(json, "image");
                if (img == null && imgs != null) img = imgs;
                StringBuilder sb = new StringBuilder();
                if (name != null && !name.isBlank()) sb.append("名称:").append(name).append(" ");
                if (desc != null && !desc.isBlank()) sb.append("描述:").append(desc).append(" ");
                if (img != null && !img.isBlank()) sb.append("图片:").append(img);
                return sb.length() > 0 ? sb.toString().trim() : null;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String extractJsonArrayFirst(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1).trim() : null;
    }

    private String extractProductFromGenericJson(String json) {
        try {
            StringBuilder sb = new StringBuilder();
            String title = extractJsonString(json, "title");
            if (title == null) title = extractJsonString(json, "name");
            if (title == null) title = extractJsonString(json, "productName");
            String desc = extractJsonString(json, "description");
            if (desc == null) desc = extractJsonString(json, "desc");
            String subTitle = extractJsonString(json, "subTitle");
            String img = extractJsonString(json, "image");
            if (img == null) img = extractJsonArrayFirst(json, "images");
            if (title != null && !title.isBlank()) sb.append("标题:").append(title).append(" ");
            if (subTitle != null && !subTitle.isBlank()) sb.append("副标题:").append(subTitle).append(" ");
            if (desc != null && !desc.isBlank()) sb.append("描述:").append(desc).append(" ");
            if (img != null && !img.isBlank()) sb.append("图片:").append(img);
            return sb.length() > 0 ? sb.toString().trim() : null;
        } catch (Exception ignored) {
        }
        return null;
    }

    private String extractTitleFromEmbedded(String embeddedText) {
        if (embeddedText == null || embeddedText.isBlank()) return null;
        Matcher m = Pattern.compile("(?:名称|标题)[:：]\\s*([^\\s]+(?:\\s+[^\\s]+){0,10})").matcher(embeddedText);
        if (m.find()) return decodeHtml(m.group(1).trim());
        return null;
    }

    private String extractJsonString(String json, String key) {
        // 匹配 "key":"value" 或 "key": "value"，value 可含转义
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(json);
        if (m.find()) return m.group(1).replace("\\\"", "\"").trim();
        return null;
    }

    private String decodeHtml(String s) {
        if (s == null) return null;
        s = s.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
        // 数字实体 &#x8D27; (hex) 或 &#36239; (decimal)
        Matcher m = NUMERIC_ENTITY.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            int code;
            if (m.group(1) != null) {
                code = Integer.parseInt(m.group(1), 16);
            } else {
                code = Integer.parseInt(m.group(2), 10);
            }
            m.appendReplacement(sb, Character.toString((char) code));
        }
        m.appendTail(sb);
        return sb.toString().trim();
    }

    private String combineForLlm(String title, String desc, String imageUrl, String bodyText, String embeddedText) {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isBlank()) sb.append("标题：").append(title).append("\n");
        if (desc != null && !desc.isBlank()) sb.append("描述：").append(desc).append("\n");
        if (imageUrl != null && !imageUrl.isBlank()) sb.append("产品主图：").append(imageUrl).append("\n");
        if (bodyText != null && !bodyText.isBlank()) sb.append("页面文本：").append(truncate(bodyText, MAX_BODY_TEXT)).append("\n");
        if (embeddedText != null && !embeddedText.isBlank()) sb.append("商品数据：").append(truncate(embeddedText, 2000));
        String raw = sb.toString().trim();
        return raw.length() > MAX_LLM_RAW ? raw.substring(0, MAX_LLM_RAW) : raw;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...";
    }

    /** 判断 raw 是否足够有意义，可调用 LLM。过滤仅含 Prefetch、Loading 等占位词的情况 */
    private boolean isMeaningfulForLlm(String raw) {
        if (raw == null || raw.length() < MIN_MEANINGFUL_LEN) return false;
        String noUrl = raw.replaceAll("https?://[^\\s]+", " ");
        String lower = noUrl.toLowerCase().replaceAll("\\s+", " ").trim();
        String content = lower.replaceAll("(?:标题|描述|产品主图|页面文本|商品数据)[：:]\\s*", " ");
        content = content.replaceAll("\\s+", " ").trim();
        String[] tokens = content.split("\\s+");
        int meaningful = 0;
        for (String t : tokens) {
            String t2 = t.replaceAll("[^a-zA-Z\\u4e00-\\u9fa5]", "");
            if (t2.length() < 2) continue;
            if (NOISE_WORDS.contains(t2)) continue;
            meaningful++;
        }
        return meaningful >= 1;
    }

    private String extractSellingPointsWithLlm(String rawContent, List<String> imageUrls) {
        List<AiModel> models = resolveModels();
        if (models == null || models.isEmpty()) {
            log.warn("无可用 AI 模型，跳过卖点提炼");
            return null;
        }

        String system = "你是抖音直播带货专家。根据商品链接页面的文本（标题、描述、页面内容）和产品图片，提炼 3-8 条核心卖点，每条一行，简洁有力，适合直播口播。只输出卖点列表，不要其他说明。";
        String prompt = rawContent != null && !rawContent.isBlank()
                ? "请从以下商品信息中提炼卖点：\n\n" + rawContent
                : "请根据下方产品图片，提炼 3-8 条直播带货核心卖点。";

        log.info("提炼卖点调用 LLM: 模型数={}, rawLen={}, imageCount={}", models.size(), rawContent != null ? rawContent.length() : 0, imageUrls != null ? imageUrls.size() : 0);
        LlmClient.LlmResponse resp = (imageUrls != null && !imageUrls.isEmpty())
                ? llmClient.chatWithImageFallback(models, system, prompt, imageUrls)
                : llmClient.chatWithFallback(models, system, prompt);
        if (!resp.success() || resp.content() == null) {
            log.warn("AI 提炼卖点失败: {}", resp.errorMsg());
            return null;
        }
        String content = resp.content().trim();
        log.info("提炼卖点成功: 返回 {} 字", content.length());

        return content;
    }

    /** 解析模型：优先 copy_processing 任务配置（Claude 等），无配置时回退到任意可用模型 */
    private List<AiModel> resolveModels() {
        var config = taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted(TASK_CODE, 1, 0);
        if (config.isPresent()) {
            AiTaskModelConfig tc = config.get();
            List<AiModel> result = new ArrayList<>();
            for (Long modelId : Arrays.asList(tc.getPrimaryModelId(), tc.getFallbackModelId(), tc.getFallback2ModelId())) {
                if (modelId == null) continue;
                aiModelRepository.findById(modelId).filter(m -> m.getStatus() == 1 && m.getDeleted() == 0)
                        .ifPresent(result::add);
            }
            if (!result.isEmpty()) {
                log.info("提炼卖点使用 copy_processing 配置: {}", result.stream().map(m -> m.getModelProvider() + "/" + m.getModelVersion()).toList());
                return result;
            }
        }
        List<AiModel> fallback = aiModelRepository.findByStatusAndDeleted(1, 0).stream().limit(3).toList();
        log.warn("提炼卖点 copy_processing 无有效配置，回退到任意模型: {}", fallback.stream().map(m -> m.getModelProvider() + "/" + m.getModelVersion()).toList());
        return fallback;
    }

    @Override
    @Async
    public void extractAndSaveAsync(Long productId) {
        if (productId == null) return;
        DyProduct product = dyProductRepository.findByIdAndDeleted(productId, 0).orElse(null);
        if (product == null || product.getProductLink() == null || product.getProductLink().isBlank()) {
            return;
        }
        try {
            Map<String, String> extracted = extractFromLink(product.getProductLink());
            boolean changed = false;
            if (product.getProductName() == null || product.getProductName().isBlank()) {
                String name = extracted.get("productName");
                if (name != null && !name.isBlank()) {
                    product.setProductName(name);
                    changed = true;
                }
            }
            if (product.getImageUrl() == null || product.getImageUrl().isBlank()) {
                String img = extracted.get("imageUrl");
                if (img != null && !img.isBlank()) {
                    product.setImageUrl(img);
                    changed = true;
                }
            }
            if (product.getDescription() == null || product.getDescription().isBlank()) {
                String desc = extracted.get("description");
                if (desc != null && !desc.isBlank()) {
                    product.setDescription(desc);
                    changed = true;
                }
            }
            if (product.getAiSellingPoints() == null || product.getAiSellingPoints().isBlank()) {
                String points = extracted.get("aiSellingPoints");
                if (points != null && !points.isBlank()) {
                    product.setAiSellingPoints(points);
                    changed = true;
                }
            }
            if (changed) {
                dyProductRepository.save(product);
                log.info("商品 {} 异步提取完成", productId);
            }
        } catch (Exception e) {
            log.warn("商品 {} 异步提取失败: {}", productId, e.getMessage());
        }
    }
}
