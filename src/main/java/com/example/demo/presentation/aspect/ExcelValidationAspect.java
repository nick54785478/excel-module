package com.example.demo.presentation.aspect;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.application.annotation.validation.ValidateExcel;
import com.example.demo.application.annotation.validation.ValidatedExcelData;
import com.example.demo.application.port.in.validation.ValidateExcelUseCase;
import com.example.demo.application.shared.context.ExcelValidationContextHolder;
import com.example.demo.application.shared.dto.ExcelValidationGottenResult;
import com.example.demo.application.shared.exception.ExcelValidationFailedException;

/**
 * Excel 宣告式資料檢核與上傳攔截切面 (Excel Validation Aspect)
 * <p>
 * <b>【架構定位與職責說明】</b><br>
 * 位於 Clean Architecture <b>表現層切面套件 ({@code presentation/aspect/})</b>，
 * 遵循六角形架構（Hexagonal Architecture）<b>Inbound Adapter（輸入適配器）</b>規範。<br>
 * 本切面作為 Web 請求前置防衛閘門（Inbound Guard），專門攔截標註有 {@link ValidateExcel} 之 Controller 匯入方法，
 * 協同 {@link com.example.demo.presentation.resolver.ValidatedExcelDataArgumentResolver}
 * 與 {@link com.example.demo.presentation.exception.GlobalExceptionHandler} 提供端到端的 Web 驗證支援：
 * <ol>
 *   <li><b>檔案物理層防呆 (Level 1)</b>：自動偵測並驗證 {@link MultipartFile} 或 {@link InputStream} 入參。</li>
 *   <li><b>自動回標短路 (Auto Feedback Short-Circuit)</b>：若資料違規且設定為 {@link ValidateExcel.OnErrorAction#AUTO_FEEDBACK}，
 *       直接生成淡紅底深紅字錯誤回標 Excel 二進位下載（{@link ResponseEntity}），阻斷 Controller 方法本體執行。</li>
 *   <li><b>例外轉譯 (Exception-Driven Mode)</b>：設定為 {@link ValidateExcel.OnErrorAction#THROW_EXCEPTION} 時，
 *       拋出 {@link ExcelValidationFailedException} 交由表現層全域例外處理器轉譯為 HTTP 422 JSON。</li>
 *   <li><b>乾淨資料自動注入 (Clean Data Injection)</b>：全數檢核通過後，自動將已反序列化之合規資料清單注入標註有
 *       {@link ValidatedExcelData} 之方法參數，並綁定至 {@link ExcelValidationContextHolder}。</li>
 * </ol>
 * </p>
 * @see ValidateExcel
 * @see ValidatedExcelData
 * @see ValidateExcelUseCase
 * @see ExcelValidationContextHolder
 */
@Aspect
@Component
public class ExcelValidationAspect {

	private static final String EXCEL_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private final ValidateExcelUseCase validateExcelUseCase;

	/**
	 * 建構子注入應用層 Inbound Port 業務案例
	 *
	 * @param validateExcelUseCase 檢核業務案例輸入埠
	 */
	public ExcelValidationAspect(ValidateExcelUseCase validateExcelUseCase) {
		this.validateExcelUseCase = Objects.requireNonNull(validateExcelUseCase,
				"validateExcelUseCase must not be null");
	}

	/**
	 * 環繞通知：攔截標註 {@link ValidateExcel} 之 Controller 方法
	 *
	 * @param joinPoint     切入點
	 * @param validateExcel 宣告式檢核註解
	 * @return 方法執行結果或錯誤回標下載二進位回應
	 * @throws Throwable 執行過程之例外
	 */
	@Around("@annotation(validateExcel)")
	public Object interceptAndValidate(ProceedingJoinPoint joinPoint, ValidateExcel validateExcel) throws Throwable {
		Object[] args = joinPoint.getArgs();

		// 1. 自動從方法入參中抽取 MultipartFile 或 InputStream
		MultipartFile multipartFile = null;
		InputStream inputStream = null;

		for (Object arg : args) {
			if (arg instanceof MultipartFile mf) {
				multipartFile = mf;
				break;
			} else if (arg instanceof InputStream is) {
				inputStream = is;
				break;
			}
		}

		// 若未傳遞檔案或檔案為空，直接阻斷回傳 HTTP 400 Bad Request
		if (multipartFile != null && multipartFile.isEmpty()) {
			return ResponseEntity.badRequest().body("上傳之 Excel 檔案不得為空");
		}
		if (multipartFile == null && inputStream == null) {
			// 若找不到任何檔案參數，放行交由原方法或 Spring Validation 處置
			return joinPoint.proceed();
		}

		// 2. 緩存檔案位元組陣列，確保可重複建立串流使用
		byte[] fileBytes = multipartFile != null ? multipartFile.getBytes() : inputStream.readAllBytes();
		Class<?> modelClass = validateExcel.model();

		try {
			// 3. 依據註解宣告策略執行攔截決策
			if (validateExcel.action() == ValidateExcel.OnErrorAction.AUTO_FEEDBACK) {
				// 調用 UseCase 產製錯誤回標 Excel
				byte[] feedbackBytes = validateExcelUseCase.validateAndGenerateFeedback(
						new ByteArrayInputStream(fileBytes),
						modelClass);

				// 若回傳二進位長度大於 0，代表存在違規 -> 【直接短路阻斷】，回傳標註紅底之錯誤回標檔下載
				if (feedbackBytes != null && feedbackBytes.length > 0) {
					return buildAttachmentResponse(feedbackBytes, validateExcel.feedbackFilename());
				}
			}

			// 4. 執行全量資料解析與校驗
			ExcelValidationGottenResult<?> result = validateExcelUseCase.validate(
					new ByteArrayInputStream(fileBytes),
					modelClass);

			// 若策略為 THROW_EXCEPTION 且存在錯誤，直接拋出例外交由 GlobalExceptionHandler 轉譯為 422 JSON
			if (validateExcel.action() == ValidateExcel.OnErrorAction.THROW_EXCEPTION && result.hasErrors()) {
				throw new ExcelValidationFailedException("Excel 資料檢核發現違規事實", result);
			}

			// 5. 檢核通過：將合格結果綁定至執行緒上下文
			if (validateExcel.bindValidData()) {
				ExcelValidationContextHolder.bind(result);
			}

			// 6. 智慧參數注入：將已驗證通過之乾淨資料清單注入標註 @ValidatedExcelData 之參數
			Object[] newArgs = injectValidatedDataIfPresent(joinPoint, args, result);
			return joinPoint.proceed(newArgs);

		} finally {
			// 7. 安全清理 ThreadLocal 上下文，防止線程池資源污染
			ExcelValidationContextHolder.clear();
		}
	}

	/**
	 * 尋找標註有 {@link ValidatedExcelData} 之參數並自動替換為合格資料清單或結果物件
	 */
	private Object[] injectValidatedDataIfPresent(ProceedingJoinPoint joinPoint, Object[] args,
			ExcelValidationGottenResult<?> result) {
		if (!(joinPoint.getSignature() instanceof MethodSignature signature)) {
			return args;
		}

		Method method = signature.getMethod();
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		Class<?>[] paramTypes = method.getParameterTypes();
		Object[] newArgs = Arrays.copyOf(args, args.length);
		boolean modified = false;

		for (int i = 0; i < paramTypes.length; i++) {
			boolean hasTargetAnnotation = false;
			for (Annotation ann : parameterAnnotations[i]) {
				if (ann instanceof ValidatedExcelData) {
					hasTargetAnnotation = true;
					break;
				}
			}

			if (hasTargetAnnotation || ExcelValidationGottenResult.class.isAssignableFrom(paramTypes[i])) {
				if (List.class.isAssignableFrom(paramTypes[i])) {
					newArgs[i] = result.validItems();
					modified = true;
				} else if (ExcelValidationGottenResult.class.isAssignableFrom(paramTypes[i])) {
					newArgs[i] = result;
					modified = true;
				}
			}
		}

		return modified ? newArgs : args;
	}

	/**
	 * 組裝帶有 RFC 5987 / RFC 6266 UTF-8 檔名之二進位檔案下載回應實體
	 */
	private ResponseEntity<byte[]> buildAttachmentResponse(byte[] fileBytes, String filename) {
		String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
				.contentType(MediaType.parseMediaType(EXCEL_MIME_TYPE))
				.contentLength(fileBytes.length)
				.body(fileBytes);
	}
}

