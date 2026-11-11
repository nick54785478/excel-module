package com.example.demo.application.port.out.template;

import com.example.demo.application.shared.command.CreateExcelTemplateCommand;
import com.example.demo.application.shared.command.UpdateExcelTemplateCommand;
import com.example.demo.application.shared.dto.ExcelTemplateGottenResult;

import java.util.List;
import java.util.Optional;

/**
 * Excel 範本持久化儲存庫輸出埠 (Outbound Port)。
 * <p>
 * 遵循 AGENTS.md 規則 3-8，Outbound Port 一律以 {@code Port} 結尾。
 * 隸屬於應用層 (Application Layer)，定義對基礎設施持久化層的資料存取抽象契約。
 * <b>嚴禁接發任何技術類或 ORM 實體 (如 ExcelTemplateJpaEntity)</b>，僅使用 Command 與 DTO。
 * </p>
 */
public interface ExcelTemplateRepositoryPort {

    /**
     * 建立並儲存新的 Excel 範本持久化實體。
     *
     * @param command 建立範本命令
     * @return 儲存後之範本中繼資訊 DTO
     */
    ExcelTemplateGottenResult save(CreateExcelTemplateCommand command);

    /**
     * 更新既有 Excel 範本持久化實體。
     *
     * @param command 更新範本命令
     * @return 更新後之範本中繼資訊 DTO
     */
    ExcelTemplateGottenResult update(UpdateExcelTemplateCommand command);

    /**
     * 依據範本唯一識別鍵查詢範本。
     *
     * @param templateKey 範本唯一識別鍵
     * @return 範本中繼資訊 DTO 包裝
     */
    Optional<ExcelTemplateGottenResult> findByTemplateKey(String templateKey);

    /**
     * 查詢全系統所有範本清單 (包含啟用與停用)。
     *
     * @return 範本中繼資訊清單
     */
    List<ExcelTemplateGottenResult> findAll();

    /**
     * 查詢所有啟用中之範本清單。
     *
     * @return 啟用中範本中繼資訊清單
     */
    List<ExcelTemplateGottenResult> findByActiveTrue();

    /**
     * 依據業務分類查詢啟用中之範本清單。
     *
     * @param category 業務分類 (例如 "PROCUREMENT", "BOM", "MATERIAL")
     * @return 符合業務分類之範本中繼資訊清單
     */
    List<ExcelTemplateGottenResult> findByCategory(String category);

    /**
     * 檢查指定識別鍵之範本是否存在。
     *
     * @param templateKey 範本唯一識別鍵
     * @return 若存在回傳 true，否則回傳 false
     */
    boolean existsByTemplateKey(String templateKey);

    /**
     * 依據範本識別鍵刪除或標記下架範本。
     *
     * @param templateKey 範本唯一識別鍵
     */
    void deleteByTemplateKey(String templateKey);
}
