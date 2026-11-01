/**
 * 輸入端口 (Inbound Ports / Use Cases)
 * <p>
 * 代表應用層對外開放的業務合約，接收 Command/Query 並回傳 DTO/View。
 * 命名規範：以 {@code UseCase} 結尾 (例如：{@code ExportExcelUseCase}, {@code ImportExcelUseCase}, {@code GetExcelUseCase})。
 * 嚴禁在此介面方法中使用任何技術類或環境相依物件 (如 HttpServletRequest、ORM Entity 等)。
 */
package com.example.demo.application.port.in;
