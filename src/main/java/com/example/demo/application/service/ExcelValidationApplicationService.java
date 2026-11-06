package com.example.demo.application.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.example.demo.application.assembler.ExcelBeanAssembler;
import com.example.demo.application.port.in.ValidateExcelUseCase;
import com.example.demo.application.port.out.ExcelErrorMarkerPort;
import com.example.demo.application.port.out.ExcelReaderPort;
import com.example.demo.application.port.out.ExcelRowValidatorPort;
import com.example.demo.application.port.out.ExcelSheetValidatorPort;
import com.example.demo.application.shared.dto.ExcelGottenResult;
import com.example.demo.application.shared.dto.ExcelValidationGottenResult;
import com.example.demo.application.shared.model.ExcelRowError;
import com.example.demo.application.shared.model.ExcelValidationContext;

/**
 * Excel 宣告式資料檢核應用層服務 (Excel Validation Application Service - Inbound Adapter)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 應用層 ({@code application/service/})，
 * 遵循 {@code AGENTS.md} 規則 25 與 3-9 (以 {@code ApplicationService} 結尾)。<br>
 * 實作 {@link ValidateExcelUseCase} 業務契約，擔任資料檢核流程中樞調度器：
 * 協調 {@link ExcelReaderPort} 進行二進位串流解析、透過 {@link ExcelBeanAssembler} 反序列化為強型別模型、
 * 依序調度行級驗證器 {@link ExcelRowValidatorPort} (含 EasyRules) 與整表約束驗證器 {@link ExcelSheetValidatorPort}，
 * 全量累積錯誤事實，並在需要時透過 {@link ExcelErrorMarkerPort} 產製視覺化錯誤標註回標檔。
 * </p>
 */
@Service
public class ExcelValidationApplicationService implements ValidateExcelUseCase {

	private final ExcelReaderPort excelReaderPort;
	private final List<ExcelRowValidatorPort<?>> rowValidators;
	private final List<ExcelSheetValidatorPort<?>> sheetValidators;
	private final ExcelErrorMarkerPort errorMarkerPort;

	/**
	 * 建構子注入所有依賴之 Outbound Ports
	 *
	 * @param excelReaderPort Excel 讀取輸出埠
	 * @param rowValidators   所有行級客製驗證器 SPI 清單 (可選)
	 * @param sheetValidators 所有整表約束驗證器 SPI 清單 (可選)
	 * @param errorMarkerPort 錯誤標註回標檔產製輸出埠
	 */
	public ExcelValidationApplicationService(
			ExcelReaderPort excelReaderPort,
			List<ExcelRowValidatorPort<?>> rowValidators,
			List<ExcelSheetValidatorPort<?>> sheetValidators,
			ExcelErrorMarkerPort errorMarkerPort) {
		this.excelReaderPort = Objects.requireNonNull(excelReaderPort, "excelReaderPort must not be null");
		this.rowValidators = rowValidators != null ? rowValidators : Collections.emptyList();
		this.sheetValidators = sheetValidators != null ? sheetValidators : Collections.emptyList();
		this.errorMarkerPort = Objects.requireNonNull(errorMarkerPort, "errorMarkerPort must not be null");
	}

	@Override
	public <T> ExcelValidationGottenResult<T> validate(InputStream excelStream, Class<T> modelClass) {
		Objects.requireNonNull(excelStream, "excelStream must not be null");
		Objects.requireNonNull(modelClass, "modelClass must not be null");

		// 1. 透過 ExcelReaderPort 讀取試算表二進位資料
		ExcelGottenResult gottenResult = excelReaderPort.read(excelStream);
		var sheetOpt = gottenResult.getFirstSheet();
		if (sheetOpt.isEmpty() || sheetOpt.get().isEmpty()) {
			return ExcelValidationGottenResult.success(Collections.emptyList());
		}

		var sheetResult = sheetOpt.get();
		String sheetName = sheetResult.sheetName() != null ? sheetResult.sheetName() : "Sheet1";

		// 2. 透過 ExcelBeanAssembler 進行容錯組裝，並收集儲存格型別轉換錯誤事實
		ExcelBeanAssembler.BeanAssemblyResult<T> assemblyResult = ExcelBeanAssembler.toBeanListWithErrors(sheetResult, modelClass);

		// 3. 針對轉換成功之資料模型執行 EasyRules 與自訂業務校驗
		ExcelValidationGottenResult<T> ruleValidationResult = validateRows(sheetName, assemblyResult.items(), modelClass);

		// 4. 合併儲存格型別轉換錯誤與業務規則檢核錯誤
		List<ExcelRowError> combinedErrors = new ArrayList<>(assemblyResult.conversionErrors());
		combinedErrors.addAll(ruleValidationResult.errors());

		// 5. 總結算合格資料列與統計資訊
		return new ExcelValidationGottenResult<>(
				ruleValidationResult.validItems(),
				combinedErrors,
				sheetResult.rows().size()
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> ExcelValidationGottenResult<T> validateRows(String sheetName, List<T> rows, Class<T> modelClass) {
		Objects.requireNonNull(modelClass, "modelClass must not be null");
		if (rows == null || rows.isEmpty()) {
			return ExcelValidationGottenResult.success(Collections.emptyList());
		}

		// 建立檢核上下文 (整表快照)
		ExcelValidationContext<T> context = new ExcelValidationContext<>(sheetName, rows);
		List<ExcelRowError> allErrors = new ArrayList<>();
		List<T> validItems = new ArrayList<>();

		// 篩選支援該 modelClass 的行級驗證器
		List<ExcelRowValidatorPort<T>> matchedRowValidators = new ArrayList<>();
		for (ExcelRowValidatorPort<?> rv : rowValidators) {
			if (rv.supports(modelClass)) {
				matchedRowValidators.add((ExcelRowValidatorPort<T>) rv);
			}
		}

		// 篩選支援該 modelClass 的整表驗證器
		List<ExcelSheetValidatorPort<T>> matchedSheetValidators = new ArrayList<>();
		for (ExcelSheetValidatorPort<?> sv : sheetValidators) {
			if (sv.supports(modelClass)) {
				matchedSheetValidators.add((ExcelSheetValidatorPort<T>) sv);
			}
		}

		// 1. 執行行級業務驗證 (1-based rowIndex，考量表頭列，資料行通常自列號 2 開始)
		for (int i = 0; i < rows.size(); i++) {
			int excelRowIndex = i + 2; // 表頭為第 1 列，資料為第 2 列起
			T rowItem = rows.get(i);
			List<ExcelRowError> rowErrors = new ArrayList<>();

			for (ExcelRowValidatorPort<T> validator : matchedRowValidators) {
				List<ExcelRowError> errs = validator.validateRow(context, excelRowIndex, rowItem);
				if (errs != null && !errs.isEmpty()) {
					rowErrors.addAll(errs);
				}
			}

			if (rowErrors.isEmpty()) {
				validItems.add(rowItem);
			} else {
				allErrors.addAll(rowErrors);
			}
		}

		// 2. 執行整表級別約束驗證 (跨列唯一性防重複、加總平衡等)
		for (ExcelSheetValidatorPort<T> sheetValidator : matchedSheetValidators) {
			List<ExcelRowError> sheetErrors = sheetValidator.validateSheet(context);
			if (sheetErrors != null && !sheetErrors.isEmpty()) {
				allErrors.addAll(sheetErrors);
			}
		}

		return new ExcelValidationGottenResult<>(validItems, allErrors, rows.size());
	}

	@Override
	public <T> byte[] validateAndGenerateFeedback(InputStream excelStream, Class<T> modelClass) {
		Objects.requireNonNull(excelStream, "excelStream must not be null");
		Objects.requireNonNull(modelClass, "modelClass must not be null");

		try {
			// 快取輸入串流，支援兩次讀取 (第一次驗證，第二次產製回標檔)
			byte[] fileBytes = excelStream.readAllBytes();
			ExcelValidationGottenResult<T> result = validate(new ByteArrayInputStream(fileBytes), modelClass);

			// 若完全合格，回傳空 byte 陣列
			if (result.valid() || !result.hasErrors()) {
				return new byte[0];
			}

			// 若存在錯誤，透過 ExcelErrorMarkerPort 渲染生成錯誤標註回標檔
			return errorMarkerPort.generateErrorMarkedWorkbook(new ByteArrayInputStream(fileBytes), result.errors());
		} catch (Exception ex) {
			throw new RuntimeException("執行檢核並產製錯誤回標檔失敗: " + ex.getMessage(), ex);
		}
	}
}

