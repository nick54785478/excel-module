package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

/**
 * OpenAPI 3.0 / Swagger 全域配置類別 (Configuration Layer)
 * <p>
 * 遵循 Clean Architecture 架構規範：配置層集中管理系統層級技術細節與 Bean 註冊。
 * 提供 Swagger UI 與 OpenAPI 規範端點文件設定。
 */
@Configuration
public class OpenApiConfig {

	/**
	 * 配置 OpenAPI 規格資訊物件
	 *
	 * @return OpenAPI 規格定義 Bean
	 */
	@Bean
	public OpenAPI excelModuleOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Excel 模組處理服務 API (Excel Module API)")
						.description("遵循 DDD (領域驅動設計) 與 Clean Architecture 六角形架構實作之高效能 Excel 處理模組。<br>"
								+ "支援標準記憶體批次匯入/匯出，以及百萬級別低記憶體消耗的大數據串流匯入/匯出。")
						.version("v1.0.0")
						.contact(new Contact()
								.name("Module Architecture Team")
								.email("architecture@example.com"))
						.license(new License()
								.name("Apache 2.0")
								.url("https://www.apache.org/licenses/LICENSE-2.0")));
	}
}

