# 維度二 Part 2：高傳真錯誤回標檔、前置切面守衛與 OCP 範本註冊 (Error-Marked Excel, Guard Aspect & Template Registry)

> 📌 本文件座落於 [`doc/design/custom-validation-and-error-feedback/02-error-marked-excel-and-guard-aspect.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/custom-validation-and-error-feedback/02-error-marked-excel-and-guard-aspect.md)，屬於「維度二：宣告式資料校驗與錯誤回標」之【Part 2：高傳真錯誤回標檔、前置切面守衛與 OCP 範本註冊】。  
> 詳述 POI 錯誤回標適配器（淡紅底深紅字高亮＋批註＋錯誤原因欄位）、`@ValidateExcel` 宣告式前置守衛切面、`ExcelTemplateRegistry` 開放封閉範本註冊中心、REST API 控制器以及單元測試。  
> 
> 🔗 **模組導讀與關聯文件**：  
> • 模組入口主文件：👉 [維度二：宣告式資料校驗與錯誤回標架構總覽 (custom-validation-and-error-feedback.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/custom-validation-and-error-feedback.md)  
> • EasyRules 規則管線：👉 [維度二 Part 1：EasyRules 檢核管線與全量錯誤診斷模型 (01-easyrules-validation-pipeline.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/custom-validation-and-error-feedback/01-easyrules-validation-pipeline.md)

---

## 1. 視覺化高傳真錯誤回標檔生成 (Error-Marked Feedback)

座落於 [`com.example.demo.infrastructure.adapter.feedback.PoiExcelErrorMarkerAdapter`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/infrastructure/adapter/feedback/PoiExcelErrorMarkerAdapter.java)，實作 [`ExcelErrorMarkerPort`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/out/ExcelErrorMarkerPort.java)：

### 1.1 核心樣式規範
1. **原樣保留**：高傳真讀取使用者上傳之原始試算表（包含自訂字型、列高、框線）。
2. **違規儲存格高亮**：
   * 背景填色：淡紅色 (`#FFC7CE` / `255, 199, 206`)
   * 文字色彩：深紅色 (`#9C0006` / `156, 0, 6`)
   * 批註標記：將具體違規訊息注入儲存格原生批註 (Cell Comment)。
3. **動態追加錯誤說明欄**：在最右側空白欄位追加「檢核結果與錯誤原因」標題，彙總該列所有違規條目（例如：`① 購買金額必須大於 0；② 下單日期不能晚於今日`）。

---

## 2. 表現層前置守衛與切面 (Inbound Guard Aspect)

座落於 [`com.example.demo.presentation.aspect.ExcelValidationAspect`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/presentation/aspect/ExcelValidationAspect.java)：

### 2.1 宣告式方法註解：`@ValidateExcel`
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidateExcel {
    String templateKey() default "";
    Class<?> modelClass() default Void.class;
    ValidationStrategy onValidationFail() default ValidationStrategy.AUTO_FEEDBACK;
}
```

### 2.2 切面行為流程
1. 攔截 Controller 方法中的 `MultipartFile` 參數。
2. 檢查檔案副檔名（`.xlsx`）與非空。
3. 調用應用層 `ValidateExcelUseCase.validateAndMark(...)`。
4. **短路防衛**：
   * 若存在違規且模式為 `AUTO_FEEDBACK`：切面直接短路回傳 `200 OK` 並輸出標紅的 Excel 二進位檔案（檔名自動附加 `_error_marked.xlsx`）。
   * 若模式為 `THROW_EXCEPTION`：拋出 `ExcelValidationException`，轉譯為 HTTP 422 結構化 JSON 診斷清單。
   * 若完全合規：放行並將解析後的乾淨 Model 清單自動反序列化注入 Controller 方法的 `@ValidatedExcelData` 參數。

---

## 3. OCP 集中範本註冊表：`ExcelTemplateRegistry` 與配置組裝

為徹底杜絕業務模型硬編碼對應用層的侵入，系統嚴格區分**「技術元數據容器」**與**「業務範本組態裝配」**：

### 3.1 基礎設施層技術註冊容器：`ExcelTemplateRegistry`
座落於 [`com.example.demo.infrastructure.template.ExcelTemplateRegistry`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/infrastructure/template/ExcelTemplateRegistry.java)：
* **純粹技術容器**：零業務模型相依（不依賴 `OrderModel` 等業務類別），移除 `@Component` 改由配置層裝配。
* **執行緒安全索引**：內部以 `ConcurrentHashMap` 維護主鍵索引 `descriptorMap` 與別名查表 `aliasMap`。
* **智慧反查能力**：
  * **別名容錯匹配**：支援小寫短別名 (`orders`)、單數 (`order`)、模型類別名稱 (`OrderModel`) 寬鬆查詢。
  * **Zero-Config 工作表名稱反查**：自動剖析 `@ExcelSheet(name = "...")`，支援由 Excel 第一個工作表名稱反向定位目標 Java 模型。

```java
package com.example.demo.infrastructure.template;

public class ExcelTemplateRegistry {
    public record TemplateDescriptor(
        String key,
        Class<?> modelClass,
        String sheetName,
        String displayName,
        String feedbackFilename
    ) {}

    private final Map<String, TemplateDescriptor> descriptorMap = new ConcurrentHashMap<>();
    private final Map<String, String> aliasMap = new ConcurrentHashMap<>();

    // 空建構子：純容器，不依賴任何具體業務 Model
    public ExcelTemplateRegistry() {}

    public void register(String key, Class<?> modelClass, String displayName, String feedbackFilename) { ... }
    public void addAlias(String alias, String target) { ... }
    public Optional<TemplateDescriptor> find(String keyOrSheetName) { ... }
    public TemplateDescriptor getRequired(String keyOrSheetName) { ... }
}
```

### 3.2 自訂擴充回呼 SPI 介面：`ExcelTemplateCustomizer`
座落於 [`com.example.demo.infrastructure.template.ExcelTemplateCustomizer`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/infrastructure/template/ExcelTemplateCustomizer.java)：
* 貫徹開放封閉原則 (OCP)，任何子模組欲擴充新單據範本（如「客戶主檔」、「請購單」），僅需宣告實作此介面的 Spring Bean，無需修改現有核心程式碼。

```java
@FunctionalInterface
public interface ExcelTemplateCustomizer {
    void customize(ExcelTemplateRegistry registry);
}
```

### 3.3 全域配置層集中裝配：`ExcelTemplateConfiguration`
座落於 [`com.example.demo.config.ExcelTemplateConfiguration`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/config/ExcelTemplateConfiguration.java)：
* 遵循 `AGENTS.md` 規則 40~41（The Dirtiest Layer / Composition Root），將所有具體業務模型與中文別名集中於配置層裝配。
* 自動收集容器中所有 `ExcelTemplateCustomizer` 擴充器並依序回呼執行。
* 提供 `createDefaultRegistry()` 靜態工廠，供 Spring 初始化與無容器之單元測試快速獲取一致配置。

```java
@Configuration
public class ExcelTemplateConfiguration {

    @Bean
    @ConditionalOnMissingBean(ExcelTemplateRegistry.class)
    public ExcelTemplateRegistry excelTemplateRegistry(
            @Autowired(required = false) List<ExcelTemplateCustomizer> customizers
    ) {
        ExcelTemplateRegistry registry = createDefaultRegistry();
        if (customizers != null) {
            customizers.forEach(c -> c.customize(registry));
        }
        return registry;
    }

    public static ExcelTemplateRegistry createDefaultRegistry() {
        ExcelTemplateRegistry registry = new ExcelTemplateRegistry();
        // 預載 4 大核心範本：orders, products, materials, boms
        registry.register("orders", OrderModel.class, "訂單", "訂單檢核錯誤回標檔.xlsx");
        registry.register("products", ProductRecordModel.class, "商品主檔", "商品檢核錯誤回標檔.xlsx");
        registry.register("materials", MaterialExcelModel.class, "物料主檔", "物料檢核錯誤回標檔.xlsx");
        registry.register("boms", BomItemExcelModel.class, "單階配方", "BOM檢核錯誤回標檔.xlsx");
        return registry;
    }
}
```

---

## 4. REST API 控制器規格

座落於 [`com.example.demo.presentation.controller.ExcelValidationController`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/presentation/controller/ExcelValidationController.java)：

| HTTP 方法 | 端點路徑 | 說明與用途 |
| :--- | :--- | :--- |
| `GET` | `/api/v1/excel/validation/templates` | 查詢系統所有已註冊的單據範本與欄位規格 |
| `POST` | `/api/v1/excel/validation/{templateKey}/feedback` | 上傳特定範本，有錯時直接下載淡紅底深紅字回標檔 |
| `POST` | `/api/v1/excel/validation/{templateKey}` | 上傳特定範本，回傳全量 JSON 錯誤診斷報告 (HTTP 200/422) |
| `POST` | `/api/v1/excel/validation/feedback` | Zero-Config 智慧探測：依工作表名稱自動辨識範本並回標 |
| `POST` | `/api/v1/excel/validation/declarative/orders` | 示範 `@ValidateExcel` AOP 前置短路守衛機制 |

---

## 5. 單元與整合測試驗證 (Testing & Verification)

* [`ExcelTemplateRegistryTest`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/test/java/com/example/demo/infrastructure/template/ExcelTemplateRegistryTest.java)：驗證純空容器建立、配置層預載 4 大業務範本、大小寫與別名智能匹配、工作表名稱反查、OCP 動態註冊與 `ExcelTemplateCustomizer` SPI 擴充管線等 6 大場景。
* [`EasyRulesExcelValidatorAdapterTest`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/test/java/com/example/demo/infrastructure/adapter/validator/EasyRulesExcelValidatorAdapterTest.java)：驗證 EasyRules 行級規則評估、非短路錯誤全量累積與跨欄位連動驗證。
* [`PoiExcelErrorMarkerAdapterTest`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/test/java/com/example/demo/infrastructure/adapter/feedback/PoiExcelErrorMarkerAdapterTest.java)：驗證 POI 原始格式保留、出錯儲存格淡紅底深紅字套用與最右側錯誤原因追加。
* [`ExcelValidationControllerTest`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/test/java/com/example/demo/presentation/controller/ExcelValidationControllerTest.java)：驗證 5 隻端點的檔案上傳、HTTP 422 錯誤報告、檔案下載與 AOP 切面攔截。

