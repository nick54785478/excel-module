package com.example.demo.application.shared.command;

import java.util.Objects;

/**
 * 刪除或下架 Excel 範本命令載體 (Command)。
 * <p>
 * 遵循 AGENTS.md 規則 3-4，以 {@code V + N + Command} 命名。
 * 應用層純資料載體 (Java 21 Record)。
 * </p>
 *
 * @param templateKey 欲刪除或下架之範本唯一識別鍵
 */
public record DeleteExcelTemplateCommand(
        String templateKey
) {
    public DeleteExcelTemplateCommand {
        Objects.requireNonNull(templateKey, "templateKey 不得為空");
    }
}
