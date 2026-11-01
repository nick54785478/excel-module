package com.example.demo.domain.bom.aggregate.vo;

/**
 * 物料清單項次序號實質物件 (Item Sequence Value Object)
 * <p>
 * 定義 BOM 清單中子件之排列序號（例如 10, 20, 30...），數值必須為大於 0 之正整數。
 */
public record ItemSequence(int value) implements Comparable<ItemSequence> {

	/**
	 * 預設起始序號 (10)
	 */
	public static final int DEFAULT_INITIAL = 10;

	/**
	 * 預設序號間隔遞增步長 (10)
	 */
	public static final int DEFAULT_STEP = 10;

	/**
	 * 建構並檢核項次序號。
	 *
	 * @param value 序號整數
	 * @throws IllegalArgumentException 當數值小於或等於 0 時拋出
	 */
	public ItemSequence {
		if (value <= 0) {
			throw new IllegalArgumentException("BOM 項次序號必須為大於 0 之正整數 [" + value + "]");
		}
	}

	/**
	 * 取得起始序號物件 (10)。
	 *
	 * @return 起始序號
	 */
	public static ItemSequence initial() {
		return new ItemSequence(DEFAULT_INITIAL);
	}

	/**
	 * 依據指定整數建立項次序號。
	 *
	 * @param value 序號整數
	 * @return 項次序號實質物件
	 */
	public static ItemSequence of(int value) {
		return new ItemSequence(value);
	}

	/**
	 * 依據預設步長 (+10) 產生下一項次序號。
	 *
	 * @return 下一個項次序號實質物件
	 */
	public ItemSequence next() {
		return new ItemSequence(this.value + DEFAULT_STEP);
	}

	/**
	 * 依據自訂步長產生下一項次序號。
	 *
	 * @param step 遞增步長
	 * @return 下一個項次序號實質物件
	 */
	public ItemSequence next(int step) {
		if (step <= 0) {
			throw new IllegalArgumentException("遞增步長必須大於 0");
		}
		return new ItemSequence(this.value + step);
	}

	@Override
	public int compareTo(ItemSequence o) {
		return Integer.compare(this.value, o.value);
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}

