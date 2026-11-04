package com.example.demo.application.shared.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Excel 串流活頁簿寫入命令 (Write Stream Excel Command)
 * <p>
 * 遵循 AGENTS.md 3-4 命名規範 ({@code V + N + Command})，採用 Java Record 作為應用層不可變純資料載體。
 * 封裝多個支援延遲載入串流的工作表寫入命令，專門用於百萬級大量資料串流匯出情境。
 *
 * @param applicationName    產製應用程式名稱 (預設為 ExcelModule)
 * @param applicationVersion 產製應用程式版本 (預設為 1.0)
 * @param sheets             多工作表串流寫入命令清單 (不可變清單)
 */
public record WriteStreamExcelCommand(
		String applicationName,
		String applicationVersion,
		List<WriteStreamSheetCommand> sheets
) {

	/**
	 * 緊湊建構子 (Compact Constructor)，執行預設值填補與集合不可變安全防禦
	 */
	public WriteStreamExcelCommand {
		applicationName = applicationName == null || applicationName.isBlank() ? "ExcelModule" : applicationName;
		applicationVersion = applicationVersion == null || applicationVersion.isBlank() ? "1.0" : applicationVersion;
		sheets = sheets == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(sheets));
	}

	/**
	 * 取得建造者物件
	 *
	 * @return 新建的 {@link Builder} 實例
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 便捷靜態建立方法 (包含單一串流工作表)
	 *
	 * @param sheet 單一串流工作表寫入命令
	 * @return 串流活頁簿寫入命令
	 */
	public static WriteStreamExcelCommand of(WriteStreamSheetCommand sheet) {
		return builder().addSheet(sheet).build();
	}

	// ==================== 向後相容別名方法 (Backward Compatible Accessors) ====================

	public String getApplicationName() {
		return applicationName();
	}

	public String getApplicationVersion() {
		return applicationVersion();
	}

	public List<WriteStreamSheetCommand> getSheets() {
		return sheets();
	}

	/**
	 * 建造者模式 (Builder)
	 */
	public static class Builder {
		private String applicationName = "ExcelModule";
		private String applicationVersion = "1.0";
		private List<WriteStreamSheetCommand> sheets = new ArrayList<>();

		public Builder applicationName(String applicationName) {
			this.applicationName = applicationName;
			return this;
		}

		public Builder applicationVersion(String applicationVersion) {
			this.applicationVersion = applicationVersion;
			return this;
		}

		public Builder sheets(List<WriteStreamSheetCommand> sheets) {
			this.sheets = sheets != null ? new ArrayList<>(sheets) : new ArrayList<>();
			return this;
		}

		public Builder addSheet(WriteStreamSheetCommand sheet) {
			if (sheet != null) {
				this.sheets.add(sheet);
			}
			return this;
		}

		public WriteStreamExcelCommand build() {
			return new WriteStreamExcelCommand(applicationName, applicationVersion, sheets);
		}
	}
}
