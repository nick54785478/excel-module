package com.example.demo.infrastructure.persistence.task.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import com.example.demo.application.shared.dto.ExcelTaskGottenResult;
import com.example.demo.application.shared.vo.ExcelTaskStatus;
import com.example.demo.application.shared.vo.ExcelTaskType;

/**
 * Excel 長任務 JPA 持久化實體 (Excel Task JPA Entity)
 * <p>
 * 隸屬於基礎設施層 (Infrastructure Layer)，映射關聯式資料庫之 {@code t_excel_task} 資料表。
 * 負責記錄非同步匯出與匯入任務之生命週期狀態、總筆數、已處理筆數與進度百分比。
 * 嚴禁將此實體直接對外傳遞至 Port 或 Controller，以確保分層隔離。
 */
@Entity
@Table(name = "t_excel_task")
public class ExcelTaskJpaEntity {

	@Id
	@Column(name = "task_id", length = 64, nullable = false)
	private String taskId;

	@Enumerated(EnumType.STRING)
	@Column(name = "task_type", length = 20, nullable = false)
	private ExcelTaskType taskType;

	@Enumerated(EnumType.STRING)
	@Column(name = "task_status", length = 20, nullable = false)
	private ExcelTaskStatus taskStatus;

	@Column(name = "total_rows", nullable = false)
	private long totalRows;

	@Column(name = "processed_rows", nullable = false)
	private long processedRows;

	@Column(name = "progress_percentage", nullable = false)
	private double progressPercentage;

	@Column(name = "file_name", length = 255)
	private String fileName;

	@Column(name = "file_url", length = 1000)
	private String fileUrl;

	@Column(name = "error_message", length = 2000)
	private String errorMessage;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	/**
	 * JPA 規範之無參建構子
	 */
	protected ExcelTaskJpaEntity() {
	}

	/**
	 * 全參初始化建構子
	 */
	public ExcelTaskJpaEntity(
			String taskId,
			ExcelTaskType taskType,
			ExcelTaskStatus taskStatus,
			long totalRows,
			long processedRows,
			double progressPercentage,
			String fileName,
			String fileUrl,
			String errorMessage
	) {
		this.taskId = Objects.requireNonNull(taskId, "taskId 不得為空");
		this.taskType = Objects.requireNonNull(taskType, "taskType 不得為空");
		this.taskStatus = Objects.requireNonNull(taskStatus, "taskStatus 不得為空");
		this.totalRows = totalRows;
		this.processedRows = processedRows;
		this.progressPercentage = progressPercentage;
		this.fileName = fileName;
		this.fileUrl = fileUrl;
		this.errorMessage = errorMessage;
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PrePersist
	protected void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	/**
	 * 更新任務處理進度
	 *
	 * @param processed 當前已完成列數
	 * @param total     預估總列數
	 */
	public void updateProgress(long processed, long total) {
		this.processedRows = processed;
		this.taskStatus = ExcelTaskStatus.PROCESSING;
		if (total > 0) {
			this.totalRows = total;
			double rawPercent = ((double) processed / total) * 100.0;
			this.progressPercentage = Math.min(100.0, Math.round(rawPercent * 100.0) / 100.0);
		}
	}

	/**
	 * 標記任務為處理中 (PROCESSING)
	 */
	public void markProcessing() {
		this.taskStatus = ExcelTaskStatus.PROCESSING;
	}

	/**
	 * 標記任務為已完成 (COMPLETED)
	 *
	 * @param fileUrl 產出檔案位址
	 * @param total   最終總列數
	 */
	public void markCompleted(String fileUrl, long total) {
		this.taskStatus = ExcelTaskStatus.COMPLETED;
		this.fileUrl = fileUrl;
		this.progressPercentage = 100.0;
		if (total > 0) {
			this.totalRows = total;
			this.processedRows = total;
		}
	}

	/**
	 * 標記任務為失敗 (FAILED)
	 *
	 * @param errorMessage 錯誤摘要原因
	 */
	public void markFailed(String errorMessage) {
		this.taskStatus = ExcelTaskStatus.FAILED;
		this.errorMessage = errorMessage;
	}

	/**
	 * 將 JPA 實體轉換為應用層純 DTO
	 *
	 * @return ExcelTaskGottenResult 資料傳輸物件
	 */
	public ExcelTaskGottenResult toDto() {
		return new ExcelTaskGottenResult(
				this.taskId,
				this.taskType,
				this.taskStatus,
				this.totalRows,
				this.processedRows,
				this.progressPercentage,
				this.fileName,
				this.fileUrl,
				this.errorMessage,
				this.createdAt != null ? this.createdAt : LocalDateTime.now(),
				this.updatedAt != null ? this.updatedAt : LocalDateTime.now()
		);
	}

	// =========================================================================
	// Getters and Setters
	// =========================================================================

	public String getTaskId() {
		return taskId;
	}

	public ExcelTaskType getTaskType() {
		return taskType;
	}

	public ExcelTaskStatus getTaskStatus() {
		return taskStatus;
	}

	public long getTotalRows() {
		return totalRows;
	}

	public long getProcessedRows() {
		return processedRows;
	}

	public double getProgressPercentage() {
		return progressPercentage;
	}

	public String getFileName() {
		return fileName;
	}

	public String getFileUrl() {
		return fileUrl;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}

