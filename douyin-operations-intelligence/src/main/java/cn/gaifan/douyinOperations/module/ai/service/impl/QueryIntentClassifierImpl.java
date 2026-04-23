package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.search.QueryIntent;
import cn.gaifan.douyinOperations.module.ai.service.QueryIntentClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * S-4：轻量规则分类（无额外 LLM 调用，成本低）。
 */
@Service
public class QueryIntentClassifierImpl implements QueryIntentClassifier {

    private static final Pattern COMPARE = Pattern.compile(
            ".*(vs\\.?|对比|相较|区别(于|是)?|还是|哪个好|哪一种|二者|两者).*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern DEFINITION = Pattern.compile(
            ".*(是什么|什么叫|什么是|定义|含义|指的是|何[谓是]|meaning\\s+of).*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HOW_TO = Pattern.compile(
            ".*(如何|怎么|怎样|步骤|教程|做法|怎样才能).*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Value("${app.ai.search.intent.enabled:true}")
    private boolean intentEnabled;

    @Override
    public QueryIntent classify(String query) {
        if (!intentEnabled || query == null || query.isBlank()) {
            return QueryIntent.GENERAL;
        }
        String q = query.trim();
        String lower = q.toLowerCase(Locale.ROOT);
        if (COMPARE.matcher(q).matches() || COMPARE.matcher(lower).matches()) {
            return QueryIntent.COMPARE;
        }
        if (DEFINITION.matcher(q).matches() || DEFINITION.matcher(lower).matches()) {
            return QueryIntent.DEFINITION;
        }
        if (HOW_TO.matcher(q).matches() || HOW_TO.matcher(lower).matches()) {
            return QueryIntent.HOW_TO;
        }
        return QueryIntent.GENERAL;
    }
}
