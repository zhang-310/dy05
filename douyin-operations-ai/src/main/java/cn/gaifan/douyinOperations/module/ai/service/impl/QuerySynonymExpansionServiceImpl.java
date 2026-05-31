package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.config.SearchSynonymsProperties;
import cn.gaifan.douyinOperations.module.ai.service.QuerySynonymExpansionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class QuerySynonymExpansionServiceImpl implements QuerySynonymExpansionService {

    @Resource
    private SearchSynonymsProperties searchSynonymsProperties;

    @Override
    public List<String> expandQueries(List<String> queries) {
        if (queries == null || queries.isEmpty()) {
            return queries != null ? List.copyOf(queries) : List.of();
        }
        if (searchSynonymsProperties == null || !searchSynonymsProperties.isEnabled()) {
            return List.copyOf(queries);
        }
        List<SearchSynonymsProperties.Group> groups = searchSynonymsProperties.getGroups();
        if (groups == null || groups.isEmpty()) {
            return List.copyOf(queries);
        }
        int maxExtra = Math.max(0, searchSynonymsProperties.getMaxExtraQueries());

        List<String> result = new ArrayList<>(queries);
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String q : queries) {
            if (q != null && !q.isBlank()) {
                seen.add(q);
            }
        }
        int extras = 0;

        for (String q : new ArrayList<>(queries)) {
            if (q == null || q.isBlank() || extras >= maxExtra) {
                continue;
            }
            for (SearchSynonymsProperties.Group g : groups) {
                if (extras >= maxExtra) {
                    break;
                }
                if (g == null || g.getTerms() == null) {
                    continue;
                }
                List<String> terms = new ArrayList<>(g.getTerms().stream().filter(t -> t != null && !t.isBlank()).map(String::trim).toList());
                if (terms.size() < 2) {
                    continue;
                }
                terms.sort(Comparator.comparingInt(String::length).reversed());
                for (String term : terms) {
                    if (extras >= maxExtra || !q.contains(term)) {
                        continue;
                    }
                    for (String alt : terms) {
                        if (extras >= maxExtra || alt.equals(term)) {
                            continue;
                        }
                        String variant = q.replace(term, alt);
                        if (variant.equals(q) || !seen.add(variant)) {
                            continue;
                        }
                        result.add(variant);
                        extras++;
                        if (extras >= maxExtra) {
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }
}
