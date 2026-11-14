package com.example.demo.domain.material.aggregate.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import com.example.demo.domain.material.exception.NegativeMaterialCostException;

/**
 * 貨幣與金額實質物件 (Money Value Object)
 * <p>
 * 封裝金額數值與幣別，提供高精度之算術運算與非負數之領域檢核。
 *
 * @param amount 金額數值 (保留 4 位小數，不可為負數)
 * @param currency 幣別 ISO 代碼 (如 "TWD", "USD")
 */
public record Money(BigDecimal amount, String currency) {

	/**
	 * 預設幣別 (新台幣)
	 */
	public static final String DEFAULT_CURRENCY = "TWD";

	/**
	 * 標準計算精度 (小數點後 4 位)
	 */
	public static final int DEFAULT_SCALE = 4;

	/**
	 * 建構並檢核金額與幣別。
	 *
	 * @param amount 金額數值
	 * @param currency 幣別字串
	 * @throws NegativeMaterialCostException 當金額為負數時拋出
	 */
	public Money {
		if (amount == null) {
			amount = BigDecimal.ZERO.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
		} else {
			if (amount.compareTo(BigDecimal.ZERO) < 0) {
				throw new NegativeMaterialCostException("物料金額/成本不得為負數 [" + amount + "]");
			}
			amount = amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
		}
		if (currency == null || currency.trim().isEmpty()) {
			currency = DEFAULT_CURRENCY;
		} else {
			currency = currency.trim().toUpperCase();
		}
	}

	/**
	 * 建立預設幣別為零之金額物件。
	 *
	 * @return 零元金額物件
	 */
	public static Money zero() {
		return new Money(BigDecimal.ZERO, DEFAULT_CURRENCY);
	}

	/**
	 * 依據指定數值建立新台幣金額物件。
	 *
	 * @param amount 金額數值
	 * @return 金額實質物件
	 */
	public static Money of(BigDecimal amount) {
		return new Money(amount, DEFAULT_CURRENCY);
	}

	/**
	 * 依據指定 double 數值建立新台幣金額物件。
	 *
	 * @param amount 金額數值
	 * @return 金額實質物件
	 */
	public static Money of(double amount) {
		return new Money(BigDecimal.valueOf(amount), DEFAULT_CURRENCY);
	}

	/**
	 * 依據數值與幣別建立金額物件。
	 *
	 * @param amount 金額數值
	 * @param currency 幣別
	 * @return 金額實質物件
	 */
	public static Money of(BigDecimal amount, String currency) {
		return new Money(amount, currency);
	}

	/**
	 * 金額加法。
	 *
	 * @param other 另一個金額物件
	 * @return 加總後之金額物件
	 * @throws IllegalArgumentException 當兩者幣別不相符時拋出
	 */
	public Money add(Money other) {
		Objects.requireNonNull(other, "相加之金額物件不得為空");
		if (!this.currency.equals(other.currency)) {
			throw new IllegalArgumentException(
					"幣別不符，無法加總: " + this.currency + " vs " + other.currency
			);
		}
		return new Money(this.amount.add(other.amount), this.currency);
	}

	/**
	 * 金額乘上倍數 (例如數量或耗用量)。
	 *
	 * @param factor 倍數因子
	 * @return 乘算後之金額物件
	 */
	public Money multiply(BigDecimal factor) {
		Objects.requireNonNull(factor, "乘數不得為空");
		return new Money(this.amount.multiply(factor), this.currency);
	}

	/**
	 * 取得金額與幣別之字串表達形式 (例如 "TWD 100.0000")。
	 *
	 * @return 幣別與金額字串
	 */
	@Override
	public String toString() {
		return currency + " " + amount.toPlainString();
	}
}

