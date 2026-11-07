package com.example.demo.presentation.resolver;

import java.util.Collections;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.example.demo.application.annotation.validation.ValidatedExcelData;
import com.example.demo.application.shared.context.ExcelValidationContextHolder;
import com.example.demo.application.shared.dto.ExcelValidationGottenResult;

/**
 * 表現層已驗證 Excel 資料參數解析器 (Validated Excel Data Handler Method Argument Resolver)
 * <p>
 * <b>【架構定位與職責說明】</b><br>
 * 位於 Clean Architecture 表現層參數解析器套件 ({@code presentation/resolver/})。<br>
 * 專門攔截並解析標註有 {@link ValidatedExcelData} 之 Controller 方法入參，
 * 防止 Spring MVC 將 {@link List} 誤認為 {@code @ModelAttribute} 實體並觸發反射建構子例外。<br>
 * 本解析器優先自 {@link ExcelValidationContextHolder} 執行緒上下文讀取合格資料，
 * 若尚未綁定則提供安全預設值，後續由 {@link com.example.demo.presentation.aspect.ExcelValidationAspect} 切面進行最終動態注入。
 * </p>
 *
 * @see ValidatedExcelData
 * @see ExcelValidationContextHolder
 */
@Component
public class ValidatedExcelDataArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(ValidatedExcelData.class);
	}

	@Override
	public Object resolveArgument(
			MethodParameter parameter,
			ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest,
			WebDataBinderFactory binderFactory) {

		Class<?> paramType = parameter.getParameterType();

		// 1. 若方法入參為 List<T> 清單
		if (List.class.isAssignableFrom(paramType)) {
			ResolvableType resolvableType = ResolvableType.forMethodParameter(parameter);
			Class<?> genericClass = resolvableType.getGeneric(0).resolve();
			if (genericClass != null) {
				List<?> items = ExcelValidationContextHolder.getValidItems(genericClass);
				if (!items.isEmpty()) {
					return items;
				}
			}
			return ExcelValidationContextHolder.get()
					.map(ExcelValidationGottenResult::validItems)
					.orElse(Collections.emptyList());
		}

		// 2. 若方法入參為 ExcelValidationGottenResult<?> DTO
		if (ExcelValidationGottenResult.class.isAssignableFrom(paramType)) {
			return ExcelValidationContextHolder.get().orElse(null);
		}

		return null;
	}
}

