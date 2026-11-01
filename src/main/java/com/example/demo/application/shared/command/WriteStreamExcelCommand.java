package com.example.demo.application.shared.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Excel 串流活頁簿寫入命令 (Write Stream Excel Command)
 * <p>
 * 遵循 {@code V + N + Command} 命名規範，封裝多個支援延遲載入串流的工作表寫入命令。
 * 專門用於百萬級大量資料匯出情境。
 */
public class WriteStreamExcelCommand {

	private final String applicationName;
	private final String applicationVersion;
	private final List<WriteStreamSheetCommand> sheets;

	private WriteStreamExcelCommand(Builder builder) {
		this.applicationName = builder.applicationName == null || builder.applicationName.isBlank() ? "ExcelModule" : builder.applicationName;
		this.applicationVersion = builder.applicationVersion == null || builder.applicationVersion.isBlank() ? "1.0" : builder.applicationVersion;
		this.sheets = builder.sheets == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(builder.sheets));
	}

	public static Builder builder() {
		return new Builder();
	}

	public static WriteStreamExcelCommand of(WriteStreamSheetCommand sheet) {
		return builder().addSheet(sheet).build();
	}

	public String getApplicationName() {
		return applicationName;
	}

	public String getApplicationVersion() {
		return applicationVersion;
	}

	public List<WriteStreamSheetCommand> getSheets() {
		return sheets;
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
			return new WriteStreamExcelCommand(this);
		}
	}
}
