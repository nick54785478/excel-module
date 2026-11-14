package com.example.demo.domain.bom.aggregate.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 耗損率實質物件 (Scrap Rate Value Object)
 * <p>
 * 定義製程中零件之損耗百分比比率（例如 0.05 代表 5% 損耗）。
 * 數值範圍介於 0.0000 至 1.0000 之間（即 0% ~ 100%）。
 *
 * @param rate 損耗比率數值 (介於 0.0 至 1.0 之間)
 */
public record ScrapRate(BigDecimal rate) {

	/**
	 * 標準計算精度 (4 位小數)
	 */
	public static final int DEFAULT_SCALE = 4;

	/**
	 * 零損耗 (0.0000)
	 */
	public static final ScrapRate ZERO = new ScrapRate(BigDecimal.ZERO);

	/**
	 * 建構並檢核損耗率比值。
	 *
	 * @param rate 損耗比率
	 * @throws IllegalArgumentException 當比率小於 0 或大於 1 時拋出
	 */
	public ScrapRate {
		if (rate == null) {
			rate = BigDecimal.ZERO.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
		} else {
			if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
				throw new IllegalArgumentException("損耗率必須介於 0.0 至 1.0 之間 [" + rate + "]");
			}
			rate = rate.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
		}
	}

	/**
	 * 依據指定數值建立損耗率。
	 *
	 * @param rate 比率數值 (如 0.05)
	 * @return 損耗率實質物件
	 */
	public static ScrapRate of(BigDecimal rate) {
		return new ScrapRate(rate);
	}

	/**
	 * 依據指定 double 建立損耗率。
	 *
	 * @param rate 比率數值 (如 0.05)
	 * @return 損耗率實質物件
	 */
	public static ScrapRate of(double rate) {
		return new ScrapRate(BigDecimal.valueOf(rate));
	}

	/**
	 * 依據百分比數值建立損耗率 (例如傳入 5.0 代表 5% -> 0.05)。
	 *
	 * @param percent 百分比數值
	 * @return 損耗率實質物件
	 */
	public static ScrapRate ofPercent(double percent) {
		return new ScrapRate(BigDecimal.valueOf(percent).divide(BigDecimal.valueOf(100), DEFAULT_SCALE, RoundingMode.HALF_UP));
	}

	/**
	 * 依據基礎數量與此損耗率計算實際有效需求用量。
	 * <p>
	 * 公式：{@code 有效用量 = 基礎用量 * (1 + 損耗率)}
	 *
	 * @param baseQuantity 基礎用量
	 * @return 加上損耗後之實際需求用量
	 */
	public Quantity calculateEffectiveQuantity(Quantity baseQuantity) {
		Objects.requireNonNull(baseQuantity, "基礎用量不得為空");
		BigDecimal multiplier = BigDecimal.ONE.add(this.rate);
		BigDecimal effectiveVal = baseQuantity.value().multiply(multiplier).setScale(Quantity.DEFAULT_SCALE, RoundingMode.HALF_UP);
		return Quantity.of(effectiveVal);
	}

	/**
	 * 取得百分比表示字串 (如 "5.00%")。
	 *
	 * @return 百分比字串
	 */
	public String toPercentString() {
		return rate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
	}

	/**
	 * 取得損耗率之小數位字串表達形式 (純數值格式)。
	 *
	 * @return 數值字串
	 */
	@Override
	public String toString() {
		return rate.toPlainString();
	}
}

