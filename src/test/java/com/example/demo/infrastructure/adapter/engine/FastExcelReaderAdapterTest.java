package com.example.demo.infrastructure.adapter.engine;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.port.out.ExcelReaderPort;
import com.example.demo.application.port.out.ExcelWriterPort;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.dto.ExcelGottenResult;
import com.example.demo.application.shared.dto.ExcelSheetGottenResult;
import com.example.demo.application.shared.exception.ExcelReadException;
import com.example.demo.application.shared.query.ReadExcelQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FastExcelReaderAdapter 單元與整合測試 (Unit & Integration Tests)
 * <p>
 * 針對 {@link FastExcelReaderAdapter} 進行檔案讀取、條件過濾、型別還原與標題映射驗證。
 * 所有測試方法嚴格遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 語法註解規範。
 */
@DisplayName("FastExcelReaderAdapter 讀取適配器測試")
class FastExcelReaderAdapterTest {

	private ExcelWriterPort excelWriterPort;
	private ExcelReaderPort excelReaderPort;

	@BeforeEach
	void setUp() {
		this.excelWriterPort = new FastExcelWriterAdapter();
		this.excelReaderPort = new FastExcelReaderAdapter();
	}

	/**
	 * Feature: Excel 單一工作表讀取
	 * Scenario: 使用 FastExcelReaderAdapter 從 byte[] 讀取預先生成的 Excel 內容
	 * Given: 建立包含標題 ("員工編號", "姓名", "薪資", "是否在職") 與 2 筆員工資料的 Excel 二進位內容
	 * When: 呼叫 excelReaderPort.readFirstSheet(bytes)
	 * Then: 應成功解析工作表名稱、標題清單與資料列內容，且型別與數值正確無誤
	 */
	@Test
	@DisplayName("情境：成功讀取單一工作表並還原標題與儲存格數值")
	void shouldReadSingleSheetSuccessfully() {
		// Given
		String sheetName = "員工名冊";
		List<String> headers = List.of("員工編號", "姓名", "薪資", "是否在職");
		List<List<Object>> originalRows = List.of(
				List.of("EMP-001", "王小明", new BigDecimal("65000"), true),
				List.of("EMP-002", "李小花", new BigDecimal("58000"), false)
		);
		byte[] excelBytes = excelWriterPort.writeSingleSheet(sheetName, headers, originalRows);

		// When
		ExcelSheetGottenResult result = excelReaderPort.readFirstSheet(excelBytes);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getSheetName()).isEqualTo(sheetName);
		assertThat(result.getHeaders()).containsExactly("員工編號", "姓名", "薪資", "是否在職");
		assertThat(result.getRowCount()).isEqualTo(2);

		// 驗證第一列資料
		List<Object> firstRow = result.getRows().get(0);
		assertThat(firstRow.get(0)).isEqualTo("EMP-001");
		assertThat(firstRow.get(1)).isEqualTo("王小明");
		assertThat(new BigDecimal(firstRow.get(2).toString())).isEqualByComparingTo("65000");
		assertThat(firstRow.get(3)).isEqualTo(true);
	}

	/**
	 * Feature: 多工作表條件讀取與最大筆數限制
	 * Scenario: 在包含多個工作表的 Excel 中，僅讀取指定名稱的工作表且限制最多 1 筆資料
	 * Given: 包含 "部門A" 與 "部門B" 兩工作表的 Excel 檔案，其中部門B含有 3 筆資料
	 * When: 使用 ReadExcelQuery 指定 sheetName="部門B" 且 maxRows=1 呼叫 read(inputStream, query)
	 * Then: 回傳結果應僅包含 "部門B" 一個工作表，且資料筆數為 1
	 */
	@Test
	@DisplayName("情境：依工作表名稱過濾並套用 maxRows 最大筆數限制")
	void shouldReadWithQueryFilteringBySheetNameAndMaxRows() {
		// Given
		WriteSheetCommand sheetA = WriteSheetCommand.of("部門A", List.of("代碼"), List.of(List.of("A1"), List.of("A2")));
		WriteSheetCommand sheetB = WriteSheetCommand.of("部門B", List.of("代碼", "名稱"), List.of(
				List.of("B1", "行銷部"),
				List.of("B2", "研發部"),
				List.of("B3", "財務部")
		));
		byte[] excelBytes = excelWriterPort.write(WriteExcelCommand.of(List.of(sheetA, sheetB)));

		ReadExcelQuery query = ReadExcelQuery.builder()
				.sheetName("部門B")
				.maxRows(1)
				.build();

		// When
		ExcelGottenResult gottenResult = excelReaderPort.read(new ByteArrayInputStream(excelBytes), query);

		// Then
		assertThat(gottenResult.getSheetCount()).isEqualTo(1);
		ExcelSheetGottenResult sheetResult = gottenResult.getFirstSheet().orElseThrow();
		assertThat(sheetResult.getSheetName()).isEqualTo("部門B");
		assertThat(sheetResult.getRowCount()).isEqualTo(1);
		assertThat(sheetResult.getRows().get(0)).containsExactly("B1", "行銷部");
	}

	/**
	 * Feature: 標題鍵值對自動映射 (Header to Row Map)
	 * Scenario: 讀取工作表後，將各列資料轉換為以標題為 Key 之 Map 集合
	 * Given: 包含標題 ("SKU", "數量") 與 2 筆庫存資料之 Excel
	 * When: 呼叫 result.getDataRowsAsMaps()
	 * Then: 回傳集合包含 2 個 Map，且每個 Map 鍵皆與對應標題吻合
	 */
	@Test
	@DisplayName("情境：成功將各列資料轉換為鍵值對字典集合 (getDataRowsAsMaps)")
	void shouldMapDataRowsAsHeadersSuccessfully() {
		// Given
		List<String> headers = List.of("SKU", "數量");
		List<List<Object>> rows = List.of(
				List.of("SKU-100", 50),
				List.of("SKU-200", 80)
		);
		byte[] excelBytes = excelWriterPort.writeSingleSheet("庫存", headers, rows);

		// When
		ExcelSheetGottenResult sheetResult = excelReaderPort.readFirstSheet(excelBytes);
		List<Map<String, Object>> mapList = sheetResult.getDataRowsAsMaps();

		// Then
		assertThat(mapList).hasSize(2);
		assertThat(mapList.get(0))
				.containsEntry("SKU", "SKU-100")
				.containsEntry("數量", new BigDecimal(50));
		assertThat(mapList.get(1))
				.containsEntry("SKU", "SKU-200")
				.containsEntry("數量", new BigDecimal(80));
	}

	/**
	 * Feature: 異常檔案讀取防呆處理
	 * Scenario: 傳入非合法 OOXML/ZIP 格式之損毀資料，應拋出明確的 ExcelReadException
	 * Given: 一組隨機非合法的損壞二進位位元組陣列
	 * When: 呼叫 excelReaderPort.read(corruptedBytes)
	 * Then: 預期拋出 ExcelReadException
	 */
	@Test
	@DisplayName("情境：傳入損毀二進位內容時拋出 ExcelReadException")
	void shouldHandleEmptyOrCorruptedExcelThrowsException() {
		// Given
		byte[] corruptedBytes = "This is definitely not an excel file content".getBytes();

		// When & Then
		assertThatThrownBy(() -> excelReaderPort.read(corruptedBytes))
				.isInstanceOf(ExcelReadException.class)
				.hasMessageContaining("Failed to read Excel");
	}
}
