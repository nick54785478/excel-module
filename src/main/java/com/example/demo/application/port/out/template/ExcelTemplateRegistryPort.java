package com.example.demo.application.port.out.template;

import com.example.demo.application.shared.dto.ExcelTemplateGottenResult;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * 範本儲存庫管理與中繼規格查詢輸出埠 (Outbound Port)。
 * <p>
 * 遵循 AGENTS.md 規則 3-8，Outbound Port 一律以 {@code Port} 結尾。
 * 負責範本檔案的查找、二進位串流載入與中繼規格驗證。
 * </p>
 */
public interface ExcelTemplateRegistryPort {

    /**
     * 依據範本鍵值開啟範本二進位輸入串流。
     *
     * @param templateKey 範本唯一識別鍵 (例如 "PROCUREMENT_COMPARISON")
     * @return 範本二進位輸入串流
     */
    InputStream openTemplateStream(String templateKey);

    /**
     * 查詢指定範本之中繼規格資訊。
     *
     * @param templateKey 範本唯一識別鍵
     * @return 範本中繼規格資訊 (若不存在則回傳 empty)
     */
    Optional<ExcelTemplateGottenResult> findTemplateInfo(String templateKey);

    /**
     * 列出系統內所有已註冊之範本規格清單。
     *
     * @return 範本規格清單
     */
    List<ExcelTemplateGottenResult> listTemplates();

    /**
     * 檢查指定範本鍵值是否存在於儲存庫中。
     *
     * @param templateKey 範本唯一識別鍵
     * @return true 若範本已註冊且實體檔案存在
     */
    boolean exists(String templateKey);
}

