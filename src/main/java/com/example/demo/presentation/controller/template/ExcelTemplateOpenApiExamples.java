package com.example.demo.presentation.controller.template;

/**
 * Excel 範本匯出 REST 控制器之 OpenAPI / Swagger UI 測試範例常數集合 (OpenAPI Documentation
 * Examples)。
 * <p>
 * 專門集中管理 {@link ExcelTemplateExportController} 對外暴露之各項 Swagger 宣告式範例資料 (Request
 * Body Examples)，
 * 包含標準資料模型注入 (Buffered / Streaming 模式) 與自訂動態欄位映射 (Field Mapping 模式) 的完整 JSON
 * 規格。
 * 遵循職責分離原則，抽離龐大的靜態文字區塊以維護控制器的精簡結構與可讀性。
 * </p>
 */
public final class ExcelTemplateOpenApiExamples {

  private ExcelTemplateOpenApiExamples() {
    // 私有建構子，防止實例化
  }

  /**
   * 採購詢價比價單 (PROCUREMENT_COMPARISON) 標準資料模型範例 JSON。
   */
  public static final String PROCUREMENT_COMPARISON_EXAMPLE_JSON = """
      {
        "outputFileName": "quotation_comparison.xlsx",
        "dataModel": {
          "inquiryNo": "INQ-202611-008",
          "inquiryDate": "2026-11-10",
          "vendorNames": "漢翔精密, 長榮航太, 神盾精密",
          "quotationItems": [
            {
              "materialCode": "MAT-SFG-001",
              "materialName": "飛控主板電路總成",
              "specification": "FC-STM32H7",
              "requiredQuantity": 50,
              "vendorName": "漢翔精密",
              "quoteUnitPrice": 4520.00,
              "targetPrice": 4500.00,
              "vendorA_Price": 4800.00,
              "vendorB_Price": 4650.00,
              "vendorC_Price": 4520.00,
              "lowestPrice": 4520.00,
              "priceDiff": 20.00
            },
            {
              "materialCode": "MAT-SFG-002",
              "materialName": "2212 920KV 無刷動力馬達組",
              "specification": "920KV CW/CCW",
              "requiredQuantity": 200,
              "vendorName": "神盾精密",
              "quoteUnitPrice": 640.00,
              "targetPrice": 650.00,
              "vendorA_Price": 680.00,
              "vendorB_Price": 660.00,
              "vendorC_Price": 640.00,
              "lowestPrice": 640.00,
              "priceDiff": -10.00
            },
            {
              "materialCode": "MAT-RM-0076",
              "materialName": "航太級碳纖維十字機臂管件",
              "specification": "3K Carbon D16",
              "requiredQuantity": 100,
              "vendorName": "神盾精密",
              "quoteUnitPrice": 1190.00,
              "targetPrice": 1200.00,
              "vendorA_Price": 1250.00,
              "vendorB_Price": 1220.00,
              "vendorC_Price": 1190.00,
              "lowestPrice": 1190.00,
              "priceDiff": -10.00
            }
          ]
        }
      }
      """;

  /**
   * 物料主檔清單 (MATERIAL_MASTER) 標準資料模型範例 JSON。
   */
  public static final String MATERIAL_MASTER_EXAMPLE_JSON = """
      {
        "outputFileName": "material_master_export.xlsx",
        "dataModel": {
          "companyName": "高準精密航太科技股份有限公司",
          "exportDate": "2026-11-10",
          "materials": [
            {
              "materialCode": "MAT-SFG-001",
              "materialName": "飛控主板電路總成",
              "specification": "FC-STM32H7",
              "category": "半成品",
              "unit": "套/組",
              "standardCost": 4800.00
            },
            {
              "materialCode": "MAT-SFG-002",
              "materialName": "2212 動力推進總成",
              "specification": "920KV 4-Pack",
              "category": "半成品",
              "unit": "套/組",
              "standardCost": 680.00
            }
          ]
        }
      }
      """;

  /**
   * 產品結構配方與成本分析表 (BOM_COST_ANALYSIS) 標準資料模型範例 JSON。
   */
  public static final String BOM_COST_ANALYSIS_EXAMPLE_JSON = """
      {
        "outputFileName": "bom_cost_analysis_PRO4K.xlsx",
        "dataModel": {
          "parentMaterialCode": "MAT-FG-001",
          "parentMaterialName": "4K 專業商用巡檢無人機旗艦裝配 (PRO-4K)",
          "version": "V1.0",
          "status": "APPROVED",
          "baseQuantity": 1.00,
          "currency": "TWD",
          "items": [
            {
              "level": 1,
              "itemSequence": 10,
              "childMaterialCode": "MAT-SFG-001",
              "childMaterialName": "飛控主板電路總成",
              "specification": "FC-STM32H7",
              "category": "半成品",
              "unit": "套/組",
              "baseQuantity": 1.00,
              "scrapRate": 0.01,
              "standardCost": 4800.00,
              "positionCode": "FC-ASM",
              "remark": "高算力航行主控板"
            }
          ]
        }
      }
      """;

  /**
   * 標準物料主檔領域開箱動態映射範例 (MATERIAL_MASTER，免傳 sourceData，系統自動查庫)。
   */
  public static final String MAPPED_MATERIAL_MASTER_EXAMPLE_JSON = """
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
      """;

  /**
   * 前端傳入自訂業務資料多租戶動態欄位映射範例 (帶 sourceData)。
   */
  public static final String MAPPED_CUSTOM_SOURCE_EXAMPLE_JSON = """
      {
        "outputFileName": "custom_tenant_material_export.xlsx",
        "sourceData": [
          {
            "itemNo": "TENANT-A-01",
            "title": "航太專用鈦合金板材",
            "specDesc": "Ti-6Al-4V 2.0mm",
            "categoryLabel": "RAW_MATERIAL",
            "unitLabel": "KG",
            "costValue": 3500.00
          },
          {
            "itemNo": "TENANT-A-02",
            "title": "高強度航空鋁合金條",
            "specDesc": "Al-7075-T6 D20",
            "categoryLabel": "RAW_MATERIAL",
            "unitLabel": "M",
            "costValue": 1280.00
          }
        ],
        "mappingDefinition": {
          "templateKey": "MATERIAL_MASTER",
          "rootCollectionVar": "materials",
          "itemAlias": "m",
          "globalMeta": {
            "companyName": "租戶 A (台灣航太精密加工)",
            "exportDate": "2026-11-10"
          },
          "rules": [
            {
              "sourcePath": "itemNo",
              "targetKey": "materialCode"
            },
            {
              "sourcePath": "title",
              "targetKey": "materialName"
            },
            {
              "sourcePath": "specDesc",
              "targetKey": "specification"
            },
            {
              "sourcePath": "categoryLabel",
              "targetKey": "category",
              "converterKey": "ENUM_DICT"
            },
            {
              "sourcePath": "unitLabel",
              "targetKey": "unit"
            },
            {
              "sourcePath": "costValue",
              "targetKey": "standardCost",
              "converterKey": "CURRENCY",
              "formatPattern": "#,##0.00"
            }
          ]
        }
      }
      """;

  /**
   * 採購詢價比價單欄位動態映射範例 (PROCUREMENT_COMPARISON)。
   */
  public static final String MAPPED_PROCUREMENT_COMPARISON_EXAMPLE_JSON = """
      {
        "outputFileName": "procurement_comparison_mapped.xlsx",
        "mappingDefinition": {
          "templateKey": "PROCUREMENT_COMPARISON",
          "rootCollectionVar": "quotationItems",
          "itemAlias": "item",
          "globalMeta": {
            "inquiryNo": "INQ-202611-008",
            "inquiryDate": "2026-11-10",
            "vendorNames": "漢翔精密, 長榮航太, 神盾精密"
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
              "sourcePath": "standardCost",
              "targetKey": "quoteUnitPrice",
              "converterKey": "CURRENCY",
              "formatPattern": "#,##0.00"
            }
          ]
        }
      }
      """;
}
