package com.example.demo.application.shared.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Excel 檔案寫入命令 (Write Excel Command)
 * <p>
 * 遵循 AGENTS.md 3-4 命名規範 ({@code V + N + Command})，採用 Java Record 作為應用層不可變純資料載體。
 * 封裝整份 Excel 活頁簿 (Workbook) 的中繼資訊與多工作表資料。
 * 作為純資料載體傳遞至 {@link com.example.demo.application.port.out.ExcelWriterPort}。
 *
 * @param applicationName    產製應用程式名稱 (預設為 ExcelModule)
 * @param applicationVersion 產製應用程式版本 (預設為 1.0)
 * @param sheets             多工作表寫入命令清單 (不可變清單)
 */
public record WriteExcelCommand(
		String applicationName,
		String applicationVersion,
		List<WriteSheetCommand> sheets
) {

	/**
	 * 緊湊建構子 (Compact Constructor)，執行中繼屬性預設值填補與集合不可變防禦
	 */
	public WriteExcelCommand {
		applicationName = applicationName == null || applicationName.isBlank() ? "ExcelModule" : applicationName;
		applicationVersion = applicationVersion == null || applicationVersion.isBlank() ? "1.0" : applicationVersion;
		sheets = sheets == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(sheets));
	}

	/**
	 * 便捷靜態建立方法 (包含單一工作表)
	 *
	 * @param sheet 單一工作表寫入命令
	 * @return Excel 寫入命令
	 */
	public static WriteExcelCommand of(WriteSheetCommand sheet) {
		return builder().addSheet(sheet).build();
	}

	/**
	 * 便捷靜態建立方法 (包含多工作表)
	 *
	 * @param sheets 多工作表寫入命令清單
	 * @return Excel 寫入命令
	 */
	public static WriteExcelCommand of(List<WriteSheetCommand> sheets) {
		return builder().sheets(sheets).build();
	}

	/**
	 * 取得建造者物件
	 *
	 * @return 新建的 {@link Builder} 實例
	 */
	public static Builder builder() {
		return new Builder();
	}

	// ==================== 向後相容別名方法 (Backward Compatible Accessors) ====================

	public String getApplicationName() {
		return applicationName();
	}

	public String getApplicationVersion() {
		return applicationVersion();
	}

	public List<WriteSheetCommand> getSheets() {
		return sheets();
	}

	/**
	 * 建造者模式 (Builder)
	 */
	public static class Builder {
		private String applicationName = "ExcelModule";
		private String applicationVersion = "1.0";
		private List<WriteSheetCommand> sheets = new ArrayList<>();

		public Builder applicationName(String applicationName) {
			this.applicationName = applicationName;
			return this;
		}

		public Builder applicationVersion(String applicationVersion) {
			this.applicationVersion = applicationVersion;
			return this;
		}

		public Builder sheets(List<WriteSheetCommand> sheets) {
			this.sheets = sheets != null ? new ArrayList<>(sheets) : new ArrayList<>();
			return this;
		}

		public Builder addSheet(WriteSheetCommand sheet) {
			if (sheet != null) {
				this.sheets.add(sheet);
			}
			return this;
		}

		public WriteExcelCommand build() {
			return new WriteExcelCommand(applicationName, applicationVersion, sheets);
		}
	}
}
