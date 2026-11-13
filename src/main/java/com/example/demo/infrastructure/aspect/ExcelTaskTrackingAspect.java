package com.example.demo.infrastructure.aspect;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import com.example.demo.application.annotation.task.TrackExcelTask;
import com.example.demo.application.port.out.task.ExcelTaskRepositoryPort;
import com.example.demo.application.port.out.task.ExcelTaskStoragePort;
import com.example.demo.application.shared.command.CreateExcelTaskCommand;
import com.example.demo.application.shared.context.ExcelTaskContextHolder;
import com.example.demo.application.shared.dto.ExcelTaskGottenResult;
import com.example.demo.application.shared.vo.ExcelTaskType;

/**
 * Excel 長任務宣告式追蹤切面 (Excel Task Declarative Tracking Aspect)
 * <p>
 * <b>【架構定位與職責說明】</b><br>
 * 本切面位於 Clean Architecture 的 <b>基礎設施層 (Infrastructure Layer - Cross-Cutting Technical Concerns)</b>，
 * 遵循六角形架構 (Hexagonal Architecture) 規範，作為跨切面的技術驅動機制。
 * 本切面專門攔截標註有 {@link TrackExcelTask} 之方法（如各類 Excel 讀寫 Outbound Adapters 或 Service 業務節點），
 * 將繁瑣的非同步/同步任務管理、進度回報與例外追蹤從核心業務邏輯中徹底解耦。
 * </p>
 * <p>
 * <b>【核心功能與生命週期管理】</b>
 * <ol>
 *   <li><b>防重入與嵌套相容 (Reentrancy & Nesting Protection)</b>：
 *       檢查當前執行緒上下文 {@link ExcelTaskContextHolder}，若外層呼叫已建立任務，則自動沿用現有上下文，避免重複建立重複紀錄。</li>
 *   <li><b>動態 SpEL 參數解析 (Dynamic SpEL Evaluation)</b>：
 *       支援透過 Spring Expression Language (SpEL) 動態抽取方法入參中的檔案名稱 (fileName) 與預估總筆數 (totalRows)，例如 {@code "#command.fileName"}。</li>
 *   <li><b>任務持久化與獨立事務 (Task Persistence)</b>：
 *       調用輸出埠 {@link ExcelTaskRepositoryPort} 建立唯一 UUID 任務記錄，並即時轉換任務狀態為 {@code PENDING} 與 {@code PROCESSING}。</li>
 *   <li><b>執行緒上下文綁定 (ThreadLocal Binding)</b>：
 *       將當前任務資訊綁定至 {@link ExcelTaskContextHolder}，供底層串流引擎（如 FastExcel / POI SXSSF）在沖刷 (flush) 時累加已處理列數。</li>
 *   <li><b>同步 / 非同步雙模分流 (Sync & Async Mode Dispatching)</b>：
 *       依據註解宣告之 {@code async} 屬性，決定是由當前執行緒即時串流輸出，或是委派至背景專屬執行緒池 (ThreadPoolTaskExecutor) 執行。</li>
 *   <li><b>閉環與例外安全清理 (Safe Completion & Cleanup)</b>：
 *       於方法執行完畢後標記為 {@code COMPLETED} 並補齊產物下載 URL；若遭遇異常則標記為 {@code FAILED}，最後於 {@code finally} 區塊安全釋放 {@code ThreadLocal} 資源以防止內存洩漏。</li>
 * </ol>
 * </p>
 *
 * @see TrackExcelTask
 * @see ExcelTaskRepositoryPort
 * @see ExcelTaskContextHolder
 */
@Aspect
@Component
public class ExcelTaskTrackingAspect {

	/**
	 * 長任務持久化輸出埠 (Outbound Port)，負責與資料庫儲存庫進行狀態更新與查詢協調
	 */
	private final ExcelTaskRepositoryPort excelTaskRepositoryPort;

	/**
	 * 長任務產出檔案本機磁碟暫存輸出埠 (Outbound Port)，負責檔案落盤暫存與串流分流
	 */
	private final ExcelTaskStoragePort excelTaskStoragePort;

	/**
	 * 長任務背景專屬執行緒池，用於執行標註為 {@code async = true} 之耗時匯入/匯出任務
	 */
	private final Executor taskExecutor;

	/**
	 * Spring SpEL 表達式解析器，負責動態求值註解宣告之字串表達式
	 */
	private final ExpressionParser expressionParser = new SpelExpressionParser();

	/**
	 * 方法參數名稱探測器，用於在 SpEL 評估上下文中注入方法形參的真實名稱（如 #query, #command）
	 */
	private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	/**
	 * 建構子依賴注入
	 *
	 * @param excelTaskRepositoryPort 長任務資料庫輸出埠，不可為空
	 * @param excelTaskStoragePort    長任務產出檔案儲存庫輸出埠，不可為空
	 * @param taskExecutor            Excel 專用非同步執行緒池 (Bean 名稱: {@code excelTaskExecutor})，不可為空
	 * @throws NullPointerException 若任何依賴組件為 {@code null}
	 */
	public ExcelTaskTrackingAspect(
			ExcelTaskRepositoryPort excelTaskRepositoryPort,
			ExcelTaskStoragePort excelTaskStoragePort,
			@Qualifier("excelTaskExecutor") Executor taskExecutor
	) {
		this.excelTaskRepositoryPort = Objects.requireNonNull(excelTaskRepositoryPort, "excelTaskRepositoryPort 不得為空");
		this.excelTaskStoragePort = Objects.requireNonNull(excelTaskStoragePort, "excelTaskStoragePort 不得為空");
		this.taskExecutor = Objects.requireNonNull(taskExecutor, "taskExecutor 不得為空");
	}

	/**
	 * 環繞通知 (Around Advice)：攔截所有標註 {@link TrackExcelTask} 之方法執行點
	 * <p>
	 * 負責解析 SpEL 表達式、持久化任務初始記錄、判斷同步/非同步分支，並全權託管任務的執行生命週期。
	 * </p>
	 *
	 * @param joinPoint      AOP 切入點 (Joint Point)，包含目標方法資訊與調用參數
	 * @param trackExcelTask 放置於目標方法上的宣告式任務追蹤註解
	 * @return 目標方法執行結果；若為非同步模式且方法回傳型別為 {@link ExcelTaskGottenResult}，則立即回傳受理初值
	 * @throws Throwable 執行過程中所拋出之任何業務或系統異常
	 */
	@Around("@annotation(trackExcelTask)")
	public Object trackTask(ProceedingJoinPoint joinPoint, TrackExcelTask trackExcelTask) throws Throwable {
		// 1. 防重入檢查：若當前執行緒上下文已有作用中的 Excel 任務 (例如外層 UseCase 或非同步排程已建立任務)，
		// 則直接沿用現有任務上下文進行串流進度追蹤，避免重複建立任務實體與覆蓋上下文
		if (ExcelTaskContextHolder.isActive()) {
			return joinPoint.proceed();
		}

		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		Object[] args = joinPoint.getArgs();

		// 2. 動態解析 SpEL 參數 (解析檔案名稱與預估總列數)
		EvaluationContext context = buildEvaluationContext(method, args);
		String resolvedFileName = resolveSpel(trackExcelTask.fileName(), context, "excel_task.xlsx");
		long resolvedTotalRows = resolveLongSpel(trackExcelTask.totalRows(), context, 0L);

		// 3. 建立任務初始實體 (產生 UUID，寫入 t_excel_task 並賦予 PENDING 狀態)
		String taskId = UUID.randomUUID().toString();
		CreateExcelTaskCommand createCommand = new CreateExcelTaskCommand(
				taskId,
				trackExcelTask.type(),
				resolvedFileName,
				resolvedTotalRows
		);
		ExcelTaskGottenResult initialTask = excelTaskRepositoryPort.createTask(createCommand);

		// 4. 判斷是否啟用非同步背景執行分流
		if (trackExcelTask.async()) {
			return handleAsyncTask(joinPoint, signature, taskId, trackExcelTask.type(), resolvedFileName, resolvedTotalRows, initialTask);
		} else {
			return handleSyncTask(joinPoint, taskId, trackExcelTask.type(), resolvedFileName, resolvedTotalRows);
		}
	}

	/**
	 * 處理同步長任務生命週期 (Synchronous Task Execution)
	 * <p>
	 * 適用於大數據 HTTP Response 串流即時下載等場景。任務狀態流轉為：
	 * {@code PENDING} -> {@code PROCESSING} -> (目標方法執行串流輸出) -> {@code COMPLETED}。
	 * </p>
	 *
	 * @param joinPoint 切入點
	 * @param taskId    任務唯一識別碼 (UUID)
	 * @param taskType  任務類型 (如 STREAM_EXPORT, ASYNC_EXPORT, ASYNC_IMPORT)
	 * @param fileName  產物或來源檔案名稱
	 * @param totalRows 預估總列數
	 * @return 目標方法執行回傳值
	 * @throws Throwable 若執行中發生異常，更新資料庫狀態為 {@code FAILED} 後重新拋出
	 */
	private Object handleSyncTask(
			ProceedingJoinPoint joinPoint,
			String taskId,
			ExcelTaskType taskType,
			String fileName,
			long totalRows
	) throws Throwable {
		// 將任務狀態更新為進行中 (PROCESSING)
		excelTaskRepositoryPort.markProcessing(taskId);
		// 將任務上下文綁定至當前執行緒 ThreadLocal，使底層 Adapter 可無感呼叫 incrementProgress
		ExcelTaskContextHolder.bind(taskId, taskType, fileName, totalRows, excelTaskRepositoryPort);
		try {
			Object[] args = joinPoint.getArgs();
			if (args != null && args.length > 0 && args[0] instanceof OutputStream originalOut) {
				OutputStream teeOut = excelTaskStoragePort.wrapOutputStream(taskId, fileName, originalOut);
				Object[] modifiedArgs = args.clone();
				modifiedArgs[0] = teeOut;
				try {
					Object result = joinPoint.proceed(modifiedArgs);
					return processTaskCompletion(taskId, totalRows, fileName, result);
				} finally {
					excelTaskStoragePort.finishStream(taskId);
				}
			} else {
				Object result = joinPoint.proceed();
				return processTaskCompletion(taskId, totalRows, fileName, result);
			}
		} catch (Throwable t) {
			// 發生未預期異常，記錄失敗原因並標記為 FAILED
			excelTaskRepositoryPort.markFailed(taskId, t.getMessage());
			throw t;
		} finally {
			// 嚴格釋放當前執行緒 ThreadLocal 資源，防止執行緒池復用造成上下文污染或內存洩漏
			ExcelTaskContextHolder.unbind();
		}
	}

	/**
	 * 處理非同步長任務生命週期 (Asynchronous Task Execution)
	 * <p>
	 * 將耗時任務打包為 Runnable 委派至背景專屬執行緒池 {@link #taskExecutor} 執行。
	 * 當前呼叫端執行緒立即解脫返回，實現無阻塞非同步調度。
	 * </p>
	 *
	 * @param joinPoint   切入點
	 * @param signature   方法簽章
	 * @param taskId      任務唯一識別碼 (UUID)
	 * @param taskType    任務類型
	 * @param fileName    產物或來源檔案名稱
	 * @param totalRows   預估總列數
	 * @param initialTask 初始建立之長任務 DTO 資訊
	 * @return 若方法回傳型別相容於 {@link ExcelTaskGottenResult}，則立即回傳初值；否則回傳 {@code null}
	 */
	private Object handleAsyncTask(
			ProceedingJoinPoint joinPoint,
			MethodSignature signature,
			String taskId,
			ExcelTaskType taskType,
			String fileName,
			long totalRows,
			ExcelTaskGottenResult initialTask
	) {
		// 委派至背景執行緒池異步執行
		taskExecutor.execute(() -> {
			// 在背景工作執行緒中標記 PROCESSING 並綁定 ThreadLocal 上下文
			excelTaskRepositoryPort.markProcessing(taskId);
			ExcelTaskContextHolder.bind(taskId, taskType, fileName, totalRows, excelTaskRepositoryPort);
			try {
				Object[] args = joinPoint.getArgs();
				if (args != null && args.length > 0 && args[0] instanceof OutputStream originalOut) {
					OutputStream teeOut = excelTaskStoragePort.wrapOutputStream(taskId, fileName, originalOut);
					Object[] modifiedArgs = args.clone();
					modifiedArgs[0] = teeOut;
					try {
						Object result = joinPoint.proceed(modifiedArgs);
						processTaskCompletion(taskId, totalRows, fileName, result);
					} finally {
						excelTaskStoragePort.finishStream(taskId);
					}
				} else {
					Object result = joinPoint.proceed();
					processTaskCompletion(taskId, totalRows, fileName, result);
				}
			} catch (Throwable t) {
				// 背景任務拋出異常，記錄錯誤訊息至資料庫
				excelTaskRepositoryPort.markFailed(taskId, t.getMessage());
			} finally {
				// 背景執行緒結束時嚴格清理 ThreadLocal
				ExcelTaskContextHolder.unbind();
			}
		});

		// 若方法宣告回傳 ExcelTaskGottenResult，立即回傳初值 (包含 taskId)，供呼叫端/前端立即開始輪詢進度
		if (ExcelTaskGottenResult.class.isAssignableFrom(signature.getReturnType())) {
			return initialTask;
		}
		return null;
	}

	/**
	 * 結算長任務產物檔案暫存與資料庫完成狀態
	 *
	 * @param taskId    任務識別碼
	 * @param totalRows 預估或設定之總列數
	 * @param fileName  產出檔案名稱
	 * @param result    原方法回傳結果 (若為 byte[] 則持久化儲存)
	 * @return 原方法執行回傳值
	 */
	private Object processTaskCompletion(String taskId, long totalRows, String fileName, Object result) {
		if (result instanceof byte[] bytes) {
			excelTaskStoragePort.store(taskId, fileName, bytes);
		}
		// 結算最終處理列數與完成狀態
		long finalProcessed = ExcelTaskContextHolder.getProcessedRows();
		long finalTotal = totalRows > 0 ? totalRows : finalProcessed;
		String fileUrl = "/api/v1/excel/tasks/" + taskId + "/download";
		excelTaskRepositoryPort.markCompleted(taskId, fileUrl, finalTotal);
		return result;
	}

	/**
	 * 建構 Spring SpEL 評估上下文 (EvaluationContext)
	 * <p>
	 * 註冊三種變數存取機制，確保在註解表達式中能靈活讀取入參：
	 * <ul>
	 *   <li><b>真實形參名稱</b>：如 {@code #query}, {@code #command}, {@code #resource}。</li>
	 *   <li><b>位置引數別名</b>：如 {@code #a0}, {@code #p0}, {@code #a1}, {@code #p1}。</li>
	 *   <li><b>類別駝峰名稱</b>：如參數型別為 {@code ReadExcelQuery} 則自動註冊為 {@code #readExcelQuery}。</li>
	 * </ul>
	 * </p>
	 *
	 * @param method 被攔截之目標反射方法
	 * @param args   傳入該方法之實體參數陣列
	 * @return 已注入參數變數之 {@link EvaluationContext}
	 */
	private EvaluationContext buildEvaluationContext(Method method, Object[] args) {
		StandardEvaluationContext context = new StandardEvaluationContext();
		String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
		if (paramNames != null && args != null) {
			for (int i = 0; i < paramNames.length && i < args.length; i++) {
				context.setVariable(paramNames[i], args[i]);
			}
		}
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				context.setVariable("a" + i, args[i]);
				context.setVariable("p" + i, args[i]);
				if (args[i] != null) {
					String simpleName = args[i].getClass().getSimpleName();
					String varName = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
					context.setVariable(varName, args[i]);
				}
			}
		}
		return context;
	}

	/**
	 * 安全解析 SpEL 字串表達式
	 * <p>
	 * 若表達式為空或求值失敗（如存取無效屬性），則自動平滑降級回退至預設值，保證系統不因表達式語法錯誤而中斷。
	 * </p>
	 *
	 * @param expressionStr 欲解析之 SpEL 表達式字串
	 * @param context       變數評估上下文
	 * @param defaultVal    解析失敗或結果為空時的回退預設值
	 * @return 解析求值後的字串結果，或指定之預設值
	 */
	private String resolveSpel(String expressionStr, EvaluationContext context, String defaultVal) {
		if (expressionStr == null || expressionStr.isBlank()) {
			return defaultVal;
		}
		try {
			Expression expression = expressionParser.parseExpression(expressionStr);
			Object value = expression.getValue(context);
			return value != null ? value.toString() : defaultVal;
		} catch (Exception e) {
			return defaultVal;
		}
	}

	/**
	 * 安全解析 SpEL 長整數值數值表達式 (用於預估總筆數 totalRows)
	 * <p>
	 * 支援從 {@link Number} 物件直接取得 {@code longValue()}，或安全轉換數值字串。
	 * 若解析失敗則平滑降級為指定預設值。
	 * </p>
	 *
	 * @param expressionStr 欲解析之數值 SpEL 表達式字串
	 * @param context       變數評估上下文
	 * @param defaultVal    解析失敗時的回退預設數值
	 * @return 解析求值後的長整數數值，或指定之預設值
	 */
	private long resolveLongSpel(String expressionStr, EvaluationContext context, long defaultVal) {
		if (expressionStr == null || expressionStr.isBlank()) {
			return defaultVal;
		}
		try {
			Expression expression = expressionParser.parseExpression(expressionStr);
			Object value = expression.getValue(context);
			if (value instanceof Number num) {
				return num.longValue();
			}
			return Long.parseLong(String.valueOf(value));
		} catch (Exception e) {
			return defaultVal;
		}
	}
}


