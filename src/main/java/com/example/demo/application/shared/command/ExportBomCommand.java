package com.example.demo.application.shared.command;

import java.util.Objects;

/**
 * 匯出物料清單命令 (Export BOM Command)
 * <p>
 * 遵循 AGENTS.md 3-4 命名規範 ({@code V + N + Command})，採用 Java Record 作為應用層不可變純資料載體。
 * 封裝查詢母件物料料號、版本號以及各個工作表之輸出開關選項與串流沖刷間隔。
 *
 * @param parentMaterialCode    母件物料料號 (可為 null，代表匯出所有 BOM)
 * @param version               BOM 版本號 (可為 null，代表最新或全部版本)
 * @param includeMultiLevel     是否包含多階層展開工作表
 * @param includeSingleLevel    是否包含單階匯入格式工作表
 * @param includeMaterialMaster 是否包含物料主檔對照表
 * @param flushInterval         串流沖刷筆數間隔 (大於 0，預設 1000 筆)
 */
public record ExportBomCommand(
		String parentMaterialCode,
		String version,
		boolean includeMultiLevel,
		boolean includeSingleLevel,
		boolean includeMaterialMaster,
		int flushInterval
) {

	/**
	 * 緊湊建構子 (Compact Constructor)，執行欄位清洗與安全防禦
	 */
	public ExportBomCommand {
		parentMaterialCode = parentMaterialCode != null && !parentMaterialCode.trim().isEmpty()
				? parentMaterialCode.trim()
				: null;
		version = version != null && !version.trim().isEmpty()
				? version.trim()
				: null;
		if (flushInterval <= 0) {
			flushInterval = 1000;
		}
	}

	/**
	 * 便捷建構子 (預設 flushInterval 為 1000 筆)
	 *
	 * @param parentMaterialCode    母件物料料號
	 * @param version               BOM 版本號
	 * @param includeMultiLevel     是否包含多階層展開工作表 (為 null 時預設為 true)
	 * @param includeSingleLevel    是否包含單階匯入格式工作表 (為 null 時預設為 true)
	 * @param includeMaterialMaster 是否包含物料主檔對照表 (為 null 時預設為 true)
	 */
	public ExportBomCommand(
			String parentMaterialCode,
			String version,
			Boolean includeMultiLevel,
			Boolean includeSingleLevel,
			Boolean includeMaterialMaster
	) {
		this(
				parentMaterialCode,
				version,
				includeMultiLevel != null ? includeMultiLevel : true,
				includeSingleLevel != null ? includeSingleLevel : true,
				includeMaterialMaster != null ? includeMaterialMaster : true,
				1000
		);
	}

	/**
	 * 便捷建構子 (支援包裝型別參數與自訂 flushInterval)
	 *
	 * @param parentMaterialCode    母件物料料號
	 * @param version               BOM 版本號
	 * @param includeMultiLevel     是否包含多階層展開工作表
	 * @param includeSingleLevel    是否包含單階匯入格式工作表
	 * @param includeMaterialMaster 是否包含物料主檔對照表
	 * @param flushInterval         串流沖刷筆數間隔
	 */
	public ExportBomCommand(
			String parentMaterialCode,
			String version,
			Boolean includeMultiLevel,
			Boolean includeSingleLevel,
			Boolean includeMaterialMaster,
			Integer flushInterval
	) {
		this(
				parentMaterialCode,
				version,
				includeMultiLevel != null ? includeMultiLevel : true,
				includeSingleLevel != null ? includeSingleLevel : true,
				includeMaterialMaster != null ? includeMaterialMaster : true,
				flushInterval != null && flushInterval > 0 ? flushInterval : 1000
		);
	}

	// ==================== 向後相容別名方法 (Backward Compatible Accessors) ====================

	public String getParentMaterialCode() {
		return parentMaterialCode();
	}

	public String getVersion() {
		return version();
	}

	public boolean isIncludeMultiLevel() {
		return includeMultiLevel();
	}

	public boolean isIncludeSingleLevel() {
		return includeSingleLevel();
	}

	public boolean isIncludeMaterialMaster() {
		return includeMaterialMaster();
	}

	public int getFlushInterval() {
		return flushInterval();
	}
}
