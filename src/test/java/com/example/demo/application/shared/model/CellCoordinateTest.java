package com.example.demo.application.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CellCoordinate 與 CellRange 值物件單元測試 (Unit Tests)
 * <p>
 * 遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 語法註解規範。
 */
@DisplayName("CellCoordinate 與 CellRange 值物件測試")
class CellCoordinateTest {

	/**
	 * Feature: 儲存格座標解析與轉譯
	 * Scenario: 驗證標準 Excel 座標參照文字與 0-based 陣列索引雙向解析
	 * Given: 座標文字 "A1", "D15", "AA100"
	 * When: 呼叫 CellCoordinate.parse
	 * Then: 正確解析出 row 與 col，且 toCellReference 能完整還原
	 */
	@Test
	@DisplayName("情境：CellCoordinate 雙向轉換正確性驗證")
	void shouldParseAndFormatCoordinatesCorrectly() {
		// A1 -> col 0, row 0
		CellCoordinate a1 = CellCoordinate.parse("A1");
		assertThat(a1.col()).isEqualTo(0);
		assertThat(a1.row()).isEqualTo(0);
		assertThat(a1.toCellReference()).isEqualTo("A1");

		// D15 -> col 3, row 14
		CellCoordinate d15 = CellCoordinate.parse("d15");
		assertThat(d15.col()).isEqualTo(3);
		assertThat(d15.row()).isEqualTo(14);
		assertThat(d15.toCellReference()).isEqualTo("D15");

		// AA100 -> col 26, row 99
		CellCoordinate aa100 = CellCoordinate.parse("AA100");
		assertThat(aa100.col()).isEqualTo(26);
		assertThat(aa100.row()).isEqualTo(99);
		assertThat(aa100.toCellReference()).isEqualTo("AA100");

		// 格式化驗證
		assertThat(CellCoordinate.format(3, 14)).isEqualTo("D15");
	}

	/**
	 * Feature: 儲存格座標無效格式防禦與邊界檢驗
	 * Scenario: 傳入空值、負數、過長字串 (如 Swagger dummy 佔位符 additionalProp1) 或超出 Excel 限制座標時拋出異常
	 * Given: 無效或越界座標字串
	 * When: 呼叫 CellCoordinate.parse 或 CellCoordinate.isValid
	 * Then: parse 拋出 IllegalArgumentException，isValid 回傳 false
	 */
	@Test
	@DisplayName("情境：CellCoordinate 防禦性檢驗與 isValid 驗證")
	void shouldThrowExceptionOnInvalidCoordinates() {
		assertThatThrownBy(() -> CellCoordinate.parse(null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> CellCoordinate.parse(""))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> CellCoordinate.parse("123"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> CellCoordinate.parse("ABC"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> CellCoordinate.parse("additionalProp1"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> CellCoordinate.parse("XFE1"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> CellCoordinate.parse("A0"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> CellCoordinate.parse("A1048577"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new CellCoordinate(-1, 0))
				.isInstanceOf(IllegalArgumentException.class);

		// isValid 驗證
		assertThat(CellCoordinate.isValid("A1")).isTrue();
		assertThat(CellCoordinate.isValid("D4")).isTrue();
		assertThat(CellCoordinate.isValid("XFD1048576")).isTrue();
		assertThat(CellCoordinate.isValid("additionalProp1")).isFalse();
		assertThat(CellCoordinate.isValid("")).isFalse();
		assertThat(CellCoordinate.isValid(null)).isFalse();
		assertThat(CellCoordinate.isValid("XFE1")).isFalse();
		assertThat(CellCoordinate.isValid("A0")).isFalse();
		assertThat(CellCoordinate.isValid("A1048577")).isFalse();
	}

	/**
	 * Feature: 儲存格合併區間 CellRange 邊界防禦與工廠方法
	 * Scenario: 驗證 CellRange 合法區間建立與無效座標防禦
	 */
	@Test
	@DisplayName("情境：CellRange 合法性與工廠方法驗證")
	void shouldCreateValidCellRange() {
		CellRange range = CellRange.ofRow(5, 0, 3);
		assertThat(range.firstRow()).isEqualTo(5);
		assertThat(range.lastRow()).isEqualTo(5);
		assertThat(range.firstCol()).isEqualTo(0);
		assertThat(range.lastCol()).isEqualTo(3);

		CellRange colRange = CellRange.ofColumn(2, 1, 10);
		assertThat(colRange.firstRow()).isEqualTo(1);
		assertThat(colRange.lastRow()).isEqualTo(10);
		assertThat(colRange.firstCol()).isEqualTo(2);
		assertThat(colRange.lastCol()).isEqualTo(2);

		// 防禦反轉座標
		assertThatThrownBy(() -> new CellRange(5, 4, 0, 3))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new CellRange(0, 5, 3, 2))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new CellRange(-1, 5, 0, 2))
				.isInstanceOf(IllegalArgumentException.class);
	}
}

