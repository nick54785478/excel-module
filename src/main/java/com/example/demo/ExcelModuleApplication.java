package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Excel 模組應用程式啟動類別 (Application Entry Point)
 * <p>
 * 本模組遵循 DDD 領域驅動設計與 Clean Architecture / 六角形架構規範，
 * 提供高效能 Excel 處理、匯入、匯出與資料轉換服務。
 */
@SpringBootApplication
public class ExcelModuleApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExcelModuleApplication.class, args);
	}

}
