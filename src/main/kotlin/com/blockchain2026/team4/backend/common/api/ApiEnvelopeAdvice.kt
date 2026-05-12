package com.blockchain2026.team4.backend.common.api

import com.blockchain2026.team4.backend.common.web.RequestIdFilter
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpResponse
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

@RestControllerAdvice(basePackages = ["com.blockchain2026.team4.backend"])
class ApiEnvelopeAdvice : ResponseBodyAdvice<Any> {
    override fun supports(
        returnType: MethodParameter,
        converterType: Class<out HttpMessageConverter<*>>,
    ): Boolean = true

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Any? {
        if (body is ApiSuccessResponse<*> || body is ApiErrorResponse) return body
        if (!selectedContentType.includes(MediaType.APPLICATION_JSON)) return body

        val status = (response as? ServletServerHttpResponse)?.servletResponse?.status ?: HttpStatus.OK.value()
        val httpStatus = HttpStatus.resolve(status) ?: HttpStatus.OK
        return ApiSuccessResponse(
            status = status,
            code = httpStatus.name,
            message = httpStatus.reasonPhrase,
            data = body,
            meta = ApiMeta(RequestIdFilter.currentRequestId()),
        )
    }
}
