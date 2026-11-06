package com.example.demo.infrastructure.adapter.persistence;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.port.out.ExcelTaskRepositoryPort;
import com.example.demo.application.shared.command.CreateExcelTaskCommand;
import com.example.demo.application.shared.dto.ExcelTaskGottenResult;
import com.example.demo.application.shared.vo.ExcelTaskStatus;
import com.example.demo.infrastructure.persistence.task.entity.ExcelTaskJpaEntity;
import com.example.demo.infrastructure.persistence.task.repository.ExcelTaskJpaRepository;

/**
 * Excel 長任務儲存庫輸出適配器 (Excel Task Repository Outbound Adapter)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範 (AGENTS.md 規則 30/56)，實作應用層
 * {@link ExcelTaskRepositoryPort}。
 * 協調 Spring Data JPA {@link ExcelTaskJpaRepository} 進行非同步長任務的建立、進度更新與查詢。
 * <b>嚴守防腐原則，禁止對外暴露 ORM 實體，僅接收與回傳純 DTO / Command</b>。
 * <p>
 * 寫入與更新方法強制宣告 {@code Propagation.REQUIRES_NEW} 獨立事務，
 * 確保在唯讀業務事務 (如 {@code @Transactional(readOnly = true)}) 中亦能獨立開啟可寫連線提交進度，
 * 同時防禦業務方法例外回滾時遺失失敗日誌。
 */
@Component
public class ExcelTaskRepositoryAdapter implements ExcelTaskRepositoryPort {

	private final ExcelTaskJpaRepository taskJpaRepository;

	public ExcelTaskRepositoryAdapter(ExcelTaskJpaRepository taskJpaRepository) {
		this.taskJpaRepository = Objects.requireNonNull(taskJpaRepository, "ExcelTaskJpaRepository 不得為空");
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public ExcelTaskGottenResult createTask(CreateExcelTaskCommand command) {
		Objects.requireNonNull(command, "CreateExcelTaskCommand 不得為空");

		ExcelTaskJpaEntity entity = new ExcelTaskJpaEntity(
				command.taskId(),
				command.taskType(),
				ExcelTaskStatus.PENDING,
				command.totalRows(),
				0L,
				0.0,
				command.fileName(),
				null,
				null);

		ExcelTaskJpaEntity saved = taskJpaRepository.saveAndFlush(entity);
		return saved.toDto();
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public ExcelTaskGottenResult updateProgress(String taskId, long processedRows, long totalRows) {
		Objects.requireNonNull(taskId, "taskId 不得為空");

		ExcelTaskJpaEntity entity = taskJpaRepository.findById(taskId)
				.orElseThrow(() -> new IllegalArgumentException("更新進度失敗，查無長任務實體: " + taskId));

		entity.updateProgress(processedRows, totalRows);
		ExcelTaskJpaEntity saved = taskJpaRepository.saveAndFlush(entity);
		return saved.toDto();
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public ExcelTaskGottenResult markProcessing(String taskId) {
		Objects.requireNonNull(taskId, "taskId 不得為空");

		ExcelTaskJpaEntity entity = taskJpaRepository.findById(taskId)
				.orElseThrow(() -> new IllegalArgumentException("標記處理中失敗，查無長任務實體: " + taskId));

		entity.markProcessing();
		ExcelTaskJpaEntity saved = taskJpaRepository.saveAndFlush(entity);
		return saved.toDto();
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public ExcelTaskGottenResult markCompleted(String taskId, String fileUrl, long totalRows) {
		Objects.requireNonNull(taskId, "taskId 不得為空");

		ExcelTaskJpaEntity entity = taskJpaRepository.findById(taskId)
				.orElseThrow(() -> new IllegalArgumentException("標記完成失敗，查無長任務實體: " + taskId));

		entity.markCompleted(fileUrl, totalRows);
		ExcelTaskJpaEntity saved = taskJpaRepository.saveAndFlush(entity);
		return saved.toDto();
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public ExcelTaskGottenResult markFailed(String taskId, String errorMessage) {
		Objects.requireNonNull(taskId, "taskId 不得為空");

		ExcelTaskJpaEntity entity = taskJpaRepository.findById(taskId)
				.orElseThrow(() -> new IllegalArgumentException("標記失敗狀態失敗，查無長任務實體: " + taskId));

		entity.markFailed(errorMessage);
		ExcelTaskJpaEntity saved = taskJpaRepository.saveAndFlush(entity);
		return saved.toDto();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ExcelTaskGottenResult> findTaskById(String taskId) {
		Objects.requireNonNull(taskId, "taskId 不得為空");
		return taskJpaRepository.findById(taskId).map(ExcelTaskJpaEntity::toDto);
	}
}
