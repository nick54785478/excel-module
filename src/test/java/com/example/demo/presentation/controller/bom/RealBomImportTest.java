package com.example.demo.presentation.controller.bom;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.application.port.in.core.ImportExcelUseCase;
import com.example.demo.application.shared.dto.BeanSheetGottenResult;
import com.example.demo.application.shared.model.BomItemExcelModel;
import com.example.demo.application.shared.model.MaterialExcelModel;
import com.example.demo.application.shared.query.ReadExcelQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BOM Excel 實體檔案匯入與宣告式解析整合測試。
 */
@SpringBootTest
@DisplayName("BOM Excel 實體檔案匯入宣告式解析整合測試")
public class RealBomImportTest {

    @Autowired
    private ImportExcelUseCase importExcelUseCase;

    /**
     * Feature: 多階層 BOM 工作表宣告式匯入解析
     * Scenario: 上傳真實 BOM_IMPORT.xlsx 並指定讀取「多階層BOM展開清單」工作表
     * Given: 磁碟上的真實 BOM_IMPORT.xlsx 範本檔案
     * When: 調用 importExcelUseCase.importBeans 指定 ReadExcelQuery.ofSheet("多階層BOM展開清單")
     * Then: 成功解析出 27 筆 BomItemExcelModel，包含含有百分比符號之損耗率數值
     */
    @Test
    @DisplayName("場景：指定多階層BOM展開清單工作表成功解析")
    void testImportMultiLevelSheet() throws Exception {
        Path path = Paths.get("src/main/resources/templates/BOM_IMPORT.xlsx");
        try (InputStream in = Files.newInputStream(path)) {
            List<BomItemExcelModel> models = importExcelUseCase.importBeans(
                    in,
                    ReadExcelQuery.ofSheet("多階層BOM展開清單"),
                    BomItemExcelModel.class
            );
            assertThat(models).hasSize(27);
        }
    }

    /**
     * Feature: 單階 BOM 配方清單宣告式匯入解析
     * Scenario: 上傳真實 BOM_IMPORT.xlsx 並指定讀取「單階BOM匯入清單」工作表
     * Given: 磁碟上的真實 BOM_IMPORT.xlsx 範本檔案
     * When: 調用 importExcelUseCase.importBeans 指定 ReadExcelQuery.ofSheet("單階BOM匯入清單")
     * Then: 成功解析出 16 筆 BomItemExcelModel
     */
    @Test
    @DisplayName("場景：指定單階BOM匯入清單工作表成功解析")
    void testImportSingleLevelSheet() throws Exception {
        Path path = Paths.get("src/main/resources/templates/BOM_IMPORT.xlsx");
        try (InputStream in = Files.newInputStream(path)) {
            List<BomItemExcelModel> models = importExcelUseCase.importBeans(
                    in,
                    ReadExcelQuery.ofSheet("單階BOM匯入清單"),
                    BomItemExcelModel.class
            );
            assertThat(models).hasSize(16);
        }
    }

    /**
     * Feature: BOM 活頁簿自動智慧探測工作表匯入解析
     * Scenario: 上傳真實 BOM_IMPORT.xlsx 未指定工作表名稱，依據 @ExcelSheet 智慧比對
     * Given: 磁碟上的真實 BOM_IMPORT.xlsx 範本檔案
     * When: 調用 importExcelUseCase.importBeans 未指定工作表名稱
     * Then: 自動依據 @ExcelSheet(name="單階BOM清單") 模糊匹配「單階BOM匯入清單」，成功解析 16 筆模型
     */
    @Test
    @DisplayName("場景：未指定工作表名稱時自動智慧探測匹配解析")
    void testImportDefaultAutoDetection() throws Exception {
        Path path = Paths.get("src/main/resources/templates/BOM_IMPORT.xlsx");
        try (InputStream in = Files.newInputStream(path)) {
            List<BomItemExcelModel> models = importExcelUseCase.importBeans(
                    in,
                    BomItemExcelModel.class
            );
            assertThat(models).hasSize(16);
        }
    }

    /**
     * Feature: 未填 sheetName 時宣告式全量解析所有相符工作表 (All-Sheets Mode)
     * Scenario: 調用 importBeanSheet(in, BomItemExcelModel.class) 未指定工作表名稱
     * Given: 磁碟上的真實 BOM_IMPORT.xlsx 範本檔案 (內含多階層與單階兩張相符之 BOM 表)
     * When: 未傳入 sheetName 執行宣告式匯入
     * Then: 自動走訪並將「多階層BOM展開清單」(27筆) 與「單階BOM匯入清單」(16筆) 全數讀出合併，總計 43 筆
     */
    @Test
    @DisplayName("場景：未填 sheetName 時自動將所有相符之 BOM 工作表全量讀出")
    void testImportBeanSheetAnnotationMode() throws Exception {
        Path path = Paths.get("src/main/resources/templates/BOM_IMPORT.xlsx");
        try (InputStream in = Files.newInputStream(path)) {
            BeanSheetGottenResult<BomItemExcelModel> result = importExcelUseCase.importBeanSheet(
                    in,
                    BomItemExcelModel.class
            );
            assertThat(result.sheetName()).contains("多階層BOM展開清單").contains("單階BOM匯入清單");
            assertThat(result.totalCount()).isEqualTo(43);
            assertThat(result.items()).hasSize(43);
        }
    }

    /**
     * Feature: 指定工作表名稱解析並獲取工作表名稱與資料 (SheetName-Driven)
     * Scenario: 調用 importBeanSheet(in, "多階層BOM展開清單", BomItemExcelModel.class)
     * Given: 磁碟上的真實 BOM_IMPORT.xlsx 範本檔案
     * When: 指定讀取「多階層BOM展開清單」
     * Then: 正確解析出「多階層BOM展開清單」為工作表名稱，且解析 27 筆資料
     */
    @Test
    @DisplayName("場景：SheetName 模式指定工作表精確解析")
    void testImportBeanSheetSheetNameMode() throws Exception {
        Path path = Paths.get("src/main/resources/templates/BOM_IMPORT.xlsx");
        try (InputStream in = Files.newInputStream(path)) {
            BeanSheetGottenResult<BomItemExcelModel> result = importExcelUseCase.importBeanSheet(
                    in,
                    "多階層BOM展開清單",
                    BomItemExcelModel.class
            );
            assertThat(result.sheetName()).isEqualTo("多階層BOM展開清單");
            assertThat(result.totalCount()).isEqualTo(27);
            assertThat(result.items()).hasSize(27);
        }
    }

    /**
     * Feature: 指定不存在之工作表時嚴格拋出例外
     * Scenario: 調用 importBeanSheet(in, "不存在的配方表", BomItemExcelModel.class)
     * Given: 磁碟上的真實 BOM_IMPORT.xlsx 範本檔案
     * When: 指定不存在之工作表名稱
     * Then: 拋出 IllegalArgumentException 例外，訊息指明找不到工作表
     */
    @Test
    @DisplayName("異常：SheetName 模式指定不存在工作表時拋出例外")
    void testImportBeanSheetSheetNotFoundThrowsException() throws Exception {
        Path path = Paths.get("src/main/resources/templates/BOM_IMPORT.xlsx");
        try (InputStream in = Files.newInputStream(path)) {
            assertThatThrownBy(() -> importExcelUseCase.importBeanSheet(
                    in,
                    "不存在的配方表",
                    BomItemExcelModel.class
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("在 Excel 活頁簿中找不到指定名稱之工作表: 不存在的配方表");
        }
    }

    /**
     * Feature: 物料主檔純註解宣告式解析並獲取實際工作表名稱 (Annotation-Driven)
     * Scenario: 調用 importBeanSheet(in, MaterialExcelModel.class)
     * Given: 磁碟上的真實 BOM_IMPORT.xlsx 範本檔案
     * When: 自動依據 @ExcelSheet(name="物料主檔清單") 探測工作表
     * Then: 正確解析出「物料主檔對照表」為真實工作表名稱，且解析 19 筆資料
     */
    @Test
    @DisplayName("場景：物料主檔 Annotation 模式自動匹配物料主檔對照表")
    void testImportMaterialBeanSheetAnnotationMode() throws Exception {
        Path path = Paths.get("src/main/resources/templates/BOM_IMPORT.xlsx");
        try (InputStream in = Files.newInputStream(path)) {
            BeanSheetGottenResult<MaterialExcelModel> result = importExcelUseCase.importBeanSheet(
                    in,
                    MaterialExcelModel.class
            );
            assertThat(result.sheetName()).isEqualTo("物料主檔對照表");
            assertThat(result.totalCount()).isEqualTo(19);
            assertThat(result.items()).hasSize(19);
        }
    }
}
