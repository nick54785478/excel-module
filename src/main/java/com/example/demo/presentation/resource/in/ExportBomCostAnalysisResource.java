package com.example.demo.presentation.resource.in;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

/**
 * 產品結構配方與標準成本分析匯出請求資源 (Export BOM Cost Analysis Request Resource)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 遵循 AGENTS.md 規則 3-1 (動詞 + 名詞 + Resource) 與規則 33~36 規範。<br>
 * 作為表現層純資料載體 (Pure Data Carrier)，<b>嚴禁於內部撰寫任何 toCommand 或業務轉換邏輯</b>。<br>
 * 參數對映與轉譯由 {@link com.example.demo.presentation.assembler.ExcelReportResourceAssembler} 統一負責。
 *
 * @param parentMaterialCode 母件物料料號 (如 MAT-FG-001)
 * @param reportTitle        自訂列印頁首報表標題
 * @param password           工作表防護密碼 (鎖定公式防篡改)
 * @param classification     列印頁尾機密等級標籤
 * @param items              自訂 BOM 明細清單 (若未傳入則由系統儲存庫自動載入)
 */
@Schema(
		description = "BOM 結構配方與標準成本卷算分析報表匯出請求資源",
		example = """
		{
		  "parentMaterialCode": "MAT-FG-001",
		  "reportTitle": "產品結構配方與標準成本卷算分析表",
		  "password": "BomAuditSecure2026",
		  "classification": "極機密・核心配方與成本審計",
		  "items": [
		    {
		      "level": 1,
		      "itemSequence": 10,
		      "childMaterialCode": "MAT-SFG-001",
		      "childMaterialName": "飛控主板電路總成",
		      "specification": "STM32H743 Cortex-M7 雙備援",
		      "category": "半成品",
		      "unit": "套/組",
		      "baseQuantity": 1,
		      "scrapRate": 0.01,
		      "standardCost": 4800,
		      "positionCode": "FC-ASM",
		      "remark": "核心飛控模組"
		    },
		    {
		      "level": 1,
		      "itemSequence": 20,
		      "childMaterialCode": "MAT-SFG-002",
		      "childMaterialName": "2212 動力推進總成 (4組)",
		      "specification": "2212 920KV 碳纖快拆",
		      "category": "半成品",
		      "unit": "套/組",
		      "baseQuantity": 4,
		      "scrapRate": 0.00,
		      "standardCost": 680,
		      "positionCode": "PROP-1~4",
		      "remark": "動力推進系統"
		    },
		    {
		      "level": 1,
		      "itemSequence": 30,
		      "childMaterialCode": "MAT-SFG-003",
		      "childMaterialName": "4K 三軸機械雲台相機總成",
		      "specification": "SONY IMX586 4800萬像素",
		      "category": "半成品",
		      "unit": "套/組",
		      "baseQuantity": 1,
		      "scrapRate": 0.00,
		      "standardCost": 3600,
		      "positionCode": "CAM-GIMBAL",
		      "remark": "光學偵蒐負載"
		    },
		    {
		      "level": 1,
		      "itemSequence": 40,
		      "childMaterialCode": "MAT-SFG-004",
		      "childMaterialName": "4S 5000mAh 智慧動力鋰電池",
		      "specification": "21700 電芯 4S1P 100A BMS",
		      "category": "半成品",
		      "unit": "套/組",
		      "baseQuantity": 2,
		      "scrapRate": 0.00,
		      "standardCost": 1850,
		      "positionCode": "BAT-1,2",
		      "remark": "航電儲能系統"
		    },
		    {
		      "level": 1,
		      "itemSequence": 50,
		      "childMaterialCode": "MAT-RM-0076",
		      "childMaterialName": "碳纖維四軸機架本體",
		      "specification": "全碳纖 3K 2.0mm 輕量化",
		      "category": "原物料",
		      "unit": "件/個",
		      "baseQuantity": 1,
		      "scrapRate": 0.00,
		      "standardCost": 1250,
		      "positionCode": "FRAME-BODY",
		      "remark": "主結構碳纖套件"
		    },
		    {
		      "level": 1,
		      "itemSequence": 60,
		      "childMaterialCode": "MAT-RM-0077",
		      "childMaterialName": "GPS/北斗雙頻陶瓷導航天線",
		      "specification": "L1/L5 雙頻抗干擾 RTK",
		      "category": "原物料",
		      "unit": "件/個",
		      "baseQuantity": 1,
		      "scrapRate": 0.00,
		      "standardCost": 750,
		      "positionCode": "GPS-ANT",
		      "remark": "高精定位天線"
		    },
		    {
		      "level": 1,
		      "itemSequence": 70,
		      "childMaterialCode": "MAT-RM-0078",
		      "childMaterialName": "M3*8 航太鈦合金裝配螺絲",
		      "specification": "TC4 鈦合金沉頭內六角",
		      "category": "原物料",
		      "unit": "件/個",
		      "baseQuantity": 24,
		      "scrapRate": 0.05,
		      "standardCost": 12.50,
		      "positionCode": "SCREW-M3-8",
		      "remark": "裝配耗損 5%"
		    },
		    {
		      "level": 1,
		      "itemSequence": 80,
		      "childMaterialCode": "MAT-RM-0079",
		      "childMaterialName": "防震防水氣密防護提箱",
		      "specification": "IP67 輕量化 PP 雙鎖扣",
		      "category": "原物料",
		      "unit": "箱/盒",
		      "baseQuantity": 1,
		      "scrapRate": 0.00,
		      "standardCost": 890,
		      "positionCode": "BOX-CASE",
		      "remark": "防震包材配件"
		    }
		  ]
		}
		"""
)
public record ExportBomCostAnalysisResource(
		@Schema(description = "母件物料料號 (例如 MAT-FG-001，商用無人機)", example = "MAT-FG-001")
		String parentMaterialCode,

		@Schema(description = "自訂列印頁首報表大標題", example = "產品結構配方與標準成本卷算分析表")
		String reportTitle,

		@Schema(description = "工作表解鎖密碼 (鎖定公式儲存格防篡改)", example = "BomAuditSecure2026")
		String password,

		@Schema(description = "列印頁尾機密等級標籤", example = "極機密・核心配方與成本審計")
		String classification,

		@Schema(description = "自訂物料子項明細清單 (選填，傳入時覆寫資料庫配方)")
		List<@Valid BomCostItemResource> items
) {

	/**
	 * BOM 子項明細資源純資料載體
	 */
	@Schema(description = "BOM 單項明細資源純資料載體")
	public record BomCostItemResource(
			@Schema(description = "階層 (Level)", example = "1")
			Integer level,

			@Schema(description = "項次序號", example = "10")
			Integer itemSequence,

			@Schema(description = "子件料號", example = "MAT-SFG-001")
			String childMaterialCode,

			@Schema(description = "子件品名", example = "飛控主板電路總成")
			String childMaterialName,

			@Schema(description = "規格型號說明", example = "STM32H743 Cortex-M7 雙備援")
			String specification,

			@Schema(description = "物料分類", example = "半成品")
			String category,

			@Schema(description = "計量單位", example = "套/組")
			String unit,

			@Schema(description = "單機基準用量", example = "1.00")
			BigDecimal baseQuantity,

			@Schema(description = "裝配損耗率 (如 0.05 代表 5%)", example = "0.01")
			BigDecimal scrapRate,

			@Schema(description = "標準採購單價", example = "4800.00")
			BigDecimal standardCost,

			@Schema(description = "安裝位號", example = "FC-ASM")
			String positionCode,

			@Schema(description = "備註說明", example = "核心飛控模組")
			String remark
	) {}
}

