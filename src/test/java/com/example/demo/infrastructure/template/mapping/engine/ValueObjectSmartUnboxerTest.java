package com.example.demo.infrastructure.template.mapping.engine;

import com.example.demo.domain.bom.aggregate.vo.BomId;
import com.example.demo.domain.bom.aggregate.vo.BomVersion;
import com.example.demo.domain.bom.aggregate.vo.Quantity;
import com.example.demo.domain.bom.aggregate.vo.ScrapRate;
import com.example.demo.domain.material.aggregate.vo.MaterialCode;
import com.example.demo.domain.material.aggregate.vo.MaterialId;
import com.example.demo.domain.material.aggregate.vo.Money;
import com.example.demo.domain.material.aggregate.vo.UnitOfMeasure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ValueObjectSmartUnboxer 實質物件自動開箱器單元測試。
 * <p>
 * 遵循 AGENTS.md 規則 5 測試規範，具備 Gherkin 語法註解。
 * 驗證各類領域 Value Object 能被智慧解構為基本型別。
 * </p>
 */
@DisplayName("基礎設施層: 領域 Value Object 智慧自動開箱器測試")
public class ValueObjectSmartUnboxerTest {

    /**
     * Feature: 領域實質物件智慧自動開箱
     * Scenario: 驗證 Money, MaterialCode, Quantity, BomVersion 等 VO 之開箱結果
     *   Given 建立各類領域封裝型 Value Object
     *   When 傳入 ValueObjectSmartUnboxer.unbox
     *   Then 應自動取出其核心數值或代碼
     */
    @Test
    @DisplayName("Gherkin: 各類 Value Object 自動提取基本型別值")
    void shouldUnboxValueObjectsCorrectly() {
        // Given
        MaterialCode code = MaterialCode.of("MAT-RM-001");
        Money money = Money.of(new BigDecimal("1250.00"), "TWD");
        Quantity quantity = Quantity.of(new BigDecimal("2.5"));
        ScrapRate scrapRate = ScrapRate.of(new BigDecimal("0.05"));
        BomVersion version = BomVersion.of("V1.0.0");
        BomId bomId = BomId.of("BOM-001");
        UnitOfMeasure uom = UnitOfMeasure.KG;

        // When
        Object unboxedCode = ValueObjectSmartUnboxer.unbox(code);
        Object unboxedMoney = ValueObjectSmartUnboxer.unbox(money);
        Object unboxedQty = ValueObjectSmartUnboxer.unbox(quantity);
        Object unboxedRate = ValueObjectSmartUnboxer.unbox(scrapRate);
        Object unboxedVersion = ValueObjectSmartUnboxer.unbox(version);
        Object unboxedBomId = ValueObjectSmartUnboxer.unbox(bomId);
        Object unboxedUom = ValueObjectSmartUnboxer.unbox(uom);
        Object unboxedPrimitive = ValueObjectSmartUnboxer.unbox(12345);
        Object unboxedNull = ValueObjectSmartUnboxer.unbox(null);

        // Then
        assertThat(unboxedCode).isEqualTo("MAT-RM-001");
        assertThat((BigDecimal) unboxedMoney).isEqualByComparingTo("1250.00");
        assertThat((BigDecimal) unboxedQty).isEqualByComparingTo("2.5");
        assertThat((BigDecimal) unboxedRate).isEqualByComparingTo("0.05");
        assertThat(unboxedVersion).isEqualTo("V1.0.0");
        assertThat(unboxedBomId).isEqualTo("BOM-001");
        assertThat(unboxedUom).isEqualTo(UnitOfMeasure.KG);
        assertThat(unboxedPrimitive).isEqualTo(12345);
        assertThat(unboxedNull).isNull();
    }
}

