package com.example.demo.application.port.out.template;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * 範本 Excel 寫入輸出埠介面 (Outbound Port)。
 * <p>
 * 遵循 AGENTS.md 規則 3-8，Outbound Port 一律以 {@code Port} 結尾。
 * 由應用層定義、基礎設施層適配器實作之純技術合約。
 * 負責將輸入之範本二進位串流與業務資料模型結合，產出渲染完成之 Excel 二進位檔案。
 * 傳參及回傳型別嚴禁使用任何技術類或環境相依的物件 (如 Workbook, Cell 等)。
 * </p>
 */
public interface ExcelTemplateWriterPort {

    /**
     * 結合範本與資料模型，輸出 Excel 二進位位元組陣列。
     *
     * @param templateStream 範本二進位輸入串流
     * @param dataModel      業務資料模型字典 (鍵值對或模型物件)
     * @return 渲染完成之 Excel 二進位位元組陣列
     */
    byte[] writeFromTemplate(InputStream templateStream, Map<String, Object> dataModel);

    /**
     * 結合範本與資料模型，將結果直接串流寫入目標輸出串流。
     *
     * @param templateStream 範本二進位輸入串流
     * @param targetStream   目標二進位輸出串流
     * @param dataModel      業務資料模型字典 (鍵值對或模型物件)
     */
    void streamWriteFromTemplate(InputStream templateStream, OutputStream targetStream, Map<String, Object> dataModel);
}

