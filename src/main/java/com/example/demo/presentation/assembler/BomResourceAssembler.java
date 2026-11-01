package com.example.demo.presentation.assembler;

import org.springframework.stereotype.Component;

import com.example.demo.application.shared.command.ExportBomCommand;
import com.example.demo.presentation.resource.in.ExportBomResource;

/**
 * BOM 表現層資源組裝器 (BOM Resource Assembler)
 * <p>
 * 遵循 Clean Architecture 表現層防腐層 (ACL) 規範，
 * 負責將表現層傳入的 Request Resource 或查詢參數轉換為應用層之 {@link ExportBomCommand}。
 */
@Component
public class BomResourceAssembler {

	/**
	 * 將 {@link ExportBomResource} 請求載體轉換為應用層匯出命令。
	 *
	 * @param resource 表現層匯出請求載體
	 * @return 應用層匯出命令
	 */
	public ExportBomCommand toExportCommand(ExportBomResource resource) {
		if (resource == null) {
			return new ExportBomCommand(null, null, true, true, true, 1000);
		}
		return new ExportBomCommand(
				resource.parentMaterialCode(),
				resource.version(),
				resource.includeMultiLevel(),
				resource.includeSingleLevel(),
				resource.includeMaterialMaster(),
				resource.flushInterval()
		);
	}

	/**
	 * 將 HTTP GET 查詢參數轉換為應用層匯出命令。
	 *
	 * @param parentMaterialCode    母件料號
	 * @param version               BOM 版本
	 * @param includeMultiLevel     是否包含多階工作表
	 * @param includeSingleLevel    是否包含單階工作表
	 * @param includeMaterialMaster 是否包含物料主檔工作表
	 * @return 應用層匯出命令
	 */
	public ExportBomCommand toExportCommand(
			String parentMaterialCode,
			String version,
			Boolean includeMultiLevel,
			Boolean includeSingleLevel,
			Boolean includeMaterialMaster
	) {
		return toExportCommand(parentMaterialCode, version, includeMultiLevel, includeSingleLevel, includeMaterialMaster, 1000);
	}

	/**
	 * 將 HTTP GET 查詢參數 (包含串流 flushInterval) 轉換為應用層匯出命令。
	 *
	 * @param parentMaterialCode    母件料號
	 * @param version               BOM 版本
	 * @param includeMultiLevel     是否包含多階工作表
	 * @param includeSingleLevel    是否包含單階工作表
	 * @param includeMaterialMaster 是否包含物料主檔工作表
	 * @param flushInterval         串流沖刷間隔筆數
	 * @return 應用層匯出命令
	 */
	public ExportBomCommand toExportCommand(
			String parentMaterialCode,
			String version,
			Boolean includeMultiLevel,
			Boolean includeSingleLevel,
			Boolean includeMaterialMaster,
			Integer flushInterval
	) {
		return new ExportBomCommand(
				parentMaterialCode,
				version,
				includeMultiLevel,
				includeSingleLevel,
				includeMaterialMaster,
				flushInterval
		);
	}
}

