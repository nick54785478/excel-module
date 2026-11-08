package com.example.demo.presentation.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.application.port.in.ExportFinancialReportUseCase;
import com.example.demo.application.shared.command.ExportBomCostAnalysisCommand;
import com.example.demo.application.shared.command.ExportCustomReportCommand;
import com.example.demo.application.shared.command.ExportProcurementAnalysisCommand;
import com.example.demo.presentation.assembler.ExcelReportResourceAssembler;
import com.example.demo.presentation.resource.in.ExportBomCostAnalysisResource;
import com.example.demo.presentation.resource.in.ExportCustomReportResource;
import com.example.demo.presentation.resource.in.ExportProcurementAnalysisResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 企業級進階報表、動態公式與安全防護 REST 控制器 (Excel Advanced Report Presentation Controller)
 * <p>
 * 遵循 Clean Architecture 六角形架構表現層規範，作為外部 HTTP 請求進入點。
 * 專門用於「維度五：企業級報表樣式、公式與安全防護 (Template, Styles & Security)」功能，
 * 支援儲存格跨欄合併、原生動態計算公式 (SUM、四則運算)、工作表密碼防篡改保護與列印標準頁首頁尾機密標記。
 */
@Tag(name = "Excel 企業級報表、公式與安全防護介面", description = "提供儲存格合併、動態公式計算、工作表密碼防護與列印頁首頁尾機密標記之完整 REST API")
@Validated
@RestController
@RequestMapping("/api/v1/excel/reports")
public class ExcelReportController {

	private static final String EXCEL_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private final ExportFinancialReportUseCase exportFinancialReportUseCase;
	private final ExcelReportResourceAssembler resourceAssembler;

	public ExcelReportController(
			ExportFinancialReportUseCase exportFinancialReportUseCase,
			ExcelReportResourceAssembler resourceAssembler
	) {
		this.exportFinancialReportUseCase = Objects.requireNonNull(exportFinancialReportUseCase, "exportFinancialReportUseCase must not be null");
		this.resourceAssembler = Objects.requireNonNull(resourceAssembler, "resourceAssembler must not be null");
	}

	/**
	 * HTTP POST 實測：匯出物料採購損益分析月報 (由 Request 傳入業務資料清單)
	 * <p>
	 * 整合維度五全特性：
	 * <ol>
	 *   <li><b>自訂業務資料</b>：由 Request 傳入物料代碼、名稱、數量、單價與預估售價。</li>
	 *   <li><b>表身動態公式</b>：採購小計公式 {@code =C{row}*D{row}}、毛利率公式 {@code =(F{row}-D{row})/F{row}}。</li>
	 *   <li><b>合計列跨欄合併</b>：底部「合計彙總」跨 4 欄合併 (A 到 D 欄)。</li>
	 *   <li><b>動態加總公式</b>：合計欄位注入原生 {@code =SUM(E2:E{lastRow})}，微調數量時自動重算。</li>
	 *   <li><b>工作表密碼防護</b>：以指定密碼鎖定工作表，禁止隨意編輯公式儲存格。</li>
	 *   <li><b>列印合規標記</b>：頁首置放報表大標題，頁尾自動計算動態頁碼 ({@code &P / &N}) 與機密審計標籤。</li>
	 * </ol>
	 *
	 * @param resource 採購損益分析匯出請求資源 (包含報表標題、解鎖密碼、機密標記與物料明細清單)
	 * @return Excel (.xlsx) 二進位下載串流
	 */
	@Operation(
			summary = "【Request傳入資料】匯出物料採購損益分析月報 (包含自訂明細、合併合計、動態公式與密碼保護)",
			description = "透過 POST 請求將物料清單與報表設定傳入，後端自動生成動態小計公式、跨欄合併彙總列、SUM公式與安全防護"
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "成功產製並下載 Excel 報表"),
			@ApiResponse(responseCode = "400", description = "請求參數格式不合法"),
			@ApiResponse(responseCode = "500", description = "伺服器內部產生異常")
	})
	@PostMapping("/procurement-analysis")
	public ResponseEntity<byte[]> exportProcurementAnalysisReport(
			@RequestBody @Valid ExportProcurementAnalysisResource resource
	) {
		ExportProcurementAnalysisCommand command = resourceAssembler.toCommand(resource);
		byte[] excelBytes = exportFinancialReportUseCase.exportProcurementAnalysisReport(command);
		String fileName = (command != null && command.reportTitle() != null && !command.reportTitle().isBlank())
				? command.reportTitle().trim() + ".xlsx"
				: "procurement_analysis_report.xlsx";
		return createAttachmentResponse(excelBytes, fileName);
	}

	/**
	 * 一鍵瀏覽器 GET 實測：匯出物料採購損益分析月報 (使用預設擬真示範資料)
	 * <p>
	 * 便於前端或瀏覽器直接點擊網址快速驗證產出之 Excel 樣式與公式。
	 *
	 * @param reportTitle    自訂報表標題 (選填，預設 "物料採購損益分析月報")
	 * @param password       工作表解鎖密碼 (選填，預設 "AuditSecure2026")
	 * @param classification 機密等級 (選填，預設 "機密資料・內部流轉")
	 * @return Excel (.xlsx) 二進位下載串流
	 */
	@Operation(
			summary = "【一鍵實測】匯出物料採購損益分析月報 (使用系統擬真示範資料)",
			description = "支援直接於瀏覽器網址列或 Swagger 點擊 GET 下載，快速體驗多階表頭、公式、密碼保護與頁尾浮水印"
	)
	@GetMapping("/procurement-analysis")
	public ResponseEntity<byte[]> exportDefaultProcurementAnalysisReport(
			@Parameter(description = "自訂列印頁首報表標題", example = "物料採購損益分析月報")
			@RequestParam(required = false, defaultValue = "物料採購損益分析月報") String reportTitle,

			@Parameter(description = "工作表解鎖密碼 (鎖定公式防篡改)", example = "AuditSecure2026")
			@RequestParam(required = false, defaultValue = "AuditSecure2026") String password,

			@Parameter(description = "列印頁尾機密等級標籤", example = "機密資料・內部流轉")
			@RequestParam(required = false, defaultValue = "機密資料・內部流轉") String classification
	) {
		byte[] excelBytes = exportFinancialReportUseCase.exportProcurementAnalysisReport(reportTitle, password, classification);
		return createAttachmentResponse(excelBytes, "procurement_analysis_report.xlsx");
	}

	/**
	 * HTTP POST 實測：匯出產品結構配方與標準成本卷算分析報表 (結合 Bom 與 Material 聚合根)
	 * <p>
	 * 整合維度五全特性與 DDD 領域實體：
	 * <ol>
	 *   <li><b>領域聚合根聯動</b>：依據母件料號 (如 MAT-FG-001) 穿透 BOM 配方與物料主檔，自動載入各階子料件。</li>
	 *   <li><b>表身動態計算公式</b>：
	 *     <ul>
	 *       <li>需求用量公式：{@code =H{row}*(1+I{row})} (基準用量乘上損耗率)。</li>
	 *       <li>成本小計公式：{@code =J{row}*K{row}} (有效需求用量乘上標準單價)。</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>大跨度跨欄合併</b>：合計列自第 A 欄跨至第 K 欄 (col 0 到 10) 寬幅合併。</li>
	 *   <li><b>動態加總公式</b>：合計欄位注入原生 {@code =SUM(L2:L{lastRow})}，單價或用量調整時自動重新計算。</li>
	 *   <li><b>專屬商務配色</b>：套用深海軍藍 (#1B365D) 表頭與白字粗體，首列凍結 (Freeze Pane)。</li>
	 *   <li><b>工作表密碼防護</b>：以指定密碼鎖定工作表，禁止隨意修改成本審計公式。</li>
	 *   <li><b>列印合規標籤</b>：頁首置放報表大標題與時間戳記，頁尾動態計算頁碼 ({@code &P / &N}) 與機密等級。</li>
	 * </ol>
	 *
	 * @param resource BOM 成本分析匯出請求資源
	 * @return Excel (.xlsx) 二進位下載串流
	 */
	@Operation(
			summary = "【Request傳入/聚合根查詢】匯出產品結構與標準成本卷算分析報表 (自訂 Strategy: BOM_COST_ANALYSIS)",
			description = "結合 Bom 與 Material 聚合根，注入需求用量與成本小計動態公式、合計列A~K跨欄合併、SUM動態加總與密碼防護",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					description = "產品結構配方與標準成本分析請求主體 (包含8項完整商用無人機次組裝與原物料明細範例)",
					required = true,
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ExportBomCostAnalysisResource.class),
							examples = @ExampleObject(
									name = "商用四軸無人機完整8筆BOM結構範例",
									summary = "包含飛控主板、推進總成、雲台相機、動力電池、碳纖機架、GPS天線、鈦合金螺絲與防震提箱",
									value = """
									{
									  "parentMaterialCode": "MAT-FG-001",
									  "reportTitle": "產品結構配方與標準成本卷算分析表",
									  "password": "BomAuditSecure2026",
									  "classification": "極機密・核心配方與成本審計",
									  "items": [
									    {
									      "level": 1,
									      "itemSequence": 10,
									      "childMaterialCode": "MAT-SFG-001",
									      "childMaterialName": "飛控主板電路總成",
									      "specification": "STM32H743 Cortex-M7 雙備援",
									      "category": "半成品",
									      "unit": "套/組",
									      "baseQuantity": 1,
									      "scrapRate": 0.01,
									      "standardCost": 4800,
									      "positionCode": "FC-ASM",
									      "remark": "核心飛控模組"
									    },
									    {
									      "level": 1,
									      "itemSequence": 20,
									      "childMaterialCode": "MAT-SFG-002",
									      "childMaterialName": "2212 動力推進總成 (4組)",
									      "specification": "2212 920KV 碳纖快拆",
									      "category": "半成品",
									      "unit": "套/組",
									      "baseQuantity": 4,
									      "scrapRate": 0.00,
									      "standardCost": 680,
									      "positionCode": "PROP-1~4",
									      "remark": "動力推進系統"
									    },
									    {
									      "level": 1,
									      "itemSequence": 30,
									      "childMaterialCode": "MAT-SFG-003",
									      "childMaterialName": "4K 三軸機械雲台相機總成",
									      "specification": "SONY IMX586 4800萬像素",
									      "category": "半成品",
									      "unit": "套/組",
									      "baseQuantity": 1,
									      "scrapRate": 0.00,
									      "standardCost": 3600,
									      "positionCode": "CAM-GIMBAL",
									      "remark": "光學偵蒐負載"
									    },
									    {
									      "level": 1,
									      "itemSequence": 40,
									      "childMaterialCode": "MAT-SFG-004",
									      "childMaterialName": "4S 5000mAh 智慧動力鋰電池",
									      "specification": "21700 電芯 4S1P 100A BMS",
									      "category": "半成品",
									      "unit": "套/組",
									      "baseQuantity": 2,
									      "scrapRate": 0.00,
									      "standardCost": 1850,
									      "positionCode": "BAT-1,2",
									      "remark": "航電儲能系統"
									    },
									    {
									      "level": 1,
									      "itemSequence": 50,
									      "childMaterialCode": "MAT-RM-0076",
									      "childMaterialName": "碳纖維四軸機架本體",
									      "specification": "全碳纖 3K 2.0mm 輕量化",
									      "category": "原物料",
									      "unit": "件/個",
									      "baseQuantity": 1,
									      "scrapRate": 0.00,
									      "standardCost": 1250,
									      "positionCode": "FRAME-BODY",
									      "remark": "主結構碳纖套件"
									    },
									    {
									      "level": 1,
									      "itemSequence": 60,
									      "childMaterialCode": "MAT-RM-0077",
									      "childMaterialName": "GPS/北斗雙頻陶瓷導航天線",
									      "specification": "L1/L5 雙頻抗干擾 RTK",
									      "category": "原物料",
									      "unit": "件/個",
									      "baseQuantity": 1,
									      "scrapRate": 0.00,
									      "standardCost": 750,
									      "positionCode": "GPS-ANT",
									      "remark": "高精定位天線"
									    },
									    {
									      "level": 1,
									      "itemSequence": 70,
									      "childMaterialCode": "MAT-RM-0078",
									      "childMaterialName": "M3*8 航太鈦合金裝配螺絲",
									      "specification": "TC4 鈦合金沉頭內六角",
									      "category": "原物料",
									      "unit": "件/個",
									      "baseQuantity": 24,
									      "scrapRate": 0.05,
									      "standardCost": 12.50,
									      "positionCode": "SCREW-M3-8",
									      "remark": "裝配耗損 5%"
									    },
									    {
									      "level": 1,
									      "itemSequence": 80,
									      "childMaterialCode": "MAT-RM-0079",
									      "childMaterialName": "防震防水氣密防護提箱",
									      "specification": "IP67 輕量化 PP 雙鎖扣",
									      "category": "原物料",
									      "unit": "箱/盒",
									      "baseQuantity": 1,
									      "scrapRate": 0.00,
									      "standardCost": 890,
									      "positionCode": "BOX-CASE",
									      "remark": "防震包材配件"
									    }
									  ]
									}
									"""
							)
					)
			)
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "成功產製並下載 BOM 成本分析 Excel 報表"),
			@ApiResponse(responseCode = "400", description = "請求參數格式不合法"),
			@ApiResponse(responseCode = "500", description = "伺服器內部產生異常")
	})
	@PostMapping("/bom-cost-analysis")
	public ResponseEntity<byte[]> exportBomCostAnalysisReport(
			@RequestBody @Valid ExportBomCostAnalysisResource resource
	) {
		ExportBomCostAnalysisCommand command = resourceAssembler.toCommand(resource);
		byte[] excelBytes = exportFinancialReportUseCase.exportBomCostAnalysisReport(command);
		String fileName = (command != null && command.reportTitle() != null && !command.reportTitle().isBlank())
				? command.reportTitle().trim() + ".xlsx"
				: "bom_cost_analysis_report.xlsx";
		return createAttachmentResponse(excelBytes, fileName);
	}

	/**
	 * 一鍵瀏覽器 GET 實測：匯出產品結構配方與標準成本卷算分析報表 (直接帶入母件料號實測)
	 * <p>
	 * 便於前端或瀏覽器網址列直接 GET 下載驗證，預設帶入商用巡檢無人機 (MAT-FG-001) 及其子件配方。
	 *
	 * @param materialCode   母件物料料號 (選填，預設 "MAT-FG-001")
	 * @param reportTitle    自訂報表標題 (選填，預設 "產品結構配方與標準成本卷算分析表")
	 * @param password       工作表解鎖密碼 (選填，預設 "BomAuditSecure2026")
	 * @param classification 機密等級 (選填，預設 "極機密・核心配方與成本審計")
	 * @return Excel (.xlsx) 二進位下載串流
	 */
	@Operation(
			summary = "【一鍵實測】匯出產品結構配方與標準成本卷算分析報表 (支援瀏覽器直接 GET 下載)",
			description = "傳入母件料號 (如 MAT-FG-001)，後端自動穿透 BOM 與物料聚合根，產出具備多階公式、合計跨欄合併與安全鎖定之 Excel"
	)
	@GetMapping("/bom-cost-analysis")
	public ResponseEntity<byte[]> exportDefaultBomCostAnalysisReport(
			@Parameter(description = "母件物料料號", example = "MAT-FG-001")
			@RequestParam(required = false, defaultValue = "MAT-FG-001") String materialCode,

			@Parameter(description = "自訂列印頁首報表標題", example = "產品結構配方與標準成本卷算分析表")
			@RequestParam(required = false, defaultValue = "產品結構配方與標準成本卷算分析表") String reportTitle,

			@Parameter(description = "工作表解鎖密碼 (鎖定公式防篡改)", example = "BomAuditSecure2026")
			@RequestParam(required = false, defaultValue = "BomAuditSecure2026") String password,

			@Parameter(description = "列印頁尾機密等級標籤", example = "極機密・核心配方與成本審計")
			@RequestParam(required = false, defaultValue = "極機密・核心配方與成本審計") String classification
	) {
		byte[] excelBytes = exportFinancialReportUseCase.exportBomCostAnalysisReport(materialCode, reportTitle, password, classification);
		String fileName = "bom_cost_analysis_" + materialCode + ".xlsx";
		return createAttachmentResponse(excelBytes, fileName);
	}

	/**
	 * 一鍵瀏覽器 GET 實測：純宣告式 Model 註解驅動範本
	 * <p>
	 * 示範透過 {@code @ExcelSheet} 與 {@code @ExcelColumn(formula = "...")} 自動動態替換列號並啟用防護。
	 *
	 * @return Excel (.xlsx) 二進位下載串流
	 */
	@Operation(
			summary = "宣告式模型註解驅動範本匯出 (示範 @ExcelColumn 動態公式與 @ExcelSheet 防護)",
			description = "展示零程式碼侵入之宣告式動態公式替換與工作表密碼防護"
	)
	@GetMapping("/declarative-sample")
	public ResponseEntity<byte[]> exportDeclarativeSample() {
		byte[] excelBytes = exportFinancialReportUseCase.exportDeclarativeSample();
		return createAttachmentResponse(excelBytes, "declarative_procurement_report.xlsx");
	}

	/**
	 * HTTP POST 彈性客製化進階報表匯出
	 * <p>
	 * 允許前端傳送自訂之表頭、資料列、任意儲存格合併區間、任意座標之動態公式以及保護密碼。
	 *
	 * @param resource 客製化匯出請求資源
	 * @return Excel (.xlsx) 二進位下載串流
	 */
	@Operation(
			summary = "自訂進階樣式、任意合併區間、動態公式與安全防護報表匯出",
			description = "以 JSON 格式動態傳送表頭、二維資料、特定座標公式 (如 D10: SUM(D2:D9)) 與合併範圍 (firstRow, lastRow, firstCol, lastCol)"
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "成功產製並下載客製化報表"),
			@ApiResponse(responseCode = "400", description = "請求參數格式不合法"),
			@ApiResponse(responseCode = "500", description = "伺服器內部產生異常")
	})
	@PostMapping("/custom-styled")
	public ResponseEntity<byte[]> exportCustomReport(
			@RequestBody @Valid ExportCustomReportResource resource
	) {
		ExportCustomReportCommand command = resourceAssembler.toCommand(resource);
		byte[] excelBytes = exportFinancialReportUseCase.exportCustomReport(command);
		String fileName = (resource.sheetName() != null && !resource.sheetName().isBlank())
				? resource.sheetName().trim() + ".xlsx"
				: "custom_styled_report.xlsx";
		return createAttachmentResponse(excelBytes, fileName);
	}

	/**
	 * 建構標準 HTTP 附件下載回應 (Content-Disposition: attachment)
	 */
	private ResponseEntity<byte[]> createAttachmentResponse(byte[] fileBytes, String fileName) {
		String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType(EXCEL_MIME_TYPE));
		headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
		headers.setContentLength(fileBytes.length);
		return ResponseEntity.ok()
				.headers(headers)
				.body(fileBytes);
	}
}

