package cn.gaifan.douyinOperations.module.search.service.impl;

import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.entity.DyProductScript;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.product.repository.DyProductScriptRepository;
import cn.gaifan.douyinOperations.module.search.service.GlobalSearchService;
import cn.gaifan.douyinOperations.module.search.vo.GlobalSearchHitVO;
import cn.gaifan.douyinOperations.module.search.vo.GlobalSearchRequestVO;
import cn.gaifan.douyinOperations.module.search.vo.GlobalSearchResponseVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvVideoRepository;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class GlobalSearchServiceImpl implements GlobalSearchService {

    private static final int PER_TYPE = 6;

    @Resource
    private LiveSessionRepository liveSessionRepository;
    @Resource
    private DyProductRepository dyProductRepository;
    @Resource
    private DyProductScriptRepository dyProductScriptRepository;
    @Resource
    private SvVideoRepository svVideoRepository;

    @Override
    public GlobalSearchResponseVO search(GlobalSearchRequestVO request, List<Long> visibleUserIds) {
        long t0 = System.currentTimeMillis();
        String kw = request.getQ().trim();
        int cap = request.getLimit() != null ? request.getLimit() : 24;

        if (visibleUserIds != null && visibleUserIds.isEmpty()) {
            return GlobalSearchResponseVO.builder().hits(List.of()).tookMs(System.currentTimeMillis() - t0).build();
        }

        List<GlobalSearchHitVO> merged = new ArrayList<>();
        Specification<LiveSession> liveSpec = liveSpec(kw, visibleUserIds);
        liveSessionRepository.findAll(liveSpec, PageRequest.of(0, PER_TYPE))
                .forEach(s -> merged.add(GlobalSearchHitVO.builder()
                        .kind("LIVE_SESSION")
                        .id(s.getId())
                        .title(s.getLiveTitle())
                        .subtitle("直播场次 · 状态 " + s.getStatus())
                        .path("live/sessions/" + s.getId())
                        .build()));

        dyProductRepository.findAll(productSpec(kw, visibleUserIds), PageRequest.of(0, PER_TYPE))
                .forEach(p -> merged.add(GlobalSearchHitVO.builder()
                        .kind("PRODUCT")
                        .id(p.getId())
                        .title(p.getProductName())
                        .subtitle("商品 · " + (p.getProductCategory() != null ? p.getProductCategory() : "未分类"))
                        .path("product")
                        .build()));

        dyProductScriptRepository.findAll(scriptSpec(kw, visibleUserIds), PageRequest.of(0, PER_TYPE))
                .forEach(sc -> merged.add(GlobalSearchHitVO.builder()
                        .kind("SCRIPT")
                        .id(sc.getId())
                        .title(trimScript(sc.getScriptContent()))
                        .subtitle("话术 · " + sc.getScriptType() + (sc.getStyle() != null ? " · " + sc.getStyle() : ""))
                        .path(sc.getProductId() != null ? "product" : "product")
                        .build()));

        svVideoRepository.findAll(videoSpec(kw, visibleUserIds), PageRequest.of(0, PER_TYPE))
                .forEach(v -> merged.add(GlobalSearchHitVO.builder()
                        .kind("SHORT_VIDEO")
                        .id(v.getId())
                        .title(v.getTitle() != null ? v.getTitle() : "(无标题)")
                        .subtitle("短视频")
                        .path("shortvideo/videos")
                        .build()));

        // 去重（同 kind+id）并截断
        Set<String> seen = new LinkedHashSet<>();
        List<GlobalSearchHitVO> out = new ArrayList<>();
        for (GlobalSearchHitVO h : merged) {
            String key = h.getKind() + ":" + h.getId();
            if (seen.add(key)) {
                out.add(h);
                if (out.size() >= cap) {
                    break;
                }
            }
        }

        return GlobalSearchResponseVO.builder()
                .hits(out)
                .tookMs(System.currentTimeMillis() - t0)
                .build();
    }

    private static String trimScript(String content) {
        if (content == null) {
            return "";
        }
        String t = content.replace('\n', ' ').trim();
        return t.length() > 80 ? t.substring(0, 80) + "…" : t;
    }

    private static String likePattern(String kw) {
        String esc = kw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + esc.toLowerCase(Locale.ROOT) + "%";
    }

    private Specification<LiveSession> liveSpec(String kw, List<Long> vis) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.like(cb.lower(root.get("liveTitle")), likePattern(kw), '\\'));
            if (vis != null) {
                ps.add(root.get("userId").in(vis));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private Specification<DyProduct> productSpec(String kw, List<Long> vis) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.or(
                    cb.like(cb.lower(root.get("productName")), likePattern(kw), '\\'),
                    cb.like(cb.lower(root.get("productCategory")), likePattern(kw), '\\')
            ));
            if (vis != null) {
                ps.add(root.get("userId").in(vis));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private Specification<DyProductScript> scriptSpec(String kw, List<Long> vis) {
        return (root, query, cb) -> {
            Predicate text = cb.like(cb.lower(root.get("scriptContent")), likePattern(kw), '\\');
            if (vis == null) {
                return text;
            }
            Subquery<Long> sq = query.subquery(Long.class);
            Root<DyProduct> dp = sq.from(DyProduct.class);
            sq.select(dp.get("id")).where(cb.equal(dp.get("deleted"), 0), dp.get("userId").in(vis));
            Predicate byCreator = root.get("createdBy").in(vis);
            Predicate byProduct = cb.and(cb.isNotNull(root.get("productId")), root.get("productId").in(sq));
            return cb.and(text, cb.or(byCreator, byProduct));
        };
    }

    private Specification<SvVideo> videoSpec(String kw, List<Long> vis) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.or(
                    cb.like(cb.lower(root.get("title")), likePattern(kw), '\\'),
                    cb.like(cb.lower(root.get("description")), likePattern(kw), '\\')
            ));
            if (vis != null) {
                ps.add(root.get("ownerId").in(vis));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
