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
import com.example.demo.application.shared.model.BomItemExcelModel;
import com.example.demo.application.shared.query.ReadExcelQuery;

import static org.assertj.core.api.Assertions.assertThat;

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
}
