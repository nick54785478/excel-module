package com.example.demo.infrastructure.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import com.example.demo.application.annotation.task.TrackExcelTask;
import com.example.demo.application.port.out.task.ExcelTaskRepositoryPort;
import com.example.demo.application.port.out.core.ExcelWriterPort;
import com.example.demo.application.shared.command.CreateExcelTaskCommand;
import com.example.demo.application.shared.command.WriteStreamExcelCommand;
import com.example.demo.application.shared.command.WriteStreamSheetCommand;
import com.example.demo.application.shared.context.ExcelTaskContextHolder;
import com.example.demo.application.shared.dto.ExcelTaskGottenResult;
import com.example.demo.application.shared.vo.ExcelTaskStatus;
import com.example.demo.application.shared.vo.ExcelTaskType;
import com.example.demo.infrastructure.adapter.core.FastExcelWriterAdapter;
import com.example.demo.infrastructure.adapter.core.PoiSxssfWriterAdapter;

/**
 * Excel 長任務宣告式追蹤切面單元測試 (ExcelTaskTrackingAspect Test)
 * <p>
 * 針對 {@link ExcelTaskTrackingAspect} 驗證透過 AOP 切面攔截 {@link TrackExcelTask}
 * 註解方法之任務初始建立、SpEL 參數解析、執行緒上下文綁定、進度同步、正常完成與異常兜底。
 * 遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 測試註解規範。
 */
@DisplayName("ExcelTaskTrackingAspect 宣告式切面測試")
class ExcelTaskTrackingAspectTest {

	private ExcelTaskRepositoryPort excelTaskRepositoryPort;
	private SampleService proxy;

	static class SampleService {

		@TrackExcelTask(type = ExcelTaskType.EXPORT, fileName = "#fileName", totalRows = "#totalRows")
		public byte[] exportSuccess(String fileName, long totalRows) {
			ExcelTaskContextHolder.incrementProgress(totalRows);
			return new byte[] { 1, 2, 3 };
		}

		@TrackExcelTask(type = ExcelTaskType.EXPORT, fileName = "#fileName")
		public void exportFailure(String fileName) {
			throw new IllegalStateException("磁碟空間已滿");
		}

		@TrackExcelTask(type = ExcelTaskType.EXPORT, fileName = "#fileName", totalRows = "#totalRows", async = true)
		public ExcelTaskGottenResult exportAsync(String fileName, long totalRows) {
			ExcelTaskContextHolder.incrementProgress(totalRows);
			return null;
		}
	}

	@BeforeEach
	void setUp() {
		this.excelTaskRepositoryPort = mock(ExcelTaskRepositoryPort.class);

		when(excelTaskRepositoryPort.createTask(any(CreateExcelTaskCommand.class)))
				.thenAnswer(invocation -> {
					CreateExcelTaskCommand cmd = invocation.getArgument(0);
					return new ExcelTaskGottenResult(
							cmd.taskId(),
							cmd.taskType(),
							ExcelTaskStatus.PENDING,
							cmd.totalRows(),
							0L,
							0.0,
							cmd.fileName(),
							null,
							null,
							LocalDateTime.now(),
							LocalDateTime.now()
					);
				});

		ExcelTaskTrackingAspect aspect = new ExcelTaskTrackingAspect(excelTaskRepositoryPort, Runnable::run);

		SampleService target = new SampleService();
		AspectJProxyFactory factory = new AspectJProxyFactory(target);
		factory.addAspect(aspect);
		this.proxy = factory.getProxy();
	}

	/**
	 * Feature: 宣告式同步任務追蹤業務
	 * Scenario: 調用標註 @TrackExcelTask 方法時自動建立任務、綁定 Context 並標記 COMPLETED
	 * Given: 代理方法 exportSuccess("report.xlsx", 500)
	 * When: 執行方法調用
	 * Then: 成功返回 byte[]，並驗證 createTask、markProcessing 與 markCompleted 被依序調用
	 */
	@Test
	@DisplayName("場景：成功透過 @TrackExcelTask 註解自動建立任務並於結束後標記完成")
	void shouldTrackTaskSuccessfullyOnMethodExecution() {
		// When
		byte[] result = proxy.exportSuccess("report.xlsx", 500L);

		// Then
		assertThat(result).containsExactly(1, 2, 3);
		verify(excelTaskRepositoryPort).createTask(any(CreateExcelTaskCommand.class));
		verify(excelTaskRepositoryPort).markProcessing(anyString());
		verify(excelTaskRepositoryPort).markCompleted(anyString(), anyString(), eq(500L));
		assertThat(ExcelTaskContextHolder.isActive()).isFalse();
	}

	/**
	 * Feature: 宣告式任務異常兜底
	 * Scenario: 當標註 @TrackExcelTask 方法拋出例外時自動記錄 markFailed 並清除 Context
	 * Given: 調用 exportFailure("error.xlsx") 必定拋出 IllegalStateException
	 * When: 執行調用
	 * Then: 捕獲例外，驗證 markFailed("磁碟空間已滿") 且 Context 成功清除
	 */
	@Test
	@DisplayName("場景：方法拋出例外時切面應自動捕獲並標記任務 FAILED")
	void shouldMarkTaskFailedWhenMethodThrowsException() {
		// When & Then
		assertThatThrownBy(() -> proxy.exportFailure("error.xlsx"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("磁碟空間已滿");

		verify(excelTaskRepositoryPort).createTask(any(CreateExcelTaskCommand.class));
		verify(excelTaskRepositoryPort).markProcessing(anyString());
		verify(excelTaskRepositoryPort).markFailed(anyString(), eq("磁碟空間已滿"));
		assertThat(ExcelTaskContextHolder.isActive()).isFalse();
	}

	/**
	 * Feature: 宣告式非同步任務排程
	 * Scenario: 調用 async = true 方法時立即返回受理任務結果並於背景完成
	 * Given: 調用 exportAsync("async.xlsx", 200L)
	 * When: 執行調用
	 * Then: 立即回傳 PENDING 狀態之 ExcelTaskGottenResult，背景執行緒標記 COMPLETED
	 */
	@Test
	@DisplayName("場景：非同步 @TrackExcelTask 方法應立即返回受理任務結果")
	void shouldReturnInitialTaskOnAsyncAnnotation() {
		// When
		ExcelTaskGottenResult result = proxy.exportAsync("async.xlsx", 200L);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.taskStatus()).isEqualTo(ExcelTaskStatus.PENDING);
		assertThat(result.fileName()).isEqualTo("async.xlsx");

		verify(excelTaskRepositoryPort).createTask(any(CreateExcelTaskCommand.class));
		verify(excelTaskRepositoryPort).markProcessing(eq(result.taskId()));
		verify(excelTaskRepositoryPort).markCompleted(eq(result.taskId()), anyString(), eq(200L));
		assertThat(ExcelTaskContextHolder.isActive()).isFalse();
	}

	/**
	 * Feature: 上下文既有任務防重複建檔 (Avoid Duplicate Task Creation)
	 * Scenario: 當外層非同步長任務或 UseCase 已建立作用中上下文時，切面直接沿用現有任務，不建立重複任務
	 * Given: 執行緒上下文已綁定 "TASK-EXTERNAL-001"
	 * When: 調用標註 @TrackExcelTask 的方法
	 * Then: excelTaskRepositoryPort.createTask 不應被重複呼叫，且原有任務上下文不受干擾
	 */
	@Test
	@DisplayName("場景：當前上下文已有作用中任務時，切面直接沿用不重複建立新任務")
	void shouldNotCreateDuplicateTaskWhenContextAlreadyActive() {
		// Given: 外層非同步長任務已綁定上下文
		ExcelTaskContextHolder.bind("TASK-EXTERNAL-001", ExcelTaskType.EXPORT, "outer.xlsx", 100L, excelTaskRepositoryPort);

		try {
			// When: 調用標註 @TrackExcelTask 之方法
			byte[] result = proxy.exportSuccess("inner.xlsx", 50L);

			// Then: 業務正常執行，但未重複建檔
			assertThat(result).isNotEmpty();
			verify(excelTaskRepositoryPort, never()).createTask(any(CreateExcelTaskCommand.class));
			assertThat(ExcelTaskContextHolder.isActive()).isTrue();
			assertThat(ExcelTaskContextHolder.getTaskId()).isEqualTo("TASK-EXTERNAL-001");
		} finally {
			ExcelTaskContextHolder.unbind();
		}
	}

	/**
	 * Feature: FastExcelWriterAdapter 串流寫入之宣告式長任務追蹤
	 * Scenario: 透過代理調用 FastExcelWriterAdapter.writeStream，驗證任務自動建檔、SpEL 檔名解析、進度同步與完成標記
	 * Given: 透過 AspectJProxyFactory 包裝 FastExcelWriterAdapter 並組裝串流資料
	 * When: 呼叫 writeStream 執行串流匯出
	 * Then: 成功生成 Excel 二進位內容，且 excelTaskRepositoryPort 依序執行建檔、處理中、進度更新與完成標記
	 */
	@Test
	@DisplayName("場景：FastExcelWriterAdapter.writeStream 透過 @TrackExcelTask 宣告式追蹤串流長任務")
	void shouldTrackFastExcelWriterAdapterWriteStream() {
		// Given: 建立 FastExcelWriterAdapter 代理
		ExcelTaskTrackingAspect aspect = new ExcelTaskTrackingAspect(excelTaskRepositoryPort, Runnable::run);
		FastExcelWriterAdapter targetAdapter = new FastExcelWriterAdapter();
		AspectJProxyFactory factory = new AspectJProxyFactory(targetAdapter);
		factory.addAspect(aspect);
		ExcelWriterPort proxyWriter = factory.getProxy();

		WriteStreamSheetCommand sheet = WriteStreamSheetCommand.builder()
				.sheetName("長任務數據")
				.headers(List.of("ID", "名稱"))
				.rowIterable(List.of(
						List.of(1, "Item1"),
						List.of(2, "Item2")
				))
				.flushInterval(1)
				.build();

		WriteStreamExcelCommand command = WriteStreamExcelCommand.builder()
				.applicationName("BigDataReport")
				.addSheet(sheet)
				.build();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// When: 執行串流匯出長任務
		proxyWriter.writeStream(baos, command);

		// Then: 驗證產出與任務追蹤生命週期
		assertThat(baos.toByteArray()).isNotEmpty();
		verify(excelTaskRepositoryPort).createTask(any(CreateExcelTaskCommand.class));
		verify(excelTaskRepositoryPort).markProcessing(anyString());
		verify(excelTaskRepositoryPort, atLeast(2)).updateProgress(anyString(), anyLong(), anyLong());
		verify(excelTaskRepositoryPort).markCompleted(anyString(), anyString(), anyLong());
		assertThat(ExcelTaskContextHolder.isActive()).isFalse();
	}

	/**
	 * Feature: PoiSxssfWriterAdapter 串流寫入之宣告式長任務追蹤
	 * Scenario: 透過代理調用 PoiSxssfWriterAdapter.writeStream，驗證任務自動建檔、SpEL 檔名解析、硬碟沖刷進度同步與完成標記
	 * Given: 透過 AspectJProxyFactory 包裝 PoiSxssfWriterAdapter 並組裝串流資料
	 * When: 呼叫 writeStream 執行串流匯出
	 * Then: 成功生成 Excel 二進位內容，且 excelTaskRepositoryPort 依序執行建檔、處理中、進度更新與完成標記
	 */
	@Test
	@DisplayName("場景：PoiSxssfWriterAdapter.writeStream 透過 @TrackExcelTask 宣告式追蹤串流長任務")
	void shouldTrackPoiSxssfWriterAdapterWriteStream() {
		// Given: 建立 PoiSxssfWriterAdapter 代理
		ExcelTaskTrackingAspect aspect = new ExcelTaskTrackingAspect(excelTaskRepositoryPort, Runnable::run);
		PoiSxssfWriterAdapter targetAdapter = new PoiSxssfWriterAdapter();
		AspectJProxyFactory factory = new AspectJProxyFactory(targetAdapter);
		factory.addAspect(aspect);
		ExcelWriterPort proxyWriter = factory.getProxy();

		WriteStreamSheetCommand sheet = WriteStreamSheetCommand.builder()
				.sheetName("POI長任務數據")
				.headers(List.of("ID", "名稱"))
				.rowIterable(List.of(
						List.of(1, "Item1"),
						List.of(2, "Item2")
				))
				.flushInterval(1)
				.build();

		WriteStreamExcelCommand command = WriteStreamExcelCommand.builder()
				.applicationName("PoiBigDataReport")
				.addSheet(sheet)
				.build();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// When: 執行串流匯出長任務
		proxyWriter.writeStream(baos, command);

		// Then: 驗證產出與任務追蹤生命週期
		assertThat(baos.toByteArray()).isNotEmpty();
		verify(excelTaskRepositoryPort).createTask(any(CreateExcelTaskCommand.class));
		verify(excelTaskRepositoryPort).markProcessing(anyString());
		verify(excelTaskRepositoryPort, atLeast(2)).updateProgress(anyString(), anyLong(), anyLong());
		verify(excelTaskRepositoryPort).markCompleted(anyString(), anyString(), anyLong());
		assertThat(ExcelTaskContextHolder.isActive()).isFalse();
	}
}

