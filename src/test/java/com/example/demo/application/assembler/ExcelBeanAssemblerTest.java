package com.example.demo.application.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.annotation.ExcelColumn;
import com.example.demo.application.annotation.ExcelSheet;
import com.example.demo.application.port.out.ExcelReaderPort;
import com.example.demo.application.port.out.ExcelWriterPort;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.dto.ExcelGottenResult;
import com.example.demo.application.shared.dto.ExcelSheetGottenResult;
import com.example.demo.infrastructure.adapter.FastExcelReaderAdapter;
import com.example.demo.infrastructure.adapter.FastExcelWriterAdapter;
import com.example.demo.infrastructure.adapter.PoiSaxReaderAdapter;
import com.example.demo.infrastructure.adapter.PoiSxssfWriterAdapter;

@DisplayName("ExcelBeanAssembler 宣告式物件映射與 Map 相容性測試")
class ExcelBeanAssemblerTest {

	// =========================================================================
	// 測試用宣告式模型定義 (POJO 與 Java 21 Record)
	// =========================================================================

	public enum OrderStatus {
		PAID, SHIPPED, CANCELLED
	}

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
			@ExcelColumn(name = "商品代碼", index = 0)
			String productCode,

			@ExcelColumn(name = "商品名稱", index = 1)
			String productName,

			@ExcelColumn(name = "售價", index = 2, format = "#,##0.00")
			BigDecimal price,

			@ExcelColumn(name = "是否上架", index = 3)
			boolean active,

			@ExcelColumn(name = "建立時間", index = 4, format = "yyyy-MM-dd HH:mm:ss")
			LocalDateTime createTime
	) {}

	// =========================================================================
	// 測試案例
	// =========================================================================

	/**
	 * Feature: JavaBean 宣告式匯出與匯入 (FastExcel 引擎)
	 * Scenario: 匯出 OrderPojoModel 清單為 Excel 並透過 readBeans 還原為強型別物件
	 * Given: 建立包含 2 筆訂單之 OrderPojoModel 清單
	 * When: 調用 FastExcelWriterAdapter.writeBeans 產製 byte[] 並使用 FastExcelReaderAdapter.readBeans 解析
	 * Then: 成功還原 2 筆物件，所有屬性型別 (String, BigDecimal, int, LocalDate, Enum) 與數值皆正確
	 */
	@Test
	@DisplayName("情境：FastExcel 引擎下 JavaBean 宣告式匯出與匯入驗證")
	void shouldWriteAndReadBeansWithFastExcelSuccessfully() {
		// Given
		ExcelWriterPort writer = new FastExcelWriterAdapter();
		ExcelReaderPort reader = new FastExcelReaderAdapter();

		List<OrderPojoModel> orders = List.of(
				new OrderPojoModel("ORD-2026-001", "張小明", new BigDecimal("12500.50"), 2, LocalDate.of(2026, 11, 1), OrderStatus.PAID),
				new OrderPojoModel("ORD-2026-002", null, new BigDecimal("8800.00"), 1, LocalDate.of(2026, 11, 2), OrderStatus.SHIPPED)
		);

		// When
		byte[] excelBytes = writer.writeBeans(orders, OrderPojoModel.class);
		List<OrderPojoModel> restoredOrders = reader.readBeans(excelBytes, OrderPojoModel.class);

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
	 * Feature: Java 21 Record 宣告式匯出與匯入 (Apache POI 引擎)
	 * Scenario: 匯出 ProductRecordModel 清單為 Excel 並透過 readBeans 還原為不可變 Record
	 * Given: 建立包含 2 筆商品之 ProductRecordModel 清單
	 * When: 調用 PoiSxssfWriterAdapter.writeBeans 產製 byte[] 並使用 PoiSaxReaderAdapter.readBeans 解析
	 * Then: 成功還原 2 筆 Record 實例，各欄位數值與格式化字串還原正確
	 */
	@Test
	@DisplayName("情境：Apache POI 引擎下 Java 21 Record 宣告式匯出與匯入驗證")
	void shouldWriteAndReadRecordsWithApachePoiSuccessfully() {
		// Given
		ExcelWriterPort writer = new PoiSxssfWriterAdapter();
		ExcelReaderPort reader = new PoiSaxReaderAdapter();

		LocalDateTime now = LocalDateTime.of(2026, 11, 1, 10, 30, 0);
		List<ProductRecordModel> products = List.of(
				new ProductRecordModel("PROD-001", "4K 專業航拍無人機", new BigDecimal("35000.00"), true, now),
				new ProductRecordModel("PROD-002", "高容量鋰電池模組", new BigDecimal("4500.00"), false, now.minusDays(1))
		);

		// When
		byte[] excelBytes = writer.writeBeans(products, ProductRecordModel.class);
		List<ProductRecordModel> restoredProducts = reader.readBeans(excelBytes, ProductRecordModel.class);

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
	 * Scenario: 驗證寫入 Map 資料並使用 getDataRowsAsMaps 讀取，同時支援轉換為 Bean
	 * Given: 建立包含 2 筆以 Map 表示的訂單資料
	 * When: 調用 writer.writeMaps 寫入，並透過 reader.readFirstSheet 讀取
	 * Then: 能正常使用 getDataRowsAsMaps 取得 Map 清單，且可使用 getDataRowsAsBeans 轉換為 OrderPojoModel
	 */
	@Test
	@DisplayName("情境：Map 結構寫入與 getDataRowsAsMaps / getDataRowsAsBeans 雙軌相容驗證")
	void shouldRetainMapMethodsAndSupportBidirectionalConversion() {
		// Given
		ExcelWriterPort writer = new FastExcelWriterAdapter();
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

		// When - 1: 使用 writeMaps 寫入 Excel
		byte[] excelBytes = writer.writeMaps("Map訂單表", headers, mapRows);

		// When - 2: 使用 readFirstSheet 讀取工作表
		ExcelSheetGottenResult sheet = reader.readFirstSheet(excelBytes);

		// Then - 1: 驗證原先的 Map 方法 getDataRowsAsMaps 完整運作
		List<Map<String, Object>> readMaps = sheet.getDataRowsAsMaps();
		assertThat(readMaps).hasSize(2);
		assertThat(readMaps.get(0).get("訂單編號")).isEqualTo("MAP-001");
		assertThat(readMaps.get(0).get("訂購人")).isEqualTo("李大仁");
		assertThat(readMaps.get(1).get("訂單編號")).isEqualTo("MAP-002");
		assertThat(readMaps.get(1).get("訂購人")).isEqualTo("程又青");

		// Then - 2: 驗證工作表可直接調用 getDataRowsAsBeans 轉為強型別 Model
		List<OrderPojoModel> beans = sheet.getDataRowsAsBeans(OrderPojoModel.class);
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
}

