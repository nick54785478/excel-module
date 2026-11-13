package com.example.demo.infrastructure.adapter.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jeasy.rules.annotation.Rule;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.jeasy.rules.api.RulesEngineParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.example.demo.application.port.out.validation.ExcelRowValidatorPort;
import com.example.demo.application.shared.model.validation.ExcelRowError;
import com.example.demo.application.shared.context.ExcelValidationContext;

/**
 * 基於 EasyRules 之行級客製驗證適配器 (EasyRules Excel Validator Adapter - Outbound
 * Adapter)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 基礎設施層 ({@code infrastructure/adapter/validator/})，
 * 遵循 {@code AGENTS.md} 規則 30 與 3-9 (以 {@code Adapter} 結尾)。<br>
 * 實作應用層 {@link ExcelRowValidatorPort} SPI 契約，將外部輕量級規則引擎 EasyRules 封裝隔離在基礎設施內部。
 * 負責將 Excel 檢核列與上下文封裝為 EasyRules {@link Facts}，啟動 {@link RulesEngine} 進行業務規則推論，
 * 並配置為非短路模式 (Non-short-circuiting)，全量收集錯誤事實至 {@code List<ExcelRowError>}。
 * </p>
 *
 * @param <T> 資料模型型別
 */
@Component
public class EasyRulesExcelValidatorAdapter<T> implements ExcelRowValidatorPort<T> {

	private final Rules rules;
	private final RulesEngine rulesEngine;

	/**
	 * Spring 容器自動注入建構子：自動掃描並註冊所有標註有 {@link Rule} 之 Spring Beans
	 *
	 * @param applicationContext Spring 應用程式上下文
	 */
	@Autowired
	public EasyRulesExcelValidatorAdapter(ApplicationContext applicationContext) {
		this.rules = new Rules();
		if (applicationContext != null) {
			Map<String, Object> ruleBeans = applicationContext.getBeansWithAnnotation(Rule.class);
			ruleBeans.values().forEach(this.rules::register);
		}
		this.rulesEngine = createConfiguredEngine();
	}

	/**
	 * 用於測試/自訂場景建構子：手動傳入一組自訂規則清單
	 *
	 * @param customRules 自訂 EasyRules 規則物件清單
	 */
	public EasyRulesExcelValidatorAdapter(List<Object> customRules) {
		this.rules = new Rules();
		if (customRules != null) {
			customRules.forEach(this.rules::register);
		}
		this.rulesEngine = createConfiguredEngine();
	}

	/**
	 * 動態註冊單一自訂規則物件
	 *
	 * @param rule EasyRules 規則 POJO
	 */
	public void registerRule(Object rule) {
		if (rule != null) {
			this.rules.register(rule);
		}
	}

	@Override
	public List<ExcelRowError> validateRow(ExcelValidationContext<T> context, int rowIndex, T rowData) {
		if (rowData == null) {
			return Collections.emptyList();
		}

		List<ExcelRowError> accumulatedErrors = new ArrayList<>();

		// 封裝事實 (Facts)
		Facts facts = new Facts();
		facts.put("context", context);
		facts.put("rowIndex", rowIndex);
		facts.put("row", rowData);
		facts.put("rowData", rowData);
		facts.put("errors", accumulatedErrors);

		// 觸發規則引擎推論
		rulesEngine.fire(rules, facts);

		return accumulatedErrors;
	}

	private RulesEngine createConfiguredEngine() {
		// 配置非短路全量錯誤累積模式 (skipOnFirstFailedRule = false, skipOnFirstAppliedRule = false)
		RulesEngineParameters parameters = new RulesEngineParameters()
				.skipOnFirstFailedRule(false)
				.skipOnFirstAppliedRule(false)
				.skipOnFirstNonTriggeredRule(false);
		return new DefaultRulesEngine(parameters);
	}
}
