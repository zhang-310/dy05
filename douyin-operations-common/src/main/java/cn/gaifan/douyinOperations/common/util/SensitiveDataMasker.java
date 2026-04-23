package cn.gaifan.douyinOperations.common.util;

import java.util.regex.Pattern;

/**
 * 敏感信息脱敏：手机号、身份证、API Key
 */
public final class SensitiveDataMasker {

    private static final Pattern PHONE = Pattern.compile("(1[3-9]\\d{1})\\d{4}(\\d{4})");
    private static final Pattern ID_CARD = Pattern.compile("(\\d{6})\\d{8}(\\d{4})");
    private static final Pattern API_KEY = Pattern.compile("(sk-[a-zA-Z0-9]{8})[a-zA-Z0-9]{20,}");
    private static final Pattern DEEPSEEK_KEY = Pattern.compile("(sk-[a-f0-9]{8})[a-f0-9]{24,}");

    private SensitiveDataMasker() {}

    public static String mask(String input) {
        if (input == null || input.isEmpty()) return input;
        String s = input;
        s = PHONE.matcher(s).replaceAll("$1****$2");
        s = ID_CARD.matcher(s).replaceAll("$1********$2");
        s = API_KEY.matcher(s).replaceAll("$1********************************");
        s = DEEPSEEK_KEY.matcher(s).replaceAll("$1********************************");
        return s;
    }
}
