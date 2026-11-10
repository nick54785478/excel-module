# Domain Aggregate JXLS 範本規格與實作說明 (03-domain-aggregate-jxls-templates.md)

本文件詳盡規範並記錄 `excel-module` 中基於 **DDD 領域模型聚合根（Domain Aggregates）** 所建立之 JXLS 3.x 宣告式 Excel 測試與正式範本。

---

## 一、 核心聚合與範本對照架構

依據領域驅動設計（DDD）原則，系統建立了兩大核心領域聚合根以及一組跨領域採購詢價情境，並為其設計專屬的高保真 JXLS 範本：

| 領域聚合根 / 情境 | 範本檔案名稱 | 實體檔案路徑 | 核心領域概念與 Value Objects | 適用報表情境 |
| :--- | :--- | :--- | :--- | :--- |
| **Material Aggregate** | `material_master.xlsx` | `src/main/resources/templates/excel/material_master.xlsx` | `MaterialId`, `MaterialCode`, `MaterialCategory`, `UnitOfMeasure`, `Money`, `MaterialStatus` | 物料主檔清單、分類匯總表、標準單價加總 |
| **Bom Aggregate** | `bom_cost_analysis.xlsx` | `src/main/resources/templates/excel/bom_cost_analysis.xlsx` | `BomId`, `BomVersion`, `BomStatus`, `BomItem` (Entity), `Quantity`, `Money`, `ScrapRate` | 階層式 BOM 配方結構、損耗率、有效需求用量動態試算、母件標準成本卷算 |
| **Procurement RFQ** | `procurement_comparison.xlsx` | `src/main/resources/templates/excel/procurement_comparison.xlsx` | 採購料號、首選供應商、報價單價、需求數量、金額合計 | 採購詢價比價表、供應商決標比價單 |

---

## 二、 範本詳細規格與 JXLS 標籤定義

### 1. 物料主檔清單範本 (`material_master.xlsx`)

- **對應領域物件**：[`Material`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/domain/material/aggregate/root/Material.java)
- **Sheet 名稱**：`物料主檔`
- **外觀風格**：深海軍藍（Navy `#1B365D`）旗艦級橫幅、經典鋼藍表頭（`#334E68`）、交替列底色與專用會計貨幣格式。

#### 格位結構與 JXLS 指令分佈：

| 儲存格 | 預設文字 / 表達式 | JXLS 批註指令 (Comment) | 業務意義與渲染行為 |
| :--- | :--- | :--- | :--- |
| **A1** (A1:H1 合併) | 物料主檔清單 (Material Master Report) | `jx:area(lastCell="H5")` | 定義 JXLS 渲染工作區邊界為 A1:H5。 |
| **A2** | `公司名稱: ${companyName}` | - | 公司抬頭（變數插值）。 |
| **D2** | `製表日期: ${exportDate}` | - | 報表生成日期。 |
| **G2** | `物料總筆數: ${materials.size()}` | - | 清單項目總量統計。 |
| **A3:H3** | 項次, 物料編號, 物料品名, 規格型號, 物料分類, 計量單位, 標準成本 (TWD), 物料狀態 | - | 欄位標題行。 |
| **A4** | `${idx + 1}` | `jx:each(items="materials" var="m" varIndex="idx" lastCell="H4")` | 循環集合 `materials`，`varIndex="idx"` 自動從 0 起算，渲染為 1-based 序號。 |
| **B4** | `${m.materialCode}` | - | 對應 `MaterialCode.value()`。 |
| **C4** | `${m.materialName}` | - | 對應物料品名。 |
| **D4** | `${m.specification}` | - | 對應物料規格型號。 |
| **E4** | `${m.category}` | - | 對應 `MaterialCategory` (原物料/半成品/成品)。 |
| **F4** | `${m.unit}` | - | 對應 `UnitOfMeasure` (支/顆/套/組/對)。 |
| **G4** | `${m.standardCost}` | - | 對應 `standardCost.amount()`，設定貨幣格式 `$#,##0.00`。 |
| **H4** | `${m.status}` | - | 對應 `MaterialStatus` (ACTIVE/INACTIVE/OBSOLETE)。 |
| **A5:F5** (合併) | 標準單價合計 (Total Standard Cost): | - | 統計彙總摘要列。 |
| **G5** | `$[SUM(G4)]` | - | **動態平移公式**：JXLS 填充後依據實際物料列數自動擴展為 `=SUM(G4:G{N})`。 |

---

### 2. BOM 配方與成本卷算分析範本 (`bom_cost_analysis.xlsx`)

- **對應領域物件**：[`Bom`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/domain/bom/aggregate/root/Bom.java) 聚合根與 [`BomItem`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/domain/bom/aggregate/entity/BomItem.java) 實體清單。
- **Sheet 名稱**：`BOM成本分析`
- **外觀風格**：工業精密藍（`#2B4C7E`）、母件結構資訊卡、多欄位運算。

#### 格位結構與 JXLS 指令分佈：

| 儲存格 | 預設文字 / 表達式 | JXLS 批註指令 (Comment) | 業務意義與動態計算 |
| :--- | :--- | :--- | :--- |
| **A1** (A1:L1 合併) | 產品結構配方與標準成本分析表 | `jx:area(lastCell="L6")` | 定義工作區邊界為 A1:L6。 |
| **A2 / D2 / I2** | 母件料號: `${parentMaterialCode}`, 母件品名: `${parentMaterialName}`, 配方版本: `${version}` | - | 母件識別資訊（來自 `Bom.getParentMaterialId()` 與 `BomVersion`）。 |
| **A3 / D3 / I3** | 審核狀態: `${status}`, 基準數量: `${baseQuantity}`, 計價幣別: `${currency}` | - | 配方審核狀態（`BomStatus`）與生產基準單位量。 |
| **A4:L4** | 項次, 階層, 子件料號, 子件品名, 規格型號, 物料分類, 單位, 基準用量, 損耗率, 有效需求用量, 標準單價, 成本小計 | - | 12 欄專業 BOM 表頭。 |
| **A5** | `${item.itemSequence}` | `jx:each(items="items" var="item" varIndex="idx" lastCell="L5")` | 循環遍歷子件清單 `items`。 |
| **B5 ~ G5** | 階層、料號、品名、規格、分類、單位 | - | 子件基本屬性與計量單位。 |
| **H5** | `${item.baseQuantity}` | - | 基準用量（例如 `1.0`）。 |
| **I5** | `${item.scrapRate}` | - | 損耗率（設定百分比格式 `0.00%`）。 |
| **J5** | `$[H5*(1+I5)]` | - | **列內動態需求量試算**：JXLS 自動平移為 `=H{row}*(1+I{row})`。 |
| **K5** | `${item.standardCost}` | - | 子件標準單價。 |
| **L5** | `$[J5*K5]` | - | **列內成本小計動態試算**：JXLS 自動平移為 `=J{row}*K{row}`。 |
| **A6:K6** (合併) | 單機物料成本總計 (Total BOM Cost): | - | 表尾加總說明。 |
| **L6** | `$[SUM(L5)]` | - | **表尾跨列動態合計**：JXLS 自動展開為 `=SUM(L5:L{N})`。 |

---

### 3. 採購比價單範本 (`procurement_comparison.xlsx`)

- **對應領域情境**：採購詢價、核價與供應商比價報表。
- **Sheet 名稱**：`採購比價單`
- **動態公式**：
  - `$[D4*F4]`：採購項目小計（需求數量 × 首選供應商報價）。
  - `$[SUM(G4)]`：整張詢價單全品項採購總金額自動加總。

---

## 三、 JXLS 3.x 語法與 JEXL 關鍵機制

在 JXLS 3.x 與 Spring Boot 3.5.0 環境下，特別注意以下實務特性與規範：

1. **項次計數器語法**：
   - JXLS 2.x 的 `varStatus="status"` 屬性已在 JXLS 3.x 中移除。
   - JXLS 3.x 請使用 `varIndex="idx"`（0-based 索引變數），配合表達式 `${idx + 1}` 實現 1-based 行號序號。
2. **Excel 動態公式標記**：
   - 請一律使用 `$[...]` 包裹 Excel 原生公式（如 `$[SUM(G4)]`, `$[H5*(1+I5)]`）。
   - 請勿使用 `${=SUM(G4)}`，因為 `${...}` 會交由 JEXL 引擎解析，而 `=` 在 JEXL 開頭會引發 `JexlException$Parsing` 語法錯誤。
3. **安全沙箱 (JEXL Sandbox)**：
   - 所有範本填充一律受 [`JxlsTemplateWriterAdapter`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/infrastructure/adapter/template/JxlsTemplateWriterAdapter.java) 內的 `JxlsJexlPermissions.RESTRICTED` 沙箱嚴格保護，禁止範本注入未授權之 Java 類別反射呼叫。

---

## 四、 測試與驗證

本模組已由 [`JxlsAggregateTemplateGeneratorTest`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/test/java/com/example/demo/infrastructure/adapter/template/JxlsAggregateTemplateGeneratorTest.java) 進行完整的 Gherkin BDD 整合驗證：

```bash
.\mvnw.cmd test -Dtest=JxlsAggregateTemplateGeneratorTest
```

驗證結果通過：
- `shouldGenerateAndVerifyMaterialMasterTemplate()`：成功驗證 5 筆物料展開與 `=SUM(G4:G8)` 動態平移公式。
- `shouldGenerateAndVerifyBomCostAnalysisTemplate()`：成功驗證 4 筆 BOM 子件展開、`=H5*(1+I5)` 損耗計算、`=J5*K5` 金額小計與 `=SUM(L5:L8)` 總成本卷算。
- `shouldGenerateProcurementComparisonTemplate()`：成功產製並驗證比價單範本。

