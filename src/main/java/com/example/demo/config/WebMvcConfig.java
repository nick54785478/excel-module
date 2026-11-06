package com.example.demo.config;

import java.util.List;
import java.util.Objects;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.demo.presentation.resolver.ValidatedExcelDataArgumentResolver;

/**
 * Spring Web MVC 全域配置 (Web MVC Configuration)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 根目錄配置層 ({@code config/})，遵循 {@code AGENTS.md} 規則 40~41。<br>
 * 註冊表現層自訂參數解析器 {@link ValidatedExcelDataArgumentResolver}，
 * 支援在 Controller 方法入參中直接宣告 {@code @ValidatedExcelData List<T> validOrders}。
 * </p>
 *
 * @author Antigravity
 * @see ValidatedExcelDataArgumentResolver
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	private final ValidatedExcelDataArgumentResolver validatedExcelDataArgumentResolver;

	/**
	 * 建構子注入參數解析器
	 *
	 * @param validatedExcelDataArgumentResolver 已校驗資料參數解析器
	 */
	public WebMvcConfig(ValidatedExcelDataArgumentResolver validatedExcelDataArgumentResolver) {
		this.validatedExcelDataArgumentResolver = Objects.requireNonNull(validatedExcelDataArgumentResolver, "validatedExcelDataArgumentResolver must not be null");
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(validatedExcelDataArgumentResolver);
	}
}

