package com.example.demo.infrastructure.adapter.core;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.application.port.out.core.ExcelWriterPort;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.command.WriteSheetCommand;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 測試範本 Excel 檔案自動產製器 (Test Template Generator)
 * <p>
 * 自動生成位於 {@code doc/template/} 與 {@code src/main/resources/templates/} 下的各類業務測試範本：
 * <ul>
 *   <li>{@code ORDER_IMPORT_SUCCESS.xlsx} - 合規訂單匯入測試範本 (JavaBean OrderModel)</li>
 *   <li>{@code ORDER_IMPORT_WITH_ERRORS.xlsx} - 含違規資料之訂單測試範本 (用於客製驗證與回標測試)</li>
 *   <li>{@code PRODUCT_IMPORT_SUCCESS.xlsx} - 合規商品主檔測試範本 (Java 21 Record ProductRecordModel)</li>
 *   <li>{@code PRODUCT_IMPORT_WITH_ERRORS.xlsx} - 含違規售價之商品測試範本</li>
 *   <li>{@code PO_IMPORT.xlsx} - 採購明細匯入範本 (同步複製)</li>
 * </ul>
 * </p>
 */
@SpringBootTest
@DisplayName("測試範本 Excel 檔案自動產製與同步")
class TestTemplateGeneratorTest {

	private final ExcelWriterPort excelWriterPort = new PoiSxssfWriterAdapter();

	@Test
	@DisplayName("場景：自動生成並同步所有業務與驗證測試範本 Excel 至 doc/template/ 目錄")
	void generateAllTestTemplates() throws IOException {
		Path docTemplateDir = Paths.get("doc/template");
		Path resourceTemplateDir = Paths.get("src/main/resources/templates");
		Files.createDirectories(docTemplateDir);
		Files.createDirectories(resourceTemplateDir);

		// 1. 生成 ORDER_IMPORT_SUCCESS.xlsx
		byte[] orderSuccessBytes = generateOrderSuccessExcel();
		writeFile(docTemplateDir.resolve("ORDER_IMPORT_SUCCESS.xlsx"), orderSuccessBytes);
		writeFile(resourceTemplateDir.resolve("ORDER_IMPORT_SUCCESS.xlsx"), orderSuccessBytes);

		// 2. 生成 ORDER_IMPORT_WITH_ERRORS.xlsx
		byte[] orderErrorsBytes = generateOrderErrorsExcel();
		writeFile(docTemplateDir.resolve("ORDER_IMPORT_WITH_ERRORS.xlsx"), orderErrorsBytes);
		writeFile(resourceTemplateDir.resolve("ORDER_IMPORT_WITH_ERRORS.xlsx"), orderErrorsBytes);

		// 3. 生成 PRODUCT_IMPORT_SUCCESS.xlsx
		byte[] productSuccessBytes = generateProductSuccessExcel();
		writeFile(docTemplateDir.resolve("PRODUCT_IMPORT_SUCCESS.xlsx"), productSuccessBytes);
		writeFile(resourceTemplateDir.resolve("PRODUCT_IMPORT_SUCCESS.xlsx"), productSuccessBytes);

		// 4. 生成 PRODUCT_IMPORT_WITH_ERRORS.xlsx
		byte[] productErrorsBytes = generateProductErrorsExcel();
		writeFile(docTemplateDir.resolve("PRODUCT_IMPORT_WITH_ERRORS.xlsx"), productErrorsBytes);
		writeFile(resourceTemplateDir.resolve("PRODUCT_IMPORT_WITH_ERRORS.xlsx"), productErrorsBytes);

		// 驗證檔案皆存在且不為空
		assertThat(Files.size(docTemplateDir.resolve("ORDER_IMPORT_SUCCESS.xlsx"))).isGreaterThan(0);
		assertThat(Files.size(docTemplateDir.resolve("ORDER_IMPORT_WITH_ERRORS.xlsx"))).isGreaterThan(0);
		assertThat(Files.size(docTemplateDir.resolve("PRODUCT_IMPORT_SUCCESS.xlsx"))).isGreaterThan(0);
		assertThat(Files.size(docTemplateDir.resolve("PRODUCT_IMPORT_WITH_ERRORS.xlsx"))).isGreaterThan(0);
	}

	private byte[] generateOrderSuccessExcel() {
		List<String> headers = List.of("訂單編號", "訂購人", "購買金額", "數量", "下單日期", "訂單狀態");
		List<List<Object>> rows = List.of(
				List.of("ORD-2026-1001", "王小明", BigDecimal.valueOf(1500.00), 2, LocalDate.of(2026, 11, 1), "PAID"),
				List.of("ORD-2026-1002", "李大華", BigDecimal.valueOf(3200.50), 1, LocalDate.of(2026, 11, 2), "PENDING"),
				List.of("ORD-2026-1003", "張美麗", BigDecimal.valueOf(880.00), 5, LocalDate.of(2026, 11, 3), "PAID"),
				List.of("ORD-2026-1004", "陳志強", BigDecimal.valueOf(650.00), 3, LocalDate.of(2026, 11, 4), "SHIPPED"),
				List.of("ORD-2026-1005", "林小芬", BigDecimal.valueOf(4200.00), 2, LocalDate.of(2026, 11, 5), "COMPLETED")
		);

		WriteSheetCommand sheet = WriteSheetCommand.builder()
				.sheetName("訂單匯入清單")
				.headers(headers)
				.rows(rows)
				.freezeHeader(true)
				.headerFillColor("1F4E78")
				.headerFontColor("FFFFFF")
				.build();

		return excelWriterPort.write(WriteExcelCommand.of(sheet));
	}

	private byte[] generateOrderErrorsExcel() {
		List<String> headers = List.of("訂單編號", "訂購人", "購買金額", "數量", "下單日期", "訂單狀態");
		List<List<Object>> rows = List.of(
				List.of("ORD-2026-E001", "陳建國", BigDecimal.valueOf(12500.00), 2, LocalDate.of(2026, 11, 1), "PAID"),
				List.of("ORD-2026-E002", "周淑珍", BigDecimal.valueOf(-500.00), 1, LocalDate.of(2026, 11, 2), "PENDING"),
				List.of("ORD-2026-E003", "黃家豪", BigDecimal.valueOf(8800.00), 15000, LocalDate.of(2026, 11, 3), "PAID"),
				List.of("ORD-2026-E004", "趙子儀", BigDecimal.valueOf(0.00), 0, LocalDate.of(2026, 11, 4), "CANCELLED"),
				List.of("ORD-2026-E005", "林小明", BigDecimal.valueOf(3600.00), 4, LocalDate.of(2026, 11, 5), "SHIPPED")
		);

		WriteSheetCommand sheet = WriteSheetCommand.builder()
				.sheetName("訂單匯入清單")
				.headers(headers)
				.rows(rows)
				.freezeHeader(true)
				.headerFillColor("C65911")
				.headerFontColor("FFFFFF")
				.build();

		return excelWriterPort.write(WriteExcelCommand.of(sheet));
	}

	private byte[] generateProductSuccessExcel() {
		List<String> headers = List.of("商品代碼", "商品名稱", "商品售價", "在售狀態");
		List<List<Object>> rows = List.of(
				List.of("PROD-A001", "4K 專業級螢幕", BigDecimal.valueOf(48900.00), true),
				List.of("PROD-A002", "人體工學機械鍵盤", BigDecimal.valueOf(6200.00), true),
				List.of("PROD-A003", "三模降噪耳塞式耳機", BigDecimal.valueOf(12800.00), true),
				List.of("PROD-A004", "鋁合金筆電支架", BigDecimal.valueOf(8500.00), false),
				List.of("PROD-A005", "多功能擴充收納包", BigDecimal.valueOf(2980.00), true)
		);

		WriteSheetCommand sheet = WriteSheetCommand.builder()
				.sheetName("商品主檔清單")
				.headers(headers)
				.rows(rows)
				.freezeHeader(true)
				.headerFillColor("2F5597")
				.headerFontColor("FFFFFF")
				.build();

		return excelWriterPort.write(WriteExcelCommand.of(sheet));
	}

	private byte[] generateProductErrorsExcel() {
		List<String> headers = List.of("商品代碼", "商品名稱", "商品售價", "在售狀態");
		List<List<Object>> rows = List.of(
				List.of("PROD-E001", "標準無人機套件", BigDecimal.valueOf(3500.00), true),
				List.of("PROD-E002", "免費贈品收納套", BigDecimal.valueOf(0.00), true),
				List.of("PROD-E003", "負值回饋折價券", BigDecimal.valueOf(-1200.00), false),
				List.of("PROD-E004", "高階手持穩定器", BigDecimal.valueOf(4200.00), true)
		);

		WriteSheetCommand sheet = WriteSheetCommand.builder()
				.sheetName("商品主檔清單")
				.headers(headers)
				.rows(rows)
				.freezeHeader(true)
				.headerFillColor("C65911")
				.headerFontColor("FFFFFF")
				.build();

		return excelWriterPort.write(WriteExcelCommand.of(sheet));
	}

	private void writeFile(Path path, byte[] content) throws IOException {
		Files.write(path, content);
	}
}
