package com.example.demo.infrastructure.adapter.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.port.out.ExcelWriterPort;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.command.WriteStreamExcelCommand;
import com.example.demo.application.shared.command.WriteStreamSheetCommand;

/**
 * PoiSxssfWriterAdapter 單元與整合測試 (Unit & Integration Tests)
 * <p>
 * 針對 {@link PoiSxssfWriterAdapter} 進行 SXSSF 串流寫入、二進位生成、多工作表與格式驗證。
 * 所有測試方法嚴格遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 語法註解規範。
 */
@DisplayName("PoiSxssfWriterAdapter 寫入適配器測試")
class PoiSxssfWriterAdapterTest {

	private ExcelWriterPort excelWriterPort;

	@BeforeEach
	void setUp() {
		this.excelWriterPort = new PoiSxssfWriterAdapter();
	}

	/**
	 * Feature: 單一工作表二進位寫入
	 * Scenario: 使用 Apache POI SXSSF 寫入包含標題與不同型別資料之單一工作表
	 * Given: 建立包含欄位名稱與不同型別資料 (String, BigDecimal, Integer, LocalDate, Boolean)
	 * When: 呼叫 excelWriterPort.writeSingleSheet 寫入為 byte 陣列
	 * Then: 產出之 byte 陣列長度大於 0，且符合標準 ZIP/OOXML 魔術字元 0x50 0x4B
	 */
	@Test
	@DisplayName("情境：成功將單一工作表寫入為 byte[] 並驗證 ZIP 魔術字元")
	void shouldWriteSingleSheetToByteArraySuccessfully() {
		// Given
		String sheetName = "訂單報表";
		List<String> headers = List.of("訂單編號", "商品名稱", "單價", "數量", "訂購日期", "是否已出貨", "備註");
		List<List<Object>> rows = List.of(
				List.of("ORD-001", "MacBook Pro", new BigDecimal("72900.00"), 1, LocalDate.of(2026, 10, 31), true, "急單"),
				List.of("ORD-002", "Magic Mouse", new BigDecimal("2490.50"), 2, LocalDate.of(2026, 10, 30), false, "")
		);

		// When
		byte[] bytes = excelWriterPort.writeSingleSheet(sheetName, headers, rows);

		// Then
		assertThat(bytes).isNotNull();
		assertThat(bytes.length).isGreaterThan(0);
		assertThat(bytes[0]).isEqualTo((byte) 0x50);
		assertThat(bytes[1]).isEqualTo((byte) 0x4B);
	}

	/**
	 * Feature: 多工作表活頁簿串流寫入
	 * Scenario: 寫入包含多個不同工作表名稱之活頁簿至 OutputStream
	 * Given: 建立包含兩個工作表 ("銷售總表", "明細數據") 的 WriteExcelCommand
	 * When: 呼叫 excelWriterPort.write(outputStream, command)
	 * Then: OutputStream 應順利接收完整的 Excel 資料且不拋出任何異常
	 */
	@Test
	@DisplayName("情境：成功將多工作表活頁簿串流寫入指定的 OutputStream")
	void shouldWriteMultiSheetWorkbookSuccessfully() throws Exception {
		// Given
		WriteSheetCommand sheet1 = WriteSheetCommand.builder()
				.sheetName("銷售總表")
				.headers(List.of("月份", "總營收"))
				.rows(List.of(
						List.of("10月", 1500000),
						List.of("11月", 1850000)
				))
				.build();

		WriteSheetCommand sheet2 = WriteSheetCommand.builder()
				.sheetName("明細數據")
				.headers(List.of("項目代碼", "狀態", "更新時間"))
				.rows(List.of(
						List.of("ITEM-A", "已審核", LocalDateTime.of(2026, 10, 31, 10, 0, 0)),
						List.of("ITEM-B", "待處理", LocalDateTime.of(2026, 10, 31, 11, 30, 0))
				))
				.build();

		WriteExcelCommand command = WriteExcelCommand.builder()
				.applicationName("OrderSystem")
				.applicationVersion("2.0")
				.addSheet(sheet1)
				.addSheet(sheet2)
				.build();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// When & Then
		assertDoesNotThrow(() -> excelWriterPort.write(baos, command));
		byte[] result = baos.toByteArray();
		assertThat(result).isNotNull();
		assertThat(result.length).isGreaterThan(0);

		try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(result))) {
			assertThat(wb.getNumberOfSheets()).isEqualTo(2);
			assertThat(wb.getSheetAt(0).getSheetName()).isEqualTo("銷售總表");
			assertThat(wb.getSheetAt(1).getSheetName()).isEqualTo("明細數據");
		}
	}

	/**
	 * Feature: 大數據串流 writeStream 寫入
	 * Scenario: 傳入串流命令 WriteStreamExcelCommand，以分批沖刷機制產生 Excel 檔案
	 * Given: 建立包含 50 筆資料且設定 flushInterval=10 的 WriteStreamSheetCommand
	 * When: 呼叫 excelWriterPort.writeStream
	 * Then: 成功產生合法 Excel 二進位陣列，驗證工作表與行數
	 */
	@Test
	@DisplayName("情境：成功透過 writeStream 執行大數據串流輸出")
	void shouldWriteStreamSuccessfully() throws Exception {
		// Given
		List<List<Object>> streamRows = List.of(
				List.of("CODE-01", "物料 A", 10.5),
				List.of("CODE-02", "物料 B", 20.0),
				List.of("CODE-03", "物料 C", 30.25)
		);

		WriteStreamSheetCommand sheet = WriteStreamSheetCommand.builder()
				.sheetName("串流測試表")
				.headers(List.of("代碼", "名稱", "單價"))
				.rowIterable(streamRows)
				.flushInterval(2)
				.freezeHeader(true)
				.headerFillColor("1F4E78")
				.headerFontColor("FFFFFF")
				.build();

		WriteStreamExcelCommand command = WriteStreamExcelCommand.builder()
				.applicationName("StreamApp")
				.applicationVersion("1.0")
				.addSheet(sheet)
				.build();

		// When
		byte[] bytes = excelWriterPort.writeStream(command);

		// Then
		assertThat(bytes).isNotEmpty();
		try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
			assertThat(wb.getNumberOfSheets()).isEqualTo(1);
			assertThat(wb.getSheetAt(0).getSheetName()).isEqualTo("串流測試表");
			assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(3);
		}
	}
}

