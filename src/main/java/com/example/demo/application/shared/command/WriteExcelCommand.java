package com.example.demo.application.shared.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Excel 檔案寫入命令物件 (Write Excel Command)
 * <p>
 * 遵循 {@code V + N + Command} 命名規範，封裝整份 Excel 活頁簿 (Workbook) 的中繼資訊與多工作表資料。
 * 作為純資料載體傳遞至 {@link com.example.demo.application.port.out.ExcelWriterPort}。
 */
public class WriteExcelCommand {

	private final String applicationName;
	private final String applicationVersion;
	private final List<WriteSheetCommand> sheets;

	private WriteExcelCommand(Builder builder) {
		this.applicationName = builder.applicationName == null || builder.applicationName.isBlank() ? "ExcelModule" : builder.applicationName;
		this.applicationVersion = builder.applicationVersion == null || builder.applicationVersion.isBlank() ? "1.0" : builder.applicationVersion;
		this.sheets = builder.sheets == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(builder.sheets));
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

	public static Builder builder() {
		return new Builder();
	}

	public String getApplicationName() {
		return applicationName;
	}

	public String getApplicationVersion() {
		return applicationVersion;
	}

	public List<WriteSheetCommand> getSheets() {
		return sheets;
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
			return new WriteExcelCommand(this);
		}
	}
}
