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
import com.example.demo.application.shared.model.MultiLevelBomExcelModel;
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

    @Autowired
    private MaterialBomExcelController materialBomExcelController;

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
     * Feature: 未填 sheetName 時宣告式全量解析相符之單階 BOM 工作表
     * Scenario: 調用 importBeanSheet(in, BomItemExcelModel.class) 未指定工作表名稱
     * Given: 磁碟上的真實 BOM_IMPORT.xlsx 範本檔案 (內含單階BOM匯入清單)
     * When: 未傳入 sheetName 執行單階 BOM 宣告式匯入
     * Then: 正確解析「單階BOM匯入清單」，總計 16 筆乾淨的單階配方項目 (具備項次序號 10~80)
     */
    @Test
    @DisplayName("場景：未填 sheetName 時單階 BOM 模型精準解析單階BOM匯入清單")
    void testImportBeanSheetAnnotationMode() throws Exception {
        Path path = Paths.get("src/main/resources/templates/BOM_IMPORT.xlsx");
        try (InputStream in = Files.newInputStream(path)) {
            BeanSheetGottenResult<BomItemExcelModel> result = importExcelUseCase.importBeanSheet(
                    in,
                    BomItemExcelModel.class
            );
            assertThat(result.sheetName()).isEqualTo("單階BOM匯入清單");
            assertThat(result.totalCount()).isEqualTo(16);
            assertThat(result.items()).hasSize(16);
            assertThat(result.items().get(0).getItemSequence()).isEqualTo(10);
            assertThat(result.items().get(0).getBomVersion()).isEqualTo("V1.0");
        }
    }

    /**
     * Feature: 多階層 BOM 模型宣告式全量解析多階層工作表
     * Scenario: 調用 importBeanSheet(in, MultiLevelBomExcelModel.class) 未指定工作表名稱
     * Given: 磁碟上的真實 BOM_IMPORT.xlsx 範本檔案 (內含多階層BOM展開清單)
     * When: 未傳入 sheetName 執行多階層 BOM 宣告式匯入
     * Then: 正確解析「多階層BOM展開清單」，總計 27 筆具備階層 (0~2)、WBS 序號、規格、有效用量與單價之完整項目
     */
    @Test
    @DisplayName("場景：多階層 BOM 模型精準解析多階層BOM展開清單")
    void testImportMultiLevelBeanSheetAnnotationMode() throws Exception {
        Path path = Paths.get("src/main/resources/templates/BOM_IMPORT.xlsx");
        try (InputStream in = Files.newInputStream(path)) {
            BeanSheetGottenResult<MultiLevelBomExcelModel> result = importExcelUseCase.importBeanSheet(
                    in,
                    MultiLevelBomExcelModel.class
            );
            assertThat(result.sheetName()).isEqualTo("多階層BOM展開清單");
            assertThat(result.totalCount()).isEqualTo(27);
            assertThat(result.items()).hasSize(27);
            assertThat(result.items().get(0).getLevel()).isEqualTo(0);
            assertThat(result.items().get(0).getLevelSequence()).isEqualTo("1");
            assertThat(result.items().get(1).getLevel()).isEqualTo(1);
            assertThat(result.items().get(1).getLevelSequence()).isEqualTo("1.1");
            assertThat(result.items().get(1).getEffectiveQuantity()).isNotNull();
        }
    }

    /**
     * Feature: 指定工作表名稱精確解析多階層 BOM
     * Scenario: 調用 importBeanSheet(in, "多階層BOM展開清單", MultiLevelBomExcelModel.class)
     * Given: 磁碟上的真實 BOM_IMPORT.xlsx 範本檔案
     * When: 指定讀取「多階層BOM展開清單」
     * Then: 正確解析出「多階層BOM展開清單」為工作表名稱，且解析 27 筆多階層資料
     */
    @Test
    @DisplayName("場景：SheetName 模式指定多階層工作表精確解析")
    void testImportBeanSheetSheetNameMode() throws Exception {
        Path path = Paths.get("src/main/resources/templates/BOM_IMPORT.xlsx");
        try (InputStream in = Files.newInputStream(path)) {
            BeanSheetGottenResult<MultiLevelBomExcelModel> result = importExcelUseCase.importBeanSheet(
                    in,
                    "多階層BOM展開清單",
                    MultiLevelBomExcelModel.class
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

    /**
     * Feature: 未指定 sheetName 時全量讀取活頁簿所有相符工作表 (單階、多階與物料主檔)
     * Scenario: 呼叫 importBomItems 未指定 sheetName 上傳真實 BOM_IMPORT.xlsx
     * Given: 包含多階展開 (27筆)、單階匯入 (16筆) 與物料主檔對照表 (19筆) 的真實活頁簿
     * When: 調用 materialBomExcelController.importBomItems 未指定 sheetName
     * Then: 成功回傳包含 62 筆之結構，且 singleLevelBom (16)、multiLevelBom (27) 與 materialMaster (19) 皆完整印出
     */
    @Test
    @DisplayName("場景：Controller 匯入未填 sheetName 時三張工作表全數完整印出")
    void testControllerImportAllThreeSheets() throws Exception {
        Path path = Paths.get("src/main/resources/templates/BOM_IMPORT.xlsx");
        byte[] bytes = Files.readAllBytes(path);
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "BOM_IMPORT.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        var response = materialBomExcelController.importBomItems(file, null);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.totalCount()).isEqualTo(62); // 16 + 27 + 19
        assertThat(body.singleLevelBom()).isNotNull();
        assertThat(body.singleLevelBom().totalCount()).isEqualTo(16);
        assertThat(body.multiLevelBom()).isNotNull();
        assertThat(body.multiLevelBom().totalCount()).isEqualTo(27);
        assertThat(body.materialMaster()).isNotNull();
        assertThat(body.materialMaster().totalCount()).isEqualTo(19);
    }

    /**
     * Feature: Controller 匯入時指定 sheetName 為物料主檔對照表
     * Scenario: 呼叫 importBomItems 指定 sheetName="物料主檔對照表"
     * Given: 包含物料主檔對照表 (19筆) 之真實活頁簿
     * When: 調用 materialBomExcelController.importBomItems 指定 sheetName="物料主檔對照表"
     * Then: 成功回傳包含 19 筆之 materialMaster 結構
     */
    @Test
    @DisplayName("場景：Controller 匯入時指定 sheetName 為物料主檔對照表成功解析")
    void testControllerImportMaterialMasterBySheet() throws Exception {
        Path path = Paths.get("src/main/resources/templates/BOM_IMPORT.xlsx");
        byte[] bytes = Files.readAllBytes(path);
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "BOM_IMPORT.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        var response = materialBomExcelController.importBomItems(file, "物料主檔對照表");
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.totalCount()).isEqualTo(19);
        assertThat(body.materialMaster()).isNotNull();
        assertThat(body.materialMaster().totalCount()).isEqualTo(19);
    }
}
