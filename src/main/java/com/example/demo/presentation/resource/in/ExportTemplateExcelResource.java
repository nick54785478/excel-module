package com.example.demo.presentation.resource.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * 範本 Excel 匯出請求資源載體 (Request Resource)。
 * <p>
 * 遵循 AGENTS.md 規則 3-1，請求物件以 {@code V + N + Resource} 命名。
 * 純資料載體 (Pure Data Carrier)，內部不得實作任何 toCommand 轉換邏輯。
 * 負責接收客戶端傳入之動態資料模型與自訂輸出檔名。
 * </p>
 */
@Schema(
    description = "範本 Excel 匯出請求資源載體",
    example = """
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
    """
)
public record ExportTemplateExcelResource(

    @Schema(
        description = "注入範本之業務資料上下文 (支援任意鍵值對字典或巢狀清單，例如 PROCUREMENT_COMPARISON 包含 inquiryNo, inquiryDate, vendorNames, quotationItems)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "{\"inquiryNo\":\"INQ-202611-008\",\"inquiryDate\":\"2026-11-10\",\"vendorNames\":\"漢翔精密, 長榮航太, 神盾精密\",\"quotationItems\":[{\"materialCode\":\"MAT-SFG-001\",\"materialName\":\"飛控主板電路總成\",\"specification\":\"FC-STM32H7\",\"requiredQuantity\":50,\"vendorName\":\"漢翔精密\",\"quoteUnitPrice\":4520.00,\"targetPrice\":4500.00,\"vendorA_Price\":4800.00,\"vendorB_Price\":4650.00,\"vendorC_Price\":4520.00,\"lowestPrice\":4520.00,\"priceDiff\":20.00}]}"
    )
    @NotNull(message = "資料模型 (dataModel) 不得為空")
    Map<String, Object> dataModel,

    @Schema(description = "自訂輸出檔案名稱 (選填，若未填則系統依範本鍵值自動命名)", example = "quotation_comparison.xlsx")
    String outputFileName
) {}

