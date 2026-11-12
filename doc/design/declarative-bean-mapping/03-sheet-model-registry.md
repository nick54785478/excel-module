# 維度一 Part 3：宣告式模型自動探測、別名註冊表與動態工作表路由 (Sheet Model Registry & Auto-Discovery)

> 📌 本文件座落於 [`doc/design/declarative-bean-mapping/03-sheet-model-registry.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/declarative-bean-mapping/03-sheet-model-registry.md)，屬於「維度一：宣告式物件映射」之【Part 3：宣告式模型自動探測、別名註冊表與動態工作表路由】。  
> 詳述六角形架構 Outbound Port、基礎設施層三層比對適配器、Spring ASM 位元組碼自動掃描裝配與表現層動態多模型調度機制。  
> 
> 🔗 **模組導讀與關聯文件**：  
> • 模組入口主文件：👉 [維度一：宣告式物件映射架構總覽 (declarative-bean-mapping.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/declarative-bean-mapping.md)  
> • 註解與反射快取：👉 [維度一 Part 1：宣告式註解、雙層反射快取與型別容錯轉換 (01-reflection-cache-and-converters.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/declarative-bean-mapping/01-reflection-cache-and-converters.md)  
> • UseCase 與 REST 端點：👉 [維度一 Part 2：應用層 UseCase、CQRS 服務調度與 REST 端點 (02-ports-usecases-and-rest-binding.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/declarative-bean-mapping/02-ports-usecases-and-rest-binding.md)

---

## 1. 痛點背景與重構演進 (Problem Background & Evolution)

在多工作表（Multi-Sheet）或複合型 Excel 匯入情境中，同一份活頁簿可能包含「物料主檔對照表」、「多階層BOM展開清單」與「單階BOM清單」等多種業務工作表。

傳統表現層（Controller）若採取硬編碼（Hardcode）字串比對：
```java
// ❌ 壞味道：粗暴字串比對，高度技術耦合且違反開放封閉原則 (OCP)
if (sheetName.contains("物料") || sheetName.contains("主檔")) {
    // 呼叫物料解析
} else if (sheetName.contains("多階") || sheetName.contains("展開")) {
    // 呼叫多階 BOM 解析
} else if (sheetName.contains("單階") || sheetName.contains("BOM")) {
    // 呼叫單階 BOM 解析
}
```
**此種寫法存在三大嚴重架構缺陷**：
1. **破壞開放封閉原則 (OCP)**：每當新增業務模型或修改 Excel 範本工作表名稱時，必須侵入修改 Controller 核心程式碼。
2. **缺乏防腐與容錯彈性**：無法支援中英文混用、年度版本前綴、大小寫不一致或包含特殊符號之工作表名稱。
3. **元數據散落**：工作表名稱定義散落在 `@ExcelSheet` 註解與 Controller 字串常數中，無法單一來源真相 (Single Source of Truth)。

---

## 2. 六角形架構分層設計 (Hexagonal Architecture & Port/Adapter Separation)

為徹底根除上述問題，同時嚴格遵守 `AGENTS.md` 規範（特別是規則 8、18、30 與 40-41），本模組設計了四層協同的動態模型註冊與工作表路由系統：

```mermaid
flowchart TD
    subgraph Presentation_Layer ["Presentation Layer (表現層)"]
        Ctrl["MaterialBomExcelController<br>(注入 ExcelSheetModelRegistryPort)"]
    end

    subgraph Application_Layer ["Application Layer (應用層 - Pure Java)"]
        RegistryPort["ExcelSheetModelRegistryPort<br>(Outbound Port 介面)"]
        Models["@ExcelSheet 業務模型清單<br>(MaterialExcelModel, MultiLevelBomExcelModel, etc.)"]
    end

    subgraph Infrastructure_Layer ["Infrastructure Layer (基礎設施層)"]
        RegistryAdapter["ExcelSheetModelRegistryAdapter<br>(三層比對倒排索引適配器)"]
    end

    subgraph Config_Layer ["Configuration Layer (配置層 - The Dirtiest Layer)"]
        Config["ExcelSheetModelConfiguration<br>(Spring ASM ClassPath 掃描器自動探測裝配)"]
    end

    Ctrl -->|依賴反轉 (DIP)| RegistryPort
    RegistryAdapter -.->|實作| RegistryPort
    Config -->|掃描 @ExcelSheet| Models
    Config -->|建構並註冊 Bean| RegistryAdapter
```

### 2.1 應用層輸出埠：`ExcelSheetModelRegistryPort`
座落於 [`com.example.demo.application.port.out.core.ExcelSheetModelRegistryPort`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/out/core/ExcelSheetModelRegistryPort.java)：
* **純粹 Java 契約**：嚴格遵守 `AGENTS.md` 規則 18，**零框架與零外層技術依賴**，僅使用 Java 核心型別（`Class<?>`、`String`、`Optional`、`List`）。
* **核心方法合約**：
  - `Optional<Class<?>> findModelClass(String sheetName)`：依據工作表名稱或別名查找對應之模型類別。
  - `Class<?> getRequiredModelClass(String sheetName)`：查找必備模型，查無時拋出業務語意防禦例外。
  - `List<Class<?>> getAllModelClasses()`：取得目前已註冊之所有唯讀模型類別清單。
  - `default boolean supports(String sheetName)`：快速檢驗指定工作表名稱是否具備對應模型。

### 2.2 基礎設施層適配器：`ExcelSheetModelRegistryAdapter`
座落於 [`com.example.demo.infrastructure.adapter.core.ExcelSheetModelRegistryAdapter`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/infrastructure/adapter/core/ExcelSheetModelRegistryAdapter.java)：
* **倒排索引建構**：於建構時自動解析傳入模型清單的 `@ExcelSheet` 註解，抽取 `value()`、`name()`、`aliases()` 以及類別 `getSimpleName()` 建立多維索引。
* **三層高效比對演算法**：
  1. **第一層（$O(1)$ 精確規格化匹配）**：
     將輸入字串進行 `trim()` 與 `toLowerCase(Locale.ROOT)` 正規化，直接於雜湊表中進行 $O(1)$ 查找。
  2. **第二層（寬鬆模糊比對）**：
     自動忽略底線 `_`、減號 `-`、括號 `()` 以及所有內部空白字元，比對寬鬆別名映射。
  3. **第三層（最長別名智慧子字串包含比對）**：
     將所有已註冊別名依字串長度**由長至短**（Descending Length）排序，當活頁簿工作表名稱包含修飾詞（如 `"2026年多階層BOM展開清單(最新版)"`）時，保證最長且最精確之別名優先命中。

### 2.3 全域配置層：`ExcelSheetModelConfiguration`
座落於 [`com.example.demo.config.ExcelSheetModelConfiguration`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/config/ExcelSheetModelConfiguration.java)：
* **遵守規則 40-41 (The Dirtiest Layer)**：將所有 Spring 框架技術、位元組碼掃描與 Bean 裝配集中隔離於 `config/`，保證外層不侵入核心。
* **Zero-Config ASM 自動探測**：
  利用 Spring 的 `ClassPathScanningCandidateComponentProvider` 搭配 `AnnotationTypeFilter(ExcelSheet.class)`，在應用程式啟動時掃描套件（預設 `com.example.demo.application.shared.model`），自動探測所有宣告式模型並動態註冊：
  ```java
  @Bean
  public ExcelSheetModelRegistryPort excelSheetModelRegistryPort() {
      List<Class<?>> detectedModels = scanExcelSheetModels(MODEL_BASE_PACKAGE);
      return new ExcelSheetModelRegistryAdapter(detectedModels);
  }
  ```
* **輕量級測試靜態工廠**：
  提供 `createDefaultRegistry()` 靜態便捷工廠方法，便於單元測試（如 Controller 單元測試）在不安裝或啟動 Spring ApplicationContext 的情況下秒級完成實例化。

---

## 3. 表現層動態模型調度 (Presentation Dynamic Model Dispatching)

在表現層控制器（如 [`MaterialBomExcelController`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/presentation/controller/bom/MaterialBomExcelController.java)）中，控制器建構子依賴注入 `ExcelSheetModelRegistryPort`，調度時完全透過 Port 取得目標 Model Class：

```java
// ✅ 優雅調度：表現層無任何寫死字串比對，全依賴模型元數據註冊表
private MaterialImportedResource dispatchSingleSheetToMaterial(byte[] fileBytes, String sheetName) {
    Class<?> targetModel = sheetModelRegistryPort.findModelClass(sheetName)
            .orElse(MaterialExcelModel.class);

    if (MultiLevelBomExcelModel.class.equals(targetModel)) {
        BeanSheetGottenResult<MultiLevelBomExcelModel> multiResult = importExcelUseCase.importBeanSheet(
                new ByteArrayInputStream(fileBytes), sheetName, MultiLevelBomExcelModel.class);
        return resourceAssembler.toMaterialImportedResourceFromMultiBom(multiResult);
    } else if (BomItemExcelModel.class.equals(targetModel)) {
        BeanSheetGottenResult<BomItemExcelModel> singleResult = importExcelUseCase.importBeanSheet(
                new ByteArrayInputStream(fileBytes), sheetName, BomItemExcelModel.class);
        return resourceAssembler.toMaterialImportedResourceFromSingleBom(singleResult);
    } else {
        BeanSheetGottenResult<MaterialExcelModel> matResult = importExcelUseCase.importBeanSheet(
                new ByteArrayInputStream(fileBytes), sheetName, MaterialExcelModel.class);
        return resourceAssembler.toMaterialImportedResource(matResult);
    }
}
```

---

## 4. 宣告式業務模型標註範例 (Model Annotation Examples)

各業務模型透過 `@ExcelSheet` 宣告主要工作表名稱與相容別名：

```java
// 範例一：物料主檔對照表
@ExcelSheet(
    value = "物料主檔對照表", 
    aliases = {"物料主檔", "材料主檔", "材料清單", "MaterialMaster", "Material_Master"}
)
public class MaterialExcelModel { ... }

// 範例二：多階層 BOM 展開清單 (最長別名優先比對)
@ExcelSheet(
    value = "多階層BOM展開清單", 
    aliases = {"多階層BOM展開", "多階BOM展開", "多階BOM", "階層式BOM", "MultiLevelBom"}
)
public class MultiLevelBomExcelModel { ... }

// 範例三：單階 BOM 配方清單
@ExcelSheet(
    value = "單階BOM清單", 
    aliases = {"單階BOM", "BOM配方", "產品結構清單", "SingleLevelBom", "BomItem"}
)
public class BomItemExcelModel { ... }
```

---

## 5. 單元測試與驗證覆蓋 (Testing & Verification)

本機制已撰寫完整的單元與整合測試，覆蓋所有路由情境：

### 5.1 適配器單元測試：[`ExcelSheetModelRegistryAdapterTest`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/test/java/com/example/demo/infrastructure/adapter/core/ExcelSheetModelRegistryAdapterTest.java)
| 測試情境 (Scenario) | 驗證重點 | 預期結果 |
| :--- | :--- | :--- |
| `shouldFindModelByExactName` | 傳入精確定義之 `@ExcelSheet.value()` | 成功命中對應 Model Class |
| `shouldFindModelCaseInsensitiveAndTrimmed` | 傳入帶有前後空格或大小寫混用字串 (如 `"  material_master  "`) | $O(1)$ 規格化自動修剪並成功命中 |
| `shouldFindModelByAliases` | 傳入 `@ExcelSheet.aliases()` 中定義的任一別名 (如 `"材料清單"`) | 成功命中對應 Model Class |
| `shouldFallbackToSimpleClassName` | 傳入類別簡稱 (如 `"MaterialExcelModel"`) | 回退機制成功命中 |
| `shouldMatchFuzzyWithoutDelimiters` | 傳入去除分隔符號之模糊字串 (如 `"materialmaster"`) | 第二層寬鬆模糊匹配成功命中 |
| `shouldMatchSubstringsWithLongestAliasPriority` | 傳入包含修飾詞之字串 (如 `"2026年多階層BOM展開清單(最新)"`) | 第三層最長別名優先比對成功命中 |
| `shouldReturnEmptyForUnknownSheet` | 傳入未註冊之無效工作表名稱 (如 `"NonExistentSheet"`) | 安全回傳 `Optional.empty()` |
| `shouldHandleNullAndEmptyGracefully` | 傳入 `null` 或空白字串 | 安全回傳 `Optional.empty()`，不拋出非預期例外 |

### 5.2 控制器整合測試：[`MaterialBomExcelControllerTest`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/test/java/com/example/demo/presentation/controller/bom/MaterialBomExcelControllerTest.java)
* 驗證在 `POST /materials/import` 與 `POST /bom-items/import` 端點傳入包含自訂別名的工作表時，控制器皆能依賴 `ExcelSheetModelRegistryPort` 自動導流至對應的宣告式模型與回應 Resource。

