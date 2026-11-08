# 維度一 Part 1：宣告式註解、雙層反射快取與型別容錯轉換 (Annotations, Reflection Cache & Converters)

> 📌 本文件座落於 [`doc/design/declarative-bean-mapping/01-reflection-cache-and-converters.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/declarative-bean-mapping/01-reflection-cache-and-converters.md)，屬於「維度一：宣告式物件映射」之【Part 1：宣告式註解、雙層反射快取與型別容錯轉換】。  
> 詳述 `@ExcelSheet` 與 `@ExcelColumn` 註解系統、JavaBean 與 Java 21 Record 雙軌建構、`ConcurrentHashMap` 雙層反射快取、容錯千分位逗號解析、日期轉換與 Map 雙向橋接。  
> 
> 🔗 **模組導讀與關聯文件**：  
> • 模組入口主文件：👉 [維度一：宣告式物件映射架構總覽 (declarative-bean-mapping.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/declarative-bean-mapping.md)  
> • UseCase 與 REST 端點：👉 [維度一 Part 2：應用層 UseCase、CQRS 服務調度與 REST 端點 (02-ports-usecases-and-rest-binding.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/declarative-bean-mapping/02-ports-usecases-and-rest-binding.md)

---

## 1. 宣告式註解系統 (Declarative Annotations)

座落於 [`com.example.demo.application.annotation`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/annotation)：

### 1.1 `@ExcelSheet`：工作表層級設定
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelSheet {
    String value() default "Sheet1";
    String name() default "";
    int headerRowIndex() default 0;
    int dataStartRowIndex() default 1;
    String protectPassword() default "";
}
```

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

