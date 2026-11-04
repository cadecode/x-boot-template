package com.github.cadecode.xboot.starter.web.advisor;

import cn.hutool.core.util.ObjUtil;
import com.github.cadecode.xboot.common.exception.GeneralException;
import com.github.cadecode.xboot.common.util.AspectUtil;
import com.github.cadecode.xboot.common.util.JacksonUtil;
import com.github.cadecode.xboot.starter.web.annotation.ApiFormat;
import com.github.cadecode.xboot.starter.web.enums.WebErrorEnum;
import com.github.cadecode.xboot.starter.web.model.ApiResult;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Objects;

/**
 * ApiResult 统一格式化
 *
 * @author Cade Li
 * @date 2022/5/8
 */
@RestControllerAdvice
public class ApiResultAdvisor implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // 获取请求路径
        String path = request.getURI().getPath();
        // 判断 body 是否是包装好的 ApiResult
        if (body instanceof ApiResult<?> result) {
            // 设置状态码
            response.setStatusCode(HttpStatus.valueOf(result.getStatus()));
            // 统一设置路径
            return result.path(path);
        }
        return apiResultFormat(returnType, body, response);
    }

    /**
     * 根据 ApiFormat 注解判断是否需要包装成 ApiResult
     *
     * @param returnType MethodParameter
     * @param body       响应体
     * @return 经过包装的 body
     */
    private Object apiResultFormat(MethodParameter returnType, Object body, ServerHttpResponse response) {
        // 方法上的注解优先，类上的兜底
        ApiFormat apiFormat = AspectUtil.findAnnotation(returnType.getContainingClass(), returnType.getMethod(),
                ApiFormat.class);
        // 没有注解或注解为 false，直接返回
        if (Objects.isNull(apiFormat) || !apiFormat.value()) {
            return body;
        }
        // 设置状态码为 200
        response.setStatusCode(HttpStatus.OK);
        if (body instanceof String) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            // String 类型的 Body 需要返回 String 类型，否则报转换错误
            return JacksonUtil.toJson(ApiResult.ok(body));
        }
        // 对接口返回 null 特殊处理
        if (ObjUtil.isNull(body)) {
            throw GeneralException.of(WebErrorEnum.RES_BODY_NULL, "");
        }
        return ApiResult.ok(body);
    }
}
