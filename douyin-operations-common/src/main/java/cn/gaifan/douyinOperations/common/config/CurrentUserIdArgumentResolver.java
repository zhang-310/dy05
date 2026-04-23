package cn.gaifan.douyinOperations.common.config;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 解析 @CurrentUserId 标注的参数，从 request 属性中注入 userId。
 */
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String ATTR_USER_ID = "userId";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
            && (parameter.getParameterType() == Long.class || parameter.getParameterType() == long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) return null;
        Object attr = request.getAttribute(ATTR_USER_ID);
        if (attr instanceof Long) return attr;
        if (attr instanceof Number) return ((Number) attr).longValue();
        return null;
    }
}
