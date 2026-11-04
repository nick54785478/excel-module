package com.example.demo.application.shared.context;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import com.example.demo.application.port.out.ExcelTaskRepositoryPort;
import com.example.demo.application.shared.vo.ExcelTaskType;

/**
 * Excel 長任務執行緒上下文保持器 (Excel Task ThreadLocal Context Holder)
 * <p>
 * 遵循 Clean Architecture 跨層資料透傳機制。
 * 在當前執行緒中存儲非同步長任務之識別碼、總列數、已處理列數與 {@link ExcelTaskRepositoryPort} 引用。
 * 讓深層之 Outbound Adapters (如讀取/寫入串流監聽器) 無需層層傳遞參數即可自發回報進度。
 */
public final class ExcelTaskContextHolder {

	private static final ThreadLocal<TaskContext> CONTEXT = new ThreadLocal<>();

	private ExcelTaskContextHolder() {
	}

	/**
	 * 任務上下文資料載體
	 *
	 * @param taskId         任務識別碼
	 * @param taskType       任務類型
	 * @param fileName       關聯檔案名稱
	 * @param totalRows      預估總列數
	 * @param processedRows  已完成處理列數計數器
	 * @param repositoryPort 儲存庫輸出端口
	 */
	public record TaskContext(
			String taskId,
			ExcelTaskType taskType,
			String fileName,
			long totalRows,
			AtomicLong processedRows,
			ExcelTaskRepositoryPort repositoryPort
	) {
	}

	/**
	 * 綁定當前執行緒之任務上下文
	 *
	 * @param taskId         任務識別碼 (UUID)
	 * @param taskType       任務類型 (IMPORT / EXPORT)
	 * @param fileName       關聯檔案名稱
	 * @param totalRows      預估總列數
	 * @param repositoryPort 儲存庫輸出端口
	 */
	public static void bind(
			String taskId,
			ExcelTaskType taskType,
			String fileName,
			long totalRows,
			ExcelTaskRepositoryPort repositoryPort
	) {
		Objects.requireNonNull(taskId, "taskId 不得為空");
		CONTEXT.set(new TaskContext(
				taskId,
				taskType != null ? taskType : ExcelTaskType.EXPORT,
				fileName,
				totalRows,
				new AtomicLong(0),
				repositoryPort
		));
	}

	/**
	 * 清除當前執行緒之任務上下文，防止記憶體洩漏
	 */
	public static void unbind() {
		CONTEXT.remove();
	}

	/**
	 * 檢查當前執行緒是否處於受監控之任務上下文中
	 *
	 * @return 若存在活動任務則回傳 true
	 */
	public static boolean isActive() {
		return CONTEXT.get() != null;
	}

	/**
	 * 獲取當前任務上下文
	 *
	 * @return 任務上下文 Optional
	 */
	public static Optional<TaskContext> getContext() {
		return Optional.ofNullable(CONTEXT.get());
	}

	/**
	 * 獲取當前任務 ID
	 *
	 * @return 任務 ID (若無活動任務則回傳 null)
	 */
	public static String getTaskId() {
		TaskContext ctx = CONTEXT.get();
		return ctx != null ? ctx.taskId() : null;
	}

	/**
	 * 獲取預計總列數
	 *
	 * @return 總列數 (若無活動任務則回傳 0)
	 */
	public static long getTotalRows() {
		TaskContext ctx = CONTEXT.get();
		return ctx != null ? ctx.totalRows() : 0;
	}

	/**
	 * 獲取當前已處理總列數
	 *
	 * @return 已處理總列數
	 */
	public static long getProcessedRows() {
		TaskContext ctx = CONTEXT.get();
		return ctx != null ? ctx.processedRows().get() : 0;
	}

	/**
	 * 遞增已處理列數並向儲存庫同步最新進度
	 *
	 * @param delta 增加的筆數 (如一個 chunk 的列數)
	 * @return 遞增後的已處理總列數
	 */
	public static long incrementProgress(long delta) {
		TaskContext ctx = CONTEXT.get();
		if (ctx == null) {
			return 0;
		}
		long current = ctx.processedRows().addAndGet(delta);
		if (ctx.repositoryPort() != null) {
			ctx.repositoryPort().updateProgress(ctx.taskId(), current, ctx.totalRows() > 0 ? ctx.totalRows() : current);
		}
		return current;
	}

	/**
	 * 更新已處理列數與總列數並同步儲存庫
	 *
	 * @param processed 已完成列數
	 * @param total     預估總列數
	 */
	public static void updateProgress(long processed, long total) {
		TaskContext ctx = CONTEXT.get();
		if (ctx == null) {
			return;
		}
		ctx.processedRows().set(processed);
		if (ctx.repositoryPort() != null) {
			ctx.repositoryPort().updateProgress(ctx.taskId(), processed, total);
		}
	}
}

