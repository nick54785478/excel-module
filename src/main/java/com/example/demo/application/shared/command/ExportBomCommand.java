package com.example.demo.application.shared.command;

import java.util.Objects;

/**
 * 匯出物料清單命令 (Export BOM Command)
 * <p>
 * 遵循 3-4 命名規範 (V + N + Command)，作為應用層匯出 BOM Excel 的命令資料載體。
 * 封裝查詢母件物料料號、版本號以及各個工作表之輸出開關選項。
 */
public class ExportBomCommand {

	private final String parentMaterialCode;
	private final String version;
	private final boolean includeMultiLevel;
	private final boolean includeSingleLevel;
	private final boolean includeMaterialMaster;
	private final int flushInterval;

	/**
	 * 建構匯出 BOM 命令物件 (預設 flushInterval 為 1000 筆)。
	 *
	 * @param parentMaterialCode    母件物料料號 (可為 null，代表匯出所有 BOM)
	 * @param version               BOM 版本號 (可為 null，代表最新或全部版本)
	 * @param includeMultiLevel     是否包含多階層展開工作表
	 * @param includeSingleLevel    是否包含單階匯入格式工作表
	 * @param includeMaterialMaster 是否包含物料主檔對照表
	 */
	public ExportBomCommand(
			String parentMaterialCode,
			String version,
			Boolean includeMultiLevel,
			Boolean includeSingleLevel,
			Boolean includeMaterialMaster
	) {
		this(parentMaterialCode, version, includeMultiLevel, includeSingleLevel, includeMaterialMaster, 1000);
	}

	/**
	 * 建構匯出 BOM 命令物件 (指定 flushInterval 串流沖刷間隔)。
	 *
	 * @param parentMaterialCode    母件物料料號
	 * @param version               BOM 版本號
	 * @param includeMultiLevel     是否包含多階層展開工作表
	 * @param includeSingleLevel    是否包含單階匯入格式工作表
	 * @param includeMaterialMaster 是否包含物料主檔對照表
	 * @param flushInterval         串流沖刷筆數間隔 (大於 0)
	 */
	public ExportBomCommand(
			String parentMaterialCode,
			String version,
			Boolean includeMultiLevel,
			Boolean includeSingleLevel,
			Boolean includeMaterialMaster,
			Integer flushInterval
	) {
		this.parentMaterialCode = parentMaterialCode != null && !parentMaterialCode.trim().isEmpty()
				? parentMaterialCode.trim()
				: null;
		this.version = version != null && !version.trim().isEmpty()
				? version.trim()
				: null;
		this.includeMultiLevel = includeMultiLevel != null ? includeMultiLevel : true;
		this.includeSingleLevel = includeSingleLevel != null ? includeSingleLevel : true;
		this.includeMaterialMaster = includeMaterialMaster != null ? includeMaterialMaster : true;
		this.flushInterval = flushInterval != null && flushInterval > 0 ? flushInterval : 1000;
	}

	public String getParentMaterialCode() {
		return parentMaterialCode;
	}

	public String getVersion() {
		return version;
	}

	public boolean isIncludeMultiLevel() {
		return includeMultiLevel;
	}

	public boolean isIncludeSingleLevel() {
		return includeSingleLevel;
	}

	public boolean isIncludeMaterialMaster() {
		return includeMaterialMaster;
	}

	public int getFlushInterval() {
		return flushInterval;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ExportBomCommand that = (ExportBomCommand) o;
		return includeMultiLevel == that.includeMultiLevel &&
				includeSingleLevel == that.includeSingleLevel &&
				includeMaterialMaster == that.includeMaterialMaster &&
				Objects.equals(parentMaterialCode, that.parentMaterialCode) &&
				Objects.equals(version, that.version);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parentMaterialCode, version, includeMultiLevel, includeSingleLevel, includeMaterialMaster);
	}

	@Override
	public String toString() {
		return "ExportBomCommand{" +
				"parentMaterialCode='" + parentMaterialCode + '\'' +
				", version='" + version + '\'' +
				", includeMultiLevel=" + includeMultiLevel +
				", includeSingleLevel=" + includeSingleLevel +
				", includeMaterialMaster=" + includeMaterialMaster +
				'}';
	}
}

