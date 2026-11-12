# 維度一 Part 1：宣告式註解、雙層反射快取與型別容錯轉換 (Annotations, Reflection Cache & Converters)

> 📌 本文件座落於 [`doc/design/declarative-bean-mapping/01-reflection-cache-and-converters.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/declarative-bean-mapping/01-reflection-cache-and-converters.md)，屬於「維度一：宣告式物件映射」之【Part 1：宣告式註解、雙層反射快取與型別容錯轉換】。  
> 詳述 `@ExcelSheet` 與 `@ExcelColumn` 註解系統、JavaBean 與 Java 21 Record 雙軌建構、`ConcurrentHashMap` 雙層反射快取、容錯千分位逗號解析、日期轉換與 Map 雙向橋接。  
> 
> 🔗 **模組導讀與關聯文件**：  
> • 模組入口主文件：👉 [維度一：宣告式物件映射架構總覽 (declarative-bean-mapping.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/declarative-bean-mapping.md)  
> • UseCase 與 REST 端點：👉 [維度一 Part 2：應用層 UseCase、CQRS 服務調度與 REST 端點 (02-ports-usecases-and-rest-binding.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/declarative-bean-mapping/02-ports-usecases-and-rest-binding.md)  
> • 模型註冊與別名路由：👉 [維度一 Part 3：宣告式模型自動探測、別名註冊表與動態工作表路由 (03-sheet-model-registry.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/declarative-bean-mapping/03-sheet-model-registry.md)

---

## 1. 宣告式註解系統 (Declarative Annotations)

座落於 [`com.example.demo.application.annotation`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/annotation)：

### 1.1 `@ExcelSheet`：工作表層級設定與別名相容
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelSheet {
    String value() default "";
    String name() default "";
    String[] aliases() default {};
    int headerRowIndex() default 0;
    int dataStartRowIndex() default 1;
    String protectPassword() default "";
}
```
* `value` / `name`：主要工作表名稱 (Sheet Name)。若未指定則預設使用 Model 類別簡稱。
* `aliases`：相容別名清單 (Sheet Name Aliases)。支援在活頁簿中自動搜尋與多個工作表別名進行宣告式匹配（例如 `aliases = {"物料主檔", "材料清單", "MaterialMaster"}`）。
  - 由應用層輸出埠 [`ExcelSheetModelRegistryPort`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/out/core/ExcelSheetModelRegistryPort.java) 與底層適配器自動建立倒排索引，實現 $O(1)$ 精確規格化匹配、模糊匹配與最長子字串包含比對。
* `headerRowIndex`：標題列索引 (從 0 起算，預設為 0)。
* `dataStartRowIndex`：資料起始列索引 (從 0 起算，預設為 1)。
* `protectPassword`：工作表保護密碼 (選填)。

### 1.2 `@ExcelColumn`：欄位層級設定
```java
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelColumn {
    String value() default "";
    String name() default "";
    int index() default -1;
    String format() default "";
    String defaultValue() default "";
    boolean ignore() default false;
    String formula() default "";
}
```

---

## 2. JavaBean 與 Java 21 Record 雙軌支援模型

```java
// 範例一：傳統標準 JavaBean (支援無參建構子 + Getter/Setter)
@ExcelSheet(name = "訂單匯入清單")
public class OrderImportModel {
    @ExcelColumn(name = "訂單編號", index = 0)
    private String orderNo;

    @ExcelColumn(name = "訂購人", index = 1, defaultValue = "匿名訪客")
    private String customerName;

    @ExcelColumn(name = "購買金額", index = 2, format = "#,##0.00")
    private BigDecimal amount;

    @ExcelColumn(name = "下單日期", index = 3, format = "yyyy-MM-dd")
    private LocalDate orderDate;
}

// 範例二：原生支援 Java 21 不可變 Record (規範建構子 Canonical Constructor 自動依序注入)
@ExcelSheet("商品主檔清單")
public record ProductRecordModel(
    @ExcelColumn(name = "商品代碼", index = 0) String productCode,
    @ExcelColumn(name = "商品名稱", index = 1) String productName,
    @ExcelColumn(name = "售價", index = 2, format = "#,##0.00") BigDecimal price
) {}
```

---

## 3. 高效能反射快取機制 (Cached Reflection Engine)

座落於 [`com.example.demo.application.assembler.ExcelBeanAssembler`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/assembler/ExcelBeanAssembler.java)：
* 以靜態 `ConcurrentHashMap<Class<?>, ModelMetadata>` 快取欄位屬性存取器、Record Components、正規建構子與格式化器。
* 消除重複反射解析開銷，效能逼近手動 Hardcode 取值。

---

## 4. 智慧型別容錯轉換 (Smart Type Converters)

1. **千分位數值容錯**：解析 `"12,500.50"` 時自動過濾逗號，避免拋出 `NumberFormatException`。
2. **日期時間解析**：支援字串轉 `LocalDate`、`LocalDateTime`、`Date`，並依 `@ExcelColumn(format = "...")` 格式化。
3. **Map 與 Bean 雙向流轉**：
   * `toMaps(beans, modelClass)`：將強型別清單轉為 `List<Map<String, Object>>`。
   * `fromMaps(mapList, modelClass)`：將字典清單反序列化為強型別 Bean 清單。

