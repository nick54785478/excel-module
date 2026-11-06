package com.example.demo.presentation.assembler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.demo.application.shared.command.ExportExcelCommand;
import com.example.demo.application.shared.command.StreamExportExcelCommand;
import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.command.WriteStreamSheetCommand;
import com.example.demo.application.shared.dto.ExcelGottenResult;
import com.example.demo.application.shared.dto.ExcelSheetGottenResult;
import com.example.demo.application.shared.dto.ExcelStreamImportGottenResult;
import com.example.demo.presentation.resource.in.ExportExcelResource;
import com.example.demo.presentation.resource.in.StreamExportExcelResource;
import com.example.demo.presentation.resource.out.ExcelImportedResource;
import com.example.demo.presentation.resource.out.ExcelStreamImportedResource;

/**
 * Excel 表現層防腐組裝器 (Excel Resource Assembler / Anti-Corruption Layer)
 * <p>
 * 遵循 Clean Architecture 規範，獨立於 Resource 之外，專責表現層與應用層之間的資料模型轉換。
 * 維持 Request / Response Resource 純資料載體特性，隔離外部 HTTP 契約與內部業務契約。
 */
@Component
public class ExcelResourceAssembler {

	/**
	 * 將匯出請求資源轉換為應用層匯出命令
	 *
	 * @param resource 匯出請求資源
	 * @return 應用層匯出命令
	 */
	public ExportExcelCommand toExportCommand(ExportExcelResource resource) {
		if (resource == null) {
			throw new IllegalArgumentException("ExportExcelResource must not be null");
		}

		List<WriteSheetCommand> sheets = new ArrayList<>();
		if (resource.sheets() != null) {
			for (ExportExcelResource.ExportSheetResource sheetResource : resource.sheets()) {
				WriteSheetCommand.Builder builder = WriteSheetCommand.builder()
						.sheetName(sheetResource.sheetName())
						.headers(sheetResource.headers())
						.rows(sheetResource.rows());

				if (sheetResource.freezeHeader() != null) {
					builder.freezeHeader(sheetResource.freezeHeader());
				}
				if (sheetResource.headerFillColor() != null && !sheetResource.headerFillColor().isBlank()) {
					builder.headerFillColor(sheetResource.headerFillColor());
				}
				if (sheetResource.headerFontColor() != null && !sheetResource.headerFontColor().isBlank()) {
					builder.headerFontColor(sheetResource.headerFontColor());
				}
				sheets.add(builder.build());
			}
		}

		return new ExportExcelCommand(resource.fileName(), sheets);
	}

	/**
	 * 將串流匯出請求資源轉換為應用層串流匯出命令
	 * <p>
	 * 將前端傳入之真實工作表資料、標題與列資料包裝為可迭代串流工作表命令。
	 *
	 * @param resource 串流匯出請求資源
	 * @return 應用層串流匯出命令
	 */
	public StreamExportExcelCommand toStreamExportCommand(StreamExportExcelResource resource) {
		if (resource == null) {
			throw new IllegalArgumentException("StreamExportExcelResource must not be null");
		}

		int flushSize = resource.batchFlushSize() != null ? resource.batchFlushSize() : 1000;
		List<WriteStreamSheetCommand> streamSheets = new ArrayList<>();

		if (resource.sheets() != null) {
			for (ExportExcelResource.ExportSheetResource sheetResource : resource.sheets()) {
				List<List<Object>> rows = sheetResource.rows() != null ? sheetResource.rows() : Collections.emptyList();
				streamSheets.add(WriteStreamSheetCommand.builder()
						.sheetName(sheetResource.sheetName())
						.headers(sheetResource.headers())
						.rowIterable(rows)
						.flushInterval(flushSize)
						.freezeHeader(Boolean.TRUE.equals(sheetResource.freezeHeader()))
						.headerFillColor(sheetResource.headerFillColor())
						.headerFontColor(sheetResource.headerFontColor())
						.build());
			}
		}

		return new StreamExportExcelCommand(
				resource.fileName() != null ? resource.fileName() : "stream_export.xlsx",
				streamSheets,
				flushSize
		);
	}

	/**
	 * 將應用層讀取結果 DTO 轉換為表現層回應資源
	 *
	 * @param gottenResult 讀取結果 DTO
	 * @return 匯入回應資源
	 */
	public ExcelImportedResource toImportedResource(ExcelGottenResult gottenResult) {
		if (gottenResult == null || gottenResult.getSheets() == null) {
			return new ExcelImportedResource(0, Collections.emptyList());
		}

		List<ExcelImportedResource.ExcelSheetImportedResource> sheetResources = new ArrayList<>();
		for (ExcelSheetGottenResult sheet : gottenResult.getSheets()) {
			sheetResources.add(new ExcelImportedResource.ExcelSheetImportedResource(
					sheet.getSheetIndex(),
					sheet.getSheetName(),
					sheet.getRowCount(),
					sheet.getHeaders(),
					sheet.getDataRowsAsMaps()
			));
		}

		return new ExcelImportedResource(sheetResources.size(), sheetResources);
	}

	/**
	 * 將應用層串流匯入結果 DTO 轉換為表現層回應資源
	 *
	 * @param result 串流匯入結果 DTO
	 * @return 串流匯入回應資源
	 */
	public ExcelStreamImportedResource toStreamImportedResource(ExcelStreamImportGottenResult result) {
		if (result == null) {
			return new ExcelStreamImportedResource("UNKNOWN", 0, 0, 0, "FAILED", "Result is null");
		}

		return new ExcelStreamImportedResource(
				result.sheetName(),
				result.totalRowsProcessed(),
				result.totalChunks(),
				result.durationMs(),
				result.status(),
				result.message()
		);
	}
}
