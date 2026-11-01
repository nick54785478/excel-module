package com.example.demo.domain.bom.aggregate.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 用量數量實質物件 (Quantity Value Object)
 * <p>
 * 封裝母件基數或子件單機用量，限制數值必須大於 0，精度統一保留 4 位小數。
 */
public record Quantity(BigDecimal value) {

	/**
	 * 標準計算精度 (4 位小數)
	 */
	public static final int DEFAULT_SCALE = 4;

	/**
	 * 預設單位基準量 (1.0000)
	 */
	public static final Quantity ONE = new Quantity(BigDecimal.ONE);

	/**
	 * 建構並檢核用量數值。
	 *
	 * @param value 數量數值
	 * @throws IllegalArgumentException 當數值為空或小於等於零時拋出
	 */
	public Quantity {
		if (value == null) {
			throw new IllegalArgumentException("用量數值不得為空");
		}
		if (value.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("用量數值必須大於零 [" + value + "]");
		}
		value = value.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
	}

	/**
	 * 依據指定 BigDecimal 建立用量。
	 *
	 * @param value 數量數值
	 * @return 用量實質物件
	 */
	public static Quantity of(BigDecimal value) {
		return new Quantity(value);
	}

	/**
	 * 依據指定 double 建立用量。
	 *
	 * @param value 數量數值
	 * @return 用量實質物件
	 */
	public static Quantity of(double value) {
		return new Quantity(BigDecimal.valueOf(value));
	}

	/**
	 * 數量加法。
	 *
	 * @param other 另一用量物件
	 * @return 加總後之用量物件
	 */
	public Quantity add(Quantity other) {
		Objects.requireNonNull(other, "相加之用量不得為空");
		return new Quantity(this.value.add(other.value));
	}

	/**
	 * 數量乘算因子。
	 *
	 * @param factor 乘數因子
	 * @return 乘算後之用量物件
	 */
	public Quantity multiply(BigDecimal factor) {
		Objects.requireNonNull(factor, "乘數因子不得為空");
		return new Quantity(this.value.multiply(factor));
	}

	@Override
	public String toString() {
		return value.toPlainString();
	}
}

