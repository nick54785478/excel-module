package com.example.demo.application.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.annotation.mapping.ExcelColumn;
import com.example.demo.application.annotation.mapping.ExcelSheet;
import com.example.demo.application.port.out.core.ExcelReaderPort;
import com.example.demo.application.port.out.core.ExcelWriterPort;
import com.example.demo.application.service.core.ExcelCommandService;
import com.example.demo.application.service.core.ExcelQueryService;
import com.example.demo.application.shared.dto.ExcelSheetGottenResult;
import com.example.demo.application.shared.model.BomItemExcelModel;
import com.example.demo.application.shared.model.MaterialExcelModel;
import com.example.demo.infrastructure.adapter.core.FastExcelReaderAdapter;
import com.example.demo.infrastructure.adapter.core.FastExcelWriterAdapter;
import com.example.demo.infrastructure.adapter.core.PoiSaxReaderAdapter;
import com.example.demo.infrastructure.adapter.core.PoiSxssfWriterAdapter;

@DisplayName("ExcelBeanAssembler 宣告式物件映射與 Application Service 流程測試")
class ExcelBeanAssemblerTest {

	// =========================================================================
	// 測試用宣告式模型定義 (POJO 與 Java 21 Record)
	// =========================================================================

	public enum OrderStatus {
		PAID, SHIPPED, CANCELLED
	}

	/**
	 * 維度五宣告式公式與工作表防護測試模型
	 */
	@ExcelSheet(
			name = "毛利分析",
			protect = true,
			protectPassword = "SecretMargin2026",
			leftHeader = "【內部審計】",
			centerHeader = "毛利分析報表",
			centerFooter = "第 &P 頁 / 共 &N 頁"
	)
	public record MarginAnalysisRecord(
			@ExcelColumn(name = "料號", index = 0) String itemCode,
			@ExcelColumn(name = "單價", index = 1) BigDecimal price,
			@ExcelColumn(name = "數量", index = 2) Integer quantity,
			@ExcelColumn(name = "小計", index = 3, formula = "B{row}*C{row}") BigDecimal subtotal
	) {}

	/**
	 * 標準 JavaBean 測試模型
	 */
	@ExcelSheet(name = "訂單匯入清單")
	public static class OrderPojoModel {

		@ExcelColumn(name = "訂單編號", index = 0)
		private String orderNo;

		@ExcelColumn(name = "訂購人", index = 1, defaultValue = "匿名訪客")
		private String customerName;

		@ExcelColumn(name = "購買金額", index = 2, format = "#,##0.00")
		private BigDecimal amount;

		@ExcelColumn(name = "數量", index = 3)
		private int quantity;

		@ExcelColumn(name = "下單日期", index = 4, format = "yyyy-MM-dd")
		private LocalDate orderDate;

		@ExcelColumn(name = "訂單狀態", index = 5)
		private OrderStatus status;

		public OrderPojoModel() {
		}

		public OrderPojoModel(String orderNo, String customerName, BigDecimal amount, int quantity,
				LocalDate orderDate, OrderStatus status) {
			this.orderNo = orderNo;
			this.customerName = customerName;
			this.amount = amount;
			this.quantity = quantity;
			this.orderDate = orderDate;
			this.status = status;
		}

		public String getOrderNo() {
			return orderNo;
		}

		public void setOrderNo(String orderNo) {
			this.orderNo = orderNo;
		}

		public String getCustomerName() {
			return customerName;
		}

		public void setCustomerName(String customerName) {
			this.customerName = customerName;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}

		public int getQuantity() {
			return quantity;
		}

		public void setQuantity(int quantity) {
			this.quantity = quantity;
		}

		public LocalDate getOrderDate() {
			return orderDate;
		}

		public void setOrderDate(LocalDate orderDate) {
			this.orderDate = orderDate;
		}

		public OrderStatus getStatus() {
			return status;
		}

		public void setStatus(OrderStatus status) {
			this.status = status;
		}
	}

	/**
	 * Java 21 Record 測試模型
	 */
	@ExcelSheet("商品主檔清單")
	public record ProductRecordModel(
			@ExcelColumn(name = "商品代碼", index = 0) String productCode,

			@ExcelColumn(name = "商品名稱", index = 1) String productName,

			@ExcelColumn(name = "售價", index = 2, format = "#,##0.00") BigDecimal price,

			@ExcelColumn(name = "是否上架", index = 3) boolean active,

			@ExcelColumn(name = "建立時間", index = 4, format = "yyyy-MM-dd HH:mm:ss") LocalDateTime createTime) {
	}

	// =========================================================================
	// 測試案例
	// =========================================================================

	/**
	 * Feature: JavaBean 宣告式匯出與匯入 (Clean Architecture 調度流程 - FastExcel 引擎)
	 * Scenario: 透過 ExcelCommandService.exportBeans 產製 Excel，並透過
	 * ExcelQueryService.importBeans 還原為強型別物件
	 * Given: 建立包含 2 筆訂單之 OrderPojoModel 清單
	 * When: Application Service 調用 Assembler 轉為 Command 後由 FastExcelWriterAdapter
	 * 產出 byte[]，再經由 FastExcelReaderAdapter 與 Assembler 還原
	 * Then: 成功還原 2 筆物件，所有屬性型別 (String, BigDecimal, int, LocalDate, Enum) 與預設值皆正確
	 */
	@Test
	@DisplayName("情境：FastExcel 引擎下 JavaBean 宣告式匯出與匯入 (經由 ApplicationService 調度)")
	void shouldWriteAndReadBeansWithFastExcelSuccessfully() {
		// Given
		ExcelCommandService exportService = new ExcelCommandService(new FastExcelWriterAdapter());
		ExcelQueryService importService = new ExcelQueryService(new FastExcelReaderAdapter());

		List<OrderPojoModel> orders = List.of(
				new OrderPojoModel("ORD-2026-001", "張小明", new BigDecimal("12500.50"), 2, LocalDate.of(2026, 11, 1),
						OrderStatus.PAID),
				new OrderPojoModel("ORD-2026-002", null, new BigDecimal("8800.00"), 1, LocalDate.of(2026, 11, 2),
						OrderStatus.SHIPPED));

		// When
		byte[] excelBytes = exportService.exportBeans(orders, OrderPojoModel.class);
		List<OrderPojoModel> restoredOrders = importService.importBeans(new ByteArrayInputStream(excelBytes),
				OrderPojoModel.class);

		// Then
		assertThat(restoredOrders).hasSize(2);

		OrderPojoModel order1 = restoredOrders.get(0);
		assertThat(order1.getOrderNo()).isEqualTo("ORD-2026-001");
		assertThat(order1.getCustomerName()).isEqualTo("張小明");
		assertThat(order1.getAmount()).isEqualByComparingTo("12500.50");
		assertThat(order1.getQuantity()).isEqualTo(2);
		assertThat(order1.getOrderDate()).isEqualTo(LocalDate.of(2026, 11, 1));
		assertThat(order1.getStatus()).isEqualTo(OrderStatus.PAID);

		// 驗證預設值 defaultValue = "匿名訪客"
		OrderPojoModel order2 = restoredOrders.get(1);
		assertThat(order2.getOrderNo()).isEqualTo("ORD-2026-002");
		assertThat(order2.getCustomerName()).isEqualTo("匿名訪客");
		assertThat(order2.getAmount()).isEqualByComparingTo("8800.00");
		assertThat(order2.getQuantity()).isEqualTo(1);
		assertThat(order2.getStatus()).isEqualTo(OrderStatus.SHIPPED);
	}

	/**
	 * Feature: Java 21 Record 宣告式匯出與匯入 (Clean Architecture 調度流程 - Apache POI 引擎)
	 * Scenario: 透過 ExcelCommandService.exportBeans 匯出 Record 清單，並透過
	 * ExcelQueryService.importBeans 還原為不可變 Record
	 * Given: 建立包含 2 筆商品之 ProductRecordModel 清單
	 * When: Application Service 調度 PoiSxssfWriterAdapter 產製 byte[] 並調度
	 * PoiSaxReaderAdapter 解析
	 * Then: 成功還原 2 筆 Record 實例，各欄位數值與格式化字串還原正確
	 */
	@Test
	@DisplayName("情境：Apache POI 引擎下 Java 21 Record 宣告式匯出與匯入 (經由 ApplicationService 調度)")
	void shouldWriteAndReadRecordsWithApachePoiSuccessfully() {
		// Given
		ExcelCommandService exportService = new ExcelCommandService(new PoiSxssfWriterAdapter());
		ExcelQueryService importService = new ExcelQueryService(new PoiSaxReaderAdapter());

		LocalDateTime now = LocalDateTime.of(2026, 11, 1, 10, 30, 0);
		List<ProductRecordModel> products = List.of(
				new ProductRecordModel("PROD-001", "4K 專業航拍無人機", new BigDecimal("35000.00"), true, now),
				new ProductRecordModel("PROD-002", "高容量鋰電池模組", new BigDecimal("4500.00"), false, now.minusDays(1)));

		// When
		byte[] excelBytes = exportService.exportBeans(products, ProductRecordModel.class);
		List<ProductRecordModel> restoredProducts = importService.importBeans(new ByteArrayInputStream(excelBytes),
				ProductRecordModel.class);

		// Then
		assertThat(restoredProducts).hasSize(2);

		ProductRecordModel prod1 = restoredProducts.get(0);
		assertThat(prod1.productCode()).isEqualTo("PROD-001");
		assertThat(prod1.productName()).isEqualTo("4K 專業航拍無人機");
		assertThat(prod1.price()).isEqualByComparingTo("35000.00");
		assertThat(prod1.active()).isTrue();
		assertThat(prod1.createTime()).isEqualTo(now);

		ProductRecordModel prod2 = restoredProducts.get(1);
		assertThat(prod2.productCode()).isEqualTo("PROD-002");
		assertThat(prod2.productName()).isEqualTo("高容量鋰電池模組");
		assertThat(prod2.price()).isEqualByComparingTo("4500.00");
		assertThat(prod2.active()).isFalse();
	}

	/**
	 * Feature: Map 字典結構相容性與 getDataRowsAsMaps 方法保留驗證
	 * Scenario: 驗證寫入 Map 資料並使用 getDataRowsAsMaps 讀取，同時支援由 Assembler 轉換為 Bean
	 * Given: 建立包含 2 筆以 Map 表示的訂單資料
	 * When: 調用 writer.writeMaps 寫入，並透過 reader.readFirstSheet 讀取純資料載體
	 * Then: 能正常使用 getDataRowsAsMaps 取得 Map 清單，且可經由 ExcelBeanAssembler.toBeanList
	 * 轉換為 OrderPojoModel
	 */
	@Test
	@DisplayName("情境：Map 結構寫入與 getDataRowsAsMaps / Assembler 轉 Bean 雙軌相容驗證")
	void shouldRetainMapMethodsAndSupportBidirectionalConversion() {
		// Given
		ExcelCommandService exportService = new ExcelCommandService(new FastExcelWriterAdapter());
		ExcelReaderPort reader = new FastExcelReaderAdapter();

		List<String> headers = List.of("訂單編號", "訂購人", "購買金額", "數量", "下單日期", "訂單狀態");
		List<Map<String, Object>> mapRows = new ArrayList<>();

		Map<String, Object> row1 = new LinkedHashMap<>();
		row1.put("訂單編號", "MAP-001");
		row1.put("訂購人", "李大仁");
		row1.put("購買金額", 6600.00);
		row1.put("數量", 3);
		row1.put("下單日期", "2026-11-01");
		row1.put("訂單狀態", "PAID");
		mapRows.add(row1);

		Map<String, Object> row2 = new LinkedHashMap<>();
		row2.put("訂單編號", "MAP-002");
		row2.put("訂購人", "程又青");
		row2.put("購買金額", 9900.50);
		row2.put("數量", 1);
		row2.put("下單日期", "2026-11-02");
		row2.put("訂單狀態", "SHIPPED");
		mapRows.add(row2);

		// When - 1: 使用 ApplicationService.exportMaps (調用 Assembler) 寫入 Excel
		byte[] excelBytes = exportService.exportMaps("Map訂單表", headers, mapRows);

		// When - 2: 使用 readFirstSheet 讀取工作表純資料載體
		ExcelSheetGottenResult sheet = reader.readFirstSheet(excelBytes);

		// Then - 1: 驗證純資料載體保留的 Map 方法 getDataRowsAsMaps 完整運作
		List<Map<String, Object>> readMaps = sheet.getDataRowsAsMaps();
		assertThat(readMaps).hasSize(2);
		assertThat(readMaps.get(0).get("訂單編號")).isEqualTo("MAP-001");
		assertThat(readMaps.get(0).get("訂購人")).isEqualTo("李大仁");
		assertThat(readMaps.get(1).get("訂單編號")).isEqualTo("MAP-002");
		assertThat(readMaps.get(1).get("訂購人")).isEqualTo("程又青");

		// Then - 2: 由 Assembler 負責將純資料載體轉換為強型別 Model (符合 Clean Architecture 職責分離)
		List<OrderPojoModel> beans = ExcelBeanAssembler.toBeanList(sheet, OrderPojoModel.class);
		assertThat(beans).hasSize(2);
		assertThat(beans.get(0).getOrderNo()).isEqualTo("MAP-001");
		assertThat(beans.get(0).getCustomerName()).isEqualTo("李大仁");
		assertThat(beans.get(0).getAmount()).isEqualByComparingTo("6600.00");
		assertThat(beans.get(0).getQuantity()).isEqualTo(3);
		assertThat(beans.get(0).getStatus()).isEqualTo(OrderStatus.PAID);
	}

	/**
	 * Feature: ExcelBeanAssembler 雙向 Map $\leftrightarrow$ Bean 記憶體轉換
	 * Scenario: 驗證 toMaps 與 fromMaps 之相互組裝
	 * Given: 建立包含 1 筆訂單之 OrderPojoModel
	 * When: 調用 ExcelBeanAssembler.toMaps 轉為 Map，再調用 fromMaps 還原
	 * Then: 還原之物件內容與原物件一致
	 */
	@Test
	@DisplayName("情境：ExcelBeanAssembler 內部 toMaps 與 fromMaps 雙向轉換正確")
	void shouldConvertBetweenBeansAndMapsBidirectionally() {
		// Given
		OrderPojoModel original = new OrderPojoModel("ORD-BIDIR-001", "王小美", new BigDecimal("1999.00"), 5,
				LocalDate.of(2026, 11, 1), OrderStatus.PAID);

		// When
		List<Map<String, Object>> maps = ExcelBeanAssembler.toMaps(List.of(original), OrderPojoModel.class);
		assertThat(maps).hasSize(1);
		assertThat(maps.get(0).get("訂單編號")).isEqualTo("ORD-BIDIR-001");
		assertThat(maps.get(0).get("訂購人")).isEqualTo("王小美");

		List<OrderPojoModel> restored = ExcelBeanAssembler.fromMaps(maps, OrderPojoModel.class);

		// Then
		assertThat(restored).hasSize(1);
		assertThat(restored.get(0).getOrderNo()).isEqualTo("ORD-BIDIR-001");
		assertThat(restored.get(0).getCustomerName()).isEqualTo("王小美");
		assertThat(restored.get(0).getAmount()).isEqualByComparingTo("1999.00");
		assertThat(restored.get(0).getQuantity()).isEqualTo(5);
		assertThat(restored.get(0).getStatus()).isEqualTo(OrderStatus.PAID);
	}

	/**
	 * Feature: Excel 數值日期序號 (Serial Number / OADate) 解析
	 * Scenario: 驗證 ExcelBeanAssembler 對浮點數日期與日期時間序號之精準還原
	 * Given: 傳入 Excel 日期序號 46327.0 (對應 2026-11-01) 與 46327.5 (對應 2026-11-01 12:00:00)
	 * When: 調用 parseExcelSerialDate 與 parseExcelSerialDateTime
	 * Then: 成功還原為正確之 LocalDate 與 LocalDateTime
	 */
	@Test
	@DisplayName("情境：Excel 浮點數日期序號 (46327.0) 精準還原為 LocalDate 與 LocalDateTime")
	void shouldParseExcelDateSerialNumbersCorrectly() {
		// Given & When
		LocalDate date = ExcelBeanAssembler.parseExcelSerialDate(46327.0);
		LocalDateTime dateTime = ExcelBeanAssembler.parseExcelSerialDateTime(46327.5);

		// Then
		assertThat(date).isEqualTo(LocalDate.of(2026, 11, 1));
		assertThat(dateTime).isEqualTo(LocalDateTime.of(2026, 11, 1, 12, 0, 0));
	}

	/**
	 * Feature: 試算表讀取包含 Excel 日期序號字串與數值時之宣告式組裝
	 * Scenario: 驗證儲存格包含 "46327.0" 字串或 46327.0 浮點數時，toBeanList 能正確轉型為 LocalDate
	 * Given: 建立包含 "46327.0" 與 46328.0 的 ExcelSheetGottenResult
	 * When: 調用 ExcelBeanAssembler.toBeanList 轉為 OrderPojoModel
	 * Then: 成功轉換，orderDate 正確解析為 2026-11-01 與 2026-11-02，不拋出 DateTimeParseException
	 */
	@Test
	@DisplayName("情境：儲存格包含日期序號字串與數值時，toBeanList 正常組裝不拋錯")
	void shouldAssembleBeansWithExcelSerialDateStringsAndNumbers() {
		// Given
		List<String> headers = List.of("訂單編號", "訂購人", "購買金額", "數量", "下單日期", "訂單狀態");
		List<List<Object>> rows = List.of(
				List.of("ORD-S01", "張三", 1000.0, 1, "46327.0", "PAID"),
				List.of("ORD-S02", "李四", 2000.0, 2, 46328.0, "PAID")
		);
		List<List<String>> textRows = List.of(
				List.of("ORD-S01", "張三", "1000.0", "1", "46327.0", "PAID"),
				List.of("ORD-S02", "李四", "2000.0", "2", "46328.0", "PAID")
		);
		ExcelSheetGottenResult sheet = new ExcelSheetGottenResult(0, "訂單匯入清單", headers, rows, textRows);

		// When
		List<OrderPojoModel> beans = ExcelBeanAssembler.toBeanList(sheet, OrderPojoModel.class);

		// Then
		assertThat(beans).hasSize(2);
		assertThat(beans.get(0).getOrderDate()).isEqualTo(LocalDate.of(2026, 11, 1));
		assertThat(beans.get(1).getOrderDate()).isEqualTo(LocalDate.of(2026, 11, 2));
	}

	/**
	 * Feature: 容錯組裝管線與型別轉換錯誤收集 (toBeanListWithErrors)
	 * Scenario: 當儲存格包含非法的字串數值 (如無法轉換為數值或日期) 時，主動收集錯誤而非拋出中斷例外
	 * Given: 包含違規字串 "INVALID_AMOUNT" 與 "NOT_A_DATE" 的儲存格列
	 * When: 調用 toBeanListWithErrors
	 * Then: 收集到型別轉換錯誤事實，不拋出 500/異常例外
	 */
	@Test
	@DisplayName("情境：儲存格包含不合法格式時，toBeanListWithErrors 正確收集錯誤事實")
	void shouldCollectConversionErrorsGracefullyInToBeanListWithErrors() {
		// Given
		List<String> headers = List.of("訂單編號", "訂購人", "購買金額", "數量", "下單日期", "訂單狀態");
		List<List<Object>> rows = List.of(
				List.of("ORD-ERR-1", "王五", "INVALID_AMOUNT", 1, "NOT_A_DATE", "PAID"),
				List.of("ORD-OK-1", "趙六", 1500.0, 3, "2026-11-05", "PAID")
		);
		List<List<String>> textRows = List.of(
				List.of("ORD-ERR-1", "王五", "INVALID_AMOUNT", "1", "NOT_A_DATE", "PAID"),
				List.of("ORD-OK-1", "趙六", "1500.0", "3", "2026-11-05", "PAID")
		);
		ExcelSheetGottenResult sheet = new ExcelSheetGottenResult(0, "訂單匯入清單", headers, rows, textRows);

		// When
		ExcelBeanAssembler.BeanAssemblyResult<OrderPojoModel> result =
				ExcelBeanAssembler.toBeanListWithErrors(sheet, OrderPojoModel.class);

		// Then
		assertThat(result.hasErrors()).isTrue();
		assertThat(result.conversionErrors()).hasSize(2); // 購買金額與下單日期轉型失敗
		assertThat(result.items()).hasSize(1);            // 第 2 列完全合格實例化
		assertThat(result.items().get(0).getOrderNo()).isEqualTo("ORD-OK-1");
	}

	/**
	 * Feature: 多工作表 BOM 活頁簿智慧探測與容錯解析 (BOM 配方)
	 * Scenario: 上傳包含「多階層BOM展開清單」、「單階BOM匯入清單」與「物料主檔對照表」之活頁簿，
	 * 自動智慧路由至「單階BOM匯入清單」並透過別名相容解析為 BomItemExcelModel 清單
	 * Given: 讀取 doc/template/BOM_IMPORT.xlsx
	 * When: 調用 ExcelQueryService.importBeans(is, BomItemExcelModel.class)
	 * Then: 智慧略過多階展開表，成功解析單階 BOM 配方清單 16 筆資料，各欄位數值與別名對應正確
	 */
	@Test
	@DisplayName("場景：多工作表 BOM 活頁簿自動探測匹配單階 BOM 配方清單並以別名解析")
	void shouldImportBomItemsFromMultiSheetWorkbookWithSmartDetection() throws Exception {
		// Given
		ExcelQueryService importService = new ExcelQueryService(new FastExcelReaderAdapter());

		// When
		List<BomItemExcelModel> models;
		try (FileInputStream fis = new FileInputStream("doc/template/BOM_IMPORT.xlsx")) {
			models = importService.importBeans(fis, BomItemExcelModel.class);
		}

		// Then
		assertThat(models).isNotEmpty();
		assertThat(models).hasSize(16); // 16 筆單階 BOM 配方

		BomItemExcelModel firstItem = models.get(0);
		assertThat(firstItem.getParentMaterialCode()).isEqualTo("MAT-FG-001");
		assertThat(firstItem.getBomVersion()).isEqualTo("V1.0");
		assertThat(firstItem.getItemSequence()).isEqualTo(10);
		assertThat(firstItem.getChildMaterialCode()).isEqualTo("MAT-SFG-001");
		assertThat(firstItem.getChildMaterialName()).isEqualTo("飛控主板電路模組總成");
		assertThat(firstItem.getQuantity()).isEqualByComparingTo("1.0");
		assertThat(firstItem.getUnit()).isEqualTo("PCS"); // 套用 defaultValue
		assertThat(firstItem.getScrapRate()).isEqualByComparingTo("0.0");
		assertThat(firstItem.getPositionCode()).isEqualTo("FC-ASM");
		assertThat(firstItem.getRemark()).isEqualTo("核心飛控組裝件");
	}

	/**
	 * Feature: 多工作表 BOM 活頁簿智慧探測與容錯解析 (物料主檔)
	 * Scenario: 上傳多工作表活頁簿，自動探測並路由至「物料主檔對照表」解析為 MaterialExcelModel 清單
	 * Given: 讀取 doc/template/BOM_IMPORT.xlsx
	 * When: 調用 ExcelQueryService.importBeans(is, MaterialExcelModel.class)
	 * Then: 自動匹配第三張工作表，成功解析 19 筆物料主檔資料
	 */
	@Test
	@DisplayName("場景：多工作表 BOM 活頁簿自動探測匹配物料主檔對照表並解析")
	void shouldImportMaterialsFromMultiSheetWorkbookWithSmartDetection() throws Exception {
		// Given
		ExcelQueryService importService = new ExcelQueryService(new FastExcelReaderAdapter());

		// When
		List<MaterialExcelModel> models;
		try (FileInputStream fis = new FileInputStream("doc/template/BOM_IMPORT.xlsx")) {
			models = importService.importBeans(fis, MaterialExcelModel.class);
		}

		// Then
		assertThat(models).isNotEmpty();
		assertThat(models).hasSize(19); // 19 筆物料

		MaterialExcelModel firstMat = models.get(0);
		assertThat(firstMat.getMaterialCode()).isEqualTo("MAT-FG-001");
		assertThat(firstMat.getMaterialName()).isEqualTo("4K 專業商用巡檢無人機");
		assertThat(firstMat.getSpecification()).isEqualTo("PRO-4K-GPS-V2");
		assertThat(firstMat.getCategory()).isEqualTo("成品 (FINISHED_GOODS)");
		assertThat(firstMat.getUnit()).isEqualTo("SET");
		assertThat(firstMat.getStandardCost()).isEqualByComparingTo("28500.0");
		assertThat(firstMat.getStatus()).isEqualTo("正常使用中 (ACTIVE)");
	}

	/**
	 * Feature: 宣告式公式替換與工作表防護設定解析
	 * Scenario: 透過 @ExcelColumn(formula = "...") 與 @ExcelSheet(protectPassword = "...") 自動產製 WriteSheetCommand
	 * Given: 建立包含 MarginAnalysisRecord 清單
	 * When: 呼叫 ExcelBeanAssembler.toSheetCommand(list, MarginAnalysisRecord.class, null)
	 * Then: 驗證產出的 WriteSheetCommand 包含自動動態替換後的 cellFormulas ("D2" -> "B2*C2", "D3" -> "B3*C3")
	 *       並驗證 protectionConfig 密碼與 headerFooterConfig
	 */
	@Test
	@DisplayName("場景：宣告式 @ExcelColumn 動態公式與 @ExcelSheet 防護密碼自動組裝")
	void shouldAssembleDeclarativeFormulasAndProtectionConfig() {
		// Given
		List<MarginAnalysisRecord> list = List.of(
				new MarginAnalysisRecord("ITM-01", new BigDecimal("120.00"), 5, null),
				new MarginAnalysisRecord("ITM-02", new BigDecimal("80.00"), 10, null)
		);

		// When
		com.example.demo.application.shared.command.WriteSheetCommand command =
				ExcelBeanAssembler.toSheetCommand(list, MarginAnalysisRecord.class, null);

		// Then
		assertThat(command.sheetName()).isEqualTo("毛利分析");
		assertThat(command.headers()).containsExactly("料號", "單價", "數量", "小計");
		assertThat(command.rows()).hasSize(2);

		// 驗證公式動態替換：第 1 筆在第 2 列 (D2 = B2*C2)，第 2 筆在第 3 列 (D3 = B3*C3)
		assertThat(command.cellFormulas()).containsEntry("D2", "B2*C2");
		assertThat(command.cellFormulas()).containsEntry("D3", "B3*C3");

		// 驗證工作表防護配置
		assertThat(command.protectionConfig()).isNotNull();
		assertThat(command.protectionConfig().password()).isEqualTo("SecretMargin2026");

		// 驗證頁首頁尾配置
		assertThat(command.headerFooterConfig()).isNotNull();
		assertThat(command.headerFooterConfig().leftHeader()).isEqualTo("【內部審計】");
		assertThat(command.headerFooterConfig().centerHeader()).isEqualTo("毛利分析報表");
		assertThat(command.headerFooterConfig().centerFooter()).isEqualTo("第 &P 頁 / 共 &N 頁");
	}
}
