package com.example.demo.infrastructure.template;

/**
 * Excel 範本註冊自訂擴充回呼介面 (Excel Template Customizer SPI)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 基礎設施層範本套件 ({@code infrastructure/template/})。<br>
 * 遵循物件導向開放封閉原則 (Open-Closed Principle, OCP)，
 * 任何子模組或下游服務若需動態擴充或自訂 Excel 範本對應規則，
 * 僅需實作此介面並宣告為 Spring Bean，
 * 配置層 {@link com.example.demo.config.ExcelTemplateConfiguration} 於啟動時將自動收集並回呼執行自訂註冊，
 * 達成現有核心程式碼 100% 零修改的極致擴充性。
 */
@FunctionalInterface
public interface ExcelTemplateCustomizer {

	/**
	 * 回呼擴充或自訂範本註冊表
	 *
	 * @param registry 範本註冊表實例
	 */
	void customize(ExcelTemplateRegistry registry);
}

