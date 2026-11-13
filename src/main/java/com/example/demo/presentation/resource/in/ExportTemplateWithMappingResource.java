package com.example.demo.presentation.resource.in;

import com.example.demo.application.shared.mapping.TemplateMappingDefinition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 具備自訂欄位映射之範本 Excel 匯出請求資源載體 (Request Resource)。
 * <p>
 * 遵循 AGENTS.md 規則 3-1，請求物件以 {@code V + N + Resource} 命名。
 * 純資料載體 (Pure Data Carrier)，內部不得實作任何 toCommand 轉換邏輯。
 * 負責接收自訂欄位映射規格、自訂來源資料 (可選) 與輸出檔名。
 * </p>
 */
@Schema(
    description = "具備自訂欄位映射之範本 Excel 匯出請求資源載體",
    example = """
    {
      "outputFileName": "material_master_basic.xlsx",
      "mappingDefinition": {
        "templateKey": "MATERIAL_MASTER",
        "rootCollectionVar": "materials",
        "itemAlias": "m",
        "globalMeta": {
          "companyName": "神盾航太科技股份有限公司",
          "exportDate": "2026-11-10"
        },
        "rules": [
          {
            "sourcePath": "code.value",
            "targetKey": "materialCode"
          },
          {
            "sourcePath": "name",
            "targetKey": "materialName"
          },
          {
            "sourcePath": "specification",
            "targetKey": "specification"
          },
          {
            "sourcePath": "category",
            "targetKey": "category",
            "converterKey": "ENUM_DICT"
          },
          {
            "sourcePath": "unit",
            "targetKey": "unit"
          },
          {
            "sourcePath": "standardCost",
            "targetKey": "standardCost",
            "converterKey": "CURRENCY",
            "formatPattern": "#,##0.00"
          }
        ]
      }
    }
    """
)
public record ExportTemplateWithMappingResource(

    @Schema(description = "範本動態欄位映射規格定義 (包含根變數、項目別名、欄位規則與全域中繼資料)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "映射規格定義 (mappingDefinition) 不得為空")
    TemplateMappingDefinition mappingDefinition,

    @Schema(
        description = "自訂來源業務資料清單或單一物件 (選填，若未填寫則系統依 templateKey 自動自領域儲存庫載入實體資料)",
        example = "[{\"itemNo\":\"TENANT-A-01\",\"title\":\"航太專用鈦合金板材\",\"specDesc\":\"Ti-6Al-4V 2.0mm\",\"categoryLabel\":\"RAW_MATERIAL\",\"unitLabel\":\"KG\",\"costValue\":3500.00}]"
    )
    Object sourceData,

    @Schema(description = "自訂輸出檔案名稱 (選填，若未填則系統依範本鍵值自動命名)", example = "custom_material_master.xlsx")
    String outputFileName
) {}

