package com.example.demo.infrastructure.adapter.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.application.port.out.core.ExcelReaderPort;
import com.example.demo.application.port.out.core.ExcelWriterPort;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.dto.ExcelGottenResult;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("BOM 測試資料與範本 Excel 檔案產製與驗證")
class BomTemplateGeneratorTest {

	private final ExcelWriterPort excelWriterPort = new FastExcelWriterAdapter();
	private final ExcelReaderPort excelReaderPort = new FastExcelReaderAdapter();

	/**
	 * Feature: BOM 測試範本 Excel 產製
	 * Scenario: 產製包含多階展開、單階匯入與物料主檔的多工作表 BOM 測試範本 Excel，並驗證其可讀性
	 * Given 構建包含「多階層BOM展開清單」、「單階BOM匯入清單」與「物料主檔對照表」之 WriteExcelCommand
	 * When 調用 FastExcelWriterAdapter 產製二進位 Excel 檔案並寫入
	 * src/main/resources/templates/
	 * Then 成功產製 .xlsx 檔案，且透過 FastExcelReaderAdapter 讀回驗證三個工作表皆完整無缺
	 */
	@Test
	@DisplayName("場景：產製高階多工作表 BOM 測試範本 Excel 並落地於 templates 目錄")
	void generateBomTemplateExcelFile() throws Exception {
		// ==========================================
		// 1. 工作表一：多階層 BOM 樹狀展開表 (Multi-Level BOM)
		// ==========================================
		List<String> treeHeaders = List.of(
				"階層", "階層序號", "母件料號", "子件料號", "品名", "規格型號",
				"類別", "單位", "單機用量", "損耗率", "有效用量", "標準單價", "小計總成本", "安裝位號", "備註說明");

		List<List<Object>> treeRows = new ArrayList<>();
		// Level 0 (整機)
		treeRows.add(List.of(0, "1", "-", "MAT-FG-001", "4K 專業商用巡檢無人機", "PRO-4K-GPS-V2", "成品", "SET",
				BigDecimal.valueOf(1.0), "0.00%", BigDecimal.valueOf(1.0), BigDecimal.valueOf(28500.0),
				BigDecimal.valueOf(28500.0), "整機", "商業巡檢旗艦機型"));

		// Level 1: 飛控主板
		treeRows.add(List.of(1, "1.1", "MAT-FG-001", "MAT-SFG-001", "飛控主板電路模組總成", "FC-PCB-ASSY-V3", "半成品", "PCS",
				BigDecimal.valueOf(1.0), "0.00%", BigDecimal.valueOf(1.0), BigDecimal.valueOf(8500.0),
				BigDecimal.valueOf(8500.0), "FC-ASM", "核心飛控組裝件"));
		// Level 2 (飛控零件)
		treeRows.add(List.of(2, "1.1.1", "MAT-SFG-001", "MAT-RM-001", "ARM Cortex-M7 雙核心 480MHz 飛行主控 MCU",
				"STM32H743IIT6 LQFP176", "原物料", "PCS",
				BigDecimal.valueOf(1.0), "1.00%", BigDecimal.valueOf(1.01), BigDecimal.valueOf(420.0),
				BigDecimal.valueOf(424.2), "U1", "主控 MCU"));
		treeRows.add(List.of(2, "1.1.2", "MAT-SFG-001", "MAT-RM-002", "6 軸高精度抗震溫補慣性測量陀螺儀 IMU", "ICM-42688-P QFN24",
				"原物料", "PCS",
				BigDecimal.valueOf(2.0), "2.00%", BigDecimal.valueOf(2.04), BigDecimal.valueOf(185.0),
				BigDecimal.valueOf(377.4), "U2, U3", "雙備援姿態陀螺儀"));
		treeRows.add(List.of(2, "1.1.3", "MAT-SFG-001", "MAT-RM-003", "高精度數位式絕對大氣氣壓計傳感器", "BMP390 LGA10", "原物料", "PCS",
				BigDecimal.valueOf(1.0), "1.00%", BigDecimal.valueOf(1.01), BigDecimal.valueOf(95.0),
				BigDecimal.valueOf(95.95), "U4", "高度計"));
		treeRows.add(List.of(2, "1.1.4", "MAT-SFG-001", "MAT-RM-004", "貼片厚膜精密電阻 10K Ohm 1% 0402", "RC0402FR-0710KL",
				"原物料", "PCS",
				BigDecimal.valueOf(28.0), "3.00%", BigDecimal.valueOf(28.84), BigDecimal.valueOf(0.05),
				BigDecimal.valueOf(1.442), "R1-R28", "分壓電阻群"));
		treeRows.add(List.of(2, "1.1.5", "MAT-SFG-001", "MAT-RM-005", "貼片積層多層陶瓷電容 0.1uF 50V X7R 0402",
				"CC0402KRX7R9BB104", "原物料", "PCS",
				BigDecimal.valueOf(36.0), "3.00%", BigDecimal.valueOf(37.08), BigDecimal.valueOf(0.06),
				BigDecimal.valueOf(2.225), "C1-C36", "去耦電容群"));
		treeRows.add(List.of(2, "1.1.6", "MAT-SFG-001", "MAT-RM-006", "沉金四層高頻低阻抗阻抗控制 PCB 空板", "FR-4 TG170 1.2mm", "原物料",
				"PCS",
				BigDecimal.valueOf(1.0), "0.00%", BigDecimal.valueOf(1.0), BigDecimal.valueOf(85.0),
				BigDecimal.valueOf(85.0), "PCB-FC", "電路板基板"));
		treeRows.add(List.of(2, "1.1.7", "MAT-SFG-001", "MAT-RM-061", "Type-C 24Pin 全功能沉板式母座母頭", "USB-C-24P-FEMALE-SMD",
				"原物料", "PCS",
				BigDecimal.valueOf(1.0), "0.00%", BigDecimal.valueOf(1.0), BigDecimal.valueOf(6.8),
				BigDecimal.valueOf(6.8), "J1-TYPEC", "數據與調試介面"));
		treeRows.add(List.of(2, "1.1.8", "MAT-SFG-001", "MAT-RM-063", "JST-GH 1.25mm 間距帶鎖扣 4Pin 壓接端子座", "GHR-04V-S JST",
				"原物料", "PCS",
				BigDecimal.valueOf(4.0), "2.00%", BigDecimal.valueOf(4.08), BigDecimal.valueOf(1.8),
				BigDecimal.valueOf(7.344), "J2-J5", "感測器介面插座"));

		// Level 1: 動力推進單元 (4 組)
		treeRows.add(List.of(1, "1.2", "MAT-FG-001", "MAT-SFG-002", "2212 動力單元推進系統總成", "PROP-UNIT-2212", "半成品", "SET",
				BigDecimal.valueOf(4.0), "0.00%", BigDecimal.valueOf(4.0), BigDecimal.valueOf(1800.0),
				BigDecimal.valueOf(7200.0), "PROP-1~4", "4 組動力單元"));
		// Level 2 (動力零件)
		treeRows.add(List.of(2, "1.2.1", "MAT-SFG-002", "MAT-RM-007", "2212 920KV 外轉子無刷直流馬達", "BLDC-2212-920KV", "原物料",
				"PCS",
				BigDecimal.valueOf(1.0), "0.00%", BigDecimal.valueOf(1.0), BigDecimal.valueOf(280.0),
				BigDecimal.valueOf(280.0), "MOTOR-1", "無刷動力馬達"));
		treeRows.add(List.of(2, "1.2.2", "MAT-SFG-002", "MAT-RM-008", "30A 高頻 PWM 無刷電調驅動晶片模組", "ESC-BLHELI-32-30A",
				"原物料", "PCS",
				BigDecimal.valueOf(1.0), "0.00%", BigDecimal.valueOf(1.0), BigDecimal.valueOf(190.0),
				BigDecimal.valueOf(190.0), "ESC-1", "高頻調速電調"));
		treeRows.add(List.of(2, "1.2.3", "MAT-SFG-002", "MAT-RM-009", "9450 自鎖式高強度碳纖維正反三葉螺旋槳", "CF-PROP-9450-PAIR",
				"原物料", "SET",
				BigDecimal.valueOf(1.0), "0.00%", BigDecimal.valueOf(1.0), BigDecimal.valueOf(120.0),
				BigDecimal.valueOf(120.0), "PROP-1", "碳纖維螺旋槳"));

		// Level 1: 4K 雲台相機
		treeRows.add(List.of(1, "1.3", "MAT-FG-001", "MAT-SFG-003", "三軸無刷增穩 4K 雲台相機總成", "GIMBAL-CAM-4K", "半成品", "SET",
				BigDecimal.valueOf(1.0), "0.00%", BigDecimal.valueOf(1.0), BigDecimal.valueOf(6200.0),
				BigDecimal.valueOf(6200.0), "CAM-GIMBAL", "4K 光學吊艙"));
		// Level 2 (雲台相機零件)
		treeRows.add(List.of(2, "1.3.1", "MAT-SFG-003", "MAT-RM-010", "SONY 1/2 吋 4800 萬像素堆疊式 CMOS 感光元件",
				"IMX586 BGA-CSP", "原物料", "PCS",
				BigDecimal.valueOf(1.0), "0.00%", BigDecimal.valueOf(1.0), BigDecimal.valueOf(1250.0),
				BigDecimal.valueOf(1250.0), "CIS-1", "影像感測器"));
		treeRows.add(List.of(2, "1.3.2", "MAT-SFG-003", "MAT-RM-011", "F/2.8 4K 高解析多層抗反射鍍膜光學鏡頭組", "LENS-4K-M12-FOV85",
				"原物料", "SET",
				BigDecimal.valueOf(1.0), "0.00%", BigDecimal.valueOf(1.0), BigDecimal.valueOf(850.0),
				BigDecimal.valueOf(850.0), "OPT-LENS", "光學鏡頭"));
		treeRows.add(List.of(2, "1.3.3", "MAT-SFG-003", "MAT-RM-012", "三軸無刷微型超精密航向與俯仰雲台微電機", "GIMBAL-MOTOR-2804", "原物料",
				"PCS",
				BigDecimal.valueOf(3.0), "0.00%", BigDecimal.valueOf(3.0), BigDecimal.valueOf(310.0),
				BigDecimal.valueOf(930.0), "MOT-P,R,Y", "三軸電機"));

		// Level 1: 4S 5000mAh 動力鋰電池 (2 組)
		treeRows.add(
				List.of(1, "1.4", "MAT-FG-001", "MAT-SFG-004", "4S 智慧動力鋰電池包模組總成", "BATT-PACK-4S-5000", "半成品", "SET",
						BigDecimal.valueOf(2.0), "0.00%", BigDecimal.valueOf(2.0), BigDecimal.valueOf(3800.0),
						BigDecimal.valueOf(7600.0), "BAT-1, 2", "雙電池熱備份"));
		// Level 2 (電池包零件)
		treeRows.add(List.of(2, "1.4.1", "MAT-SFG-004", "MAT-RM-013", "21700 高倍率三元動力鋰電芯 4500mAh 45A", "INR21700-45D",
				"原物料", "PCS",
				BigDecimal.valueOf(4.0), "0.00%", BigDecimal.valueOf(4.0), BigDecimal.valueOf(160.0),
				BigDecimal.valueOf(640.0), "CELL-1~4", "動力電芯"));
		treeRows.add(List.of(2, "1.4.2", "MAT-SFG-004", "MAT-RM-014", "4S 100A 智慧電池保護均衡監控 BMS 晶片組", "BMS-4S-100A-SMBUS",
				"原物料", "PCS",
				BigDecimal.valueOf(1.0), "0.00%", BigDecimal.valueOf(1.0), BigDecimal.valueOf(450.0),
				BigDecimal.valueOf(450.0), "BMS-BRD", "保護板模組"));
		treeRows.add(List.of(2, "1.4.3", "MAT-SFG-004", "MAT-RM-015", "高阻燃抗摔改性 PC/ABS 塑料射出電池外殼", "PC-ABS-V0-BLACK",
				"原物料", "SET",
				BigDecimal.valueOf(1.0), "0.00%", BigDecimal.valueOf(1.0), BigDecimal.valueOf(180.0),
				BigDecimal.valueOf(180.0), "SHELL-BAT", "阻燃外殼"));
		treeRows.add(List.of(2, "1.4.4", "MAT-SFG-004", "MAT-RM-062", "XT60H 高溫阻燃大電流鍍金電源插頭公母對裝", "AMASS-XT60H-PAIR",
				"原物料", "SET",
				BigDecimal.valueOf(1.0), "0.00%", BigDecimal.valueOf(1.0), BigDecimal.valueOf(18.0),
				BigDecimal.valueOf(18.0), "XT60H-CONN", "電源插頭"));

		// Level 1: 其他結構與零件
		treeRows.add(
				List.of(1, "1.5", "MAT-FG-001", "MAT-RM-016", "3K 斜紋全碳纖維一體成型四軸輕量化機架", "CF-FRAME-QUAD-450", "原物料", "SET",
						BigDecimal.valueOf(1.0), "0.00%", BigDecimal.valueOf(1.0), BigDecimal.valueOf(1800.0),
						BigDecimal.valueOf(1800.0), "FRAME-BODY", "碳纖維機身"));
		treeRows.add(
				List.of(1, "1.6", "MAT-FG-001", "MAT-RM-017", "GPS/北斗/格洛納斯三模陶瓷有源雙頻天線", "ATGM336H-5N31", "原物料", "PCS",
						BigDecimal.valueOf(1.0), "0.00%", BigDecimal.valueOf(1.0), BigDecimal.valueOf(220.0),
						BigDecimal.valueOf(220.0), "GPS-ANT", "陶瓷定位天線"));
		treeRows.add(List.of(1, "1.7", "MAT-FG-001", "MAT-RM-018", "M3*8mm 航太級高強度 TC4 鈦合金內六角螺絲", "TC4-HEX-M3-8", "原物料",
				"PCS",
				BigDecimal.valueOf(24.0), "5.00%", BigDecimal.valueOf(25.2), BigDecimal.valueOf(4.5),
				BigDecimal.valueOf(113.4), "SCREW-M3-8", "裝配緊固件"));
		treeRows.add(List.of(1, "1.8", "MAT-FG-001", "MAT-RM-019", "防震防水耐壓拉桿儀器設備氣密防護箱", "PELICAN-CASE-1510-EVA", "原物料",
				"PCS",
				BigDecimal.valueOf(1.0), "0.00%", BigDecimal.valueOf(1.0), BigDecimal.valueOf(1500.0),
				BigDecimal.valueOf(1500.0), "BOX-CASE", "氣密包裝手提箱"));

		WriteSheetCommand sheet1 = WriteSheetCommand.builder()
				.sheetName("多階層BOM展開清單")
				.headers(treeHeaders)
				.rows(treeRows)
				.freezeHeader(true)
				.headerFillColor("1F4E78")
				.headerFontColor("FFFFFF")
				.build();

		// ==========================================
		// 2. 工作表二：單階 BOM 配方匯入清單 (Single-Level BOM Import)
		// ==========================================
		List<String> importHeaders = List.of(
				"母件料號", "BOM版本", "項次", "子件料號", "子件品名", "單機用量", "損耗率", "安裝位號", "備註說明");

		List<List<Object>> importRows = List.of(
				// MAT-FG-001 配方
				List.of("MAT-FG-001", "V1.0", 10, "MAT-SFG-001", "飛控主板電路模組總成", BigDecimal.valueOf(1.0),
						BigDecimal.valueOf(0.0), "FC-ASM", "核心飛控組裝件"),
				List.of("MAT-FG-001", "V1.0", 20, "MAT-SFG-002", "2212 動力單元推進系統總成", BigDecimal.valueOf(4.0),
						BigDecimal.valueOf(0.0), "PROP-1~4", "4 組動力推進總成"),
				List.of("MAT-FG-001", "V1.0", 30, "MAT-SFG-003", "三軸無刷增穩 4K 雲台相機總成", BigDecimal.valueOf(1.0),
						BigDecimal.valueOf(0.0), "CAM-GIMBAL", "4K 光學吊艙"),
				List.of("MAT-FG-001", "V1.0", 40, "MAT-SFG-004", "4S 智慧動力鋰電池包模組總成", BigDecimal.valueOf(2.0),
						BigDecimal.valueOf(0.0), "BAT-1, 2", "雙電池熱備份"),
				List.of("MAT-FG-001", "V1.0", 50, "MAT-RM-016", "3K 斜紋全碳纖維一體成型四軸輕量化機架", BigDecimal.valueOf(1.0),
						BigDecimal.valueOf(0.0), "FRAME-BODY", "碳纖維機架本體"),
				List.of("MAT-FG-001", "V1.0", 60, "MAT-RM-017", "GPS/北斗/格洛納斯三模陶瓷有源雙頻天線", BigDecimal.valueOf(1.0),
						BigDecimal.valueOf(0.0), "GPS-ANT", "衛星導航陶瓷天線"),
				List.of("MAT-FG-001", "V1.0", 70, "MAT-RM-018", "M3*8mm 航太級高強度 TC4 鈦合金內六角螺絲", BigDecimal.valueOf(24.0),
						BigDecimal.valueOf(0.05), "SCREW-M3-8", "裝配緊固螺絲 (含 5% 損耗)"),
				List.of("MAT-FG-001", "V1.0", 80, "MAT-RM-019", "防震防水耐壓拉桿儀器設備氣密防護箱", BigDecimal.valueOf(1.0),
						BigDecimal.valueOf(0.0), "BOX-CASE", "氣密包裝防護手提箱"),

				// MAT-SFG-001 飛控次階配方
				List.of("MAT-SFG-001", "V1.0", 10, "MAT-RM-001", "ARM Cortex-M7 雙核心 480MHz 飛行主控 MCU",
						BigDecimal.valueOf(1.0), BigDecimal.valueOf(0.01), "U1", "主控 MCU 晶片"),
				List.of("MAT-SFG-001", "V1.0", 20, "MAT-RM-002", "6 軸高精度抗震溫補慣性測量陀螺儀 IMU", BigDecimal.valueOf(2.0),
						BigDecimal.valueOf(0.02), "U2, U3", "雙備援 6 軸陀螺儀"),
				List.of("MAT-SFG-001", "V1.0", 30, "MAT-RM-003", "高精度數位式絕對大氣氣壓計傳感器", BigDecimal.valueOf(1.0),
						BigDecimal.valueOf(0.01), "U4", "氣壓計傳感器"),
				List.of("MAT-SFG-001", "V1.0", 40, "MAT-RM-004", "貼片厚膜精密電阻 10K Ohm 1% 0402", BigDecimal.valueOf(28.0),
						BigDecimal.valueOf(0.03), "R1-R28", "精密分壓電阻群"),
				List.of("MAT-SFG-001", "V1.0", 50, "MAT-RM-005", "貼片積層多層陶瓷電容 0.1uF 50V X7R 0402",
						BigDecimal.valueOf(36.0), BigDecimal.valueOf(0.03), "C1-C36", "去耦陶瓷濾波電容"),
				List.of("MAT-SFG-001", "V1.0", 60, "MAT-RM-006", "沉金四層高頻低阻抗阻抗控制 PCB 空板", BigDecimal.valueOf(1.0),
						BigDecimal.valueOf(0.0), "PCB-FC", "高頻四層電路空板"),
				List.of("MAT-SFG-001", "V1.0", 70, "MAT-RM-061", "Type-C 24Pin 全功能沉板式母座母頭", BigDecimal.valueOf(1.0),
						BigDecimal.valueOf(0.0), "J1-TYPEC", "全功能 Type-C 調試通信母座"),
				List.of("MAT-SFG-001", "V1.0", 80, "MAT-RM-063", "JST-GH 1.25mm 間距帶鎖扣 4Pin 壓接端子座",
						BigDecimal.valueOf(4.0), BigDecimal.valueOf(0.02), "J2-J5", "感測器通訊端子座"));

		WriteSheetCommand sheet2 = WriteSheetCommand.builder()
				.sheetName("單階BOM匯入清單")
				.headers(importHeaders)
				.rows(importRows)
				.freezeHeader(true)
				.headerFillColor("2F5597")
				.headerFontColor("FFFFFF")
				.build();

		// ==========================================
		// 3. 工作表三：物料主檔對照表 (Material Master)
		// ==========================================
		List<String> matHeaders = List.of(
				"物料料號", "物料品名", "規格型號", "物料分類", "基本單位", "標準單價 (TWD)", "生命週期狀態");

		List<List<Object>> matRows = List.of(
				List.of("MAT-FG-001", "4K 專業商用巡檢無人機", "PRO-4K-GPS-V2", "成品 (FINISHED_GOODS)", "SET",
						BigDecimal.valueOf(28500.0), "正常使用中 (ACTIVE)"),
				List.of("MAT-FG-002", "工業級邊緣運算物聯網閘道器", "EG-5000-AI", "成品 (FINISHED_GOODS)", "PCS",
						BigDecimal.valueOf(18200.0), "正常使用中 (ACTIVE)"),
				List.of("MAT-FG-003", "22kW 智慧雙槍交流充電樁", "EVC-22KW-OCPP", "成品 (FINISHED_GOODS)", "SET",
						BigDecimal.valueOf(36800.0), "正常使用中 (ACTIVE)"),
				List.of("MAT-SFG-001", "飛控主板電路模組總成", "FC-PCB-ASSY-V3", "半成品 (SEMI_FINISHED)", "PCS",
						BigDecimal.valueOf(8500.0), "正常使用中 (ACTIVE)"),
				List.of("MAT-SFG-002", "2212 動力單元推進系統總成", "PROP-UNIT-2212", "半成品 (SEMI_FINISHED)", "SET",
						BigDecimal.valueOf(1800.0), "正常使用中 (ACTIVE)"),
				List.of("MAT-SFG-003", "三軸無刷增穩 4K 雲台相機總成", "GIMBAL-CAM-4K", "半成品 (SEMI_FINISHED)", "SET",
						BigDecimal.valueOf(6200.0), "正常使用中 (ACTIVE)"),
				List.of("MAT-SFG-004", "4S 智慧動力鋰電池包模組總成", "BATT-PACK-4S-5000", "半成品 (SEMI_FINISHED)", "SET",
						BigDecimal.valueOf(3800.0), "正常使用中 (ACTIVE)"),
				List.of("MAT-RM-001", "ARM Cortex-M7 雙核心 480MHz 飛行主控 MCU", "STM32H743IIT6 LQFP176",
						"原物料 (RAW_MATERIAL)", "PCS", BigDecimal.valueOf(420.0), "正常使用中 (ACTIVE)"),
				List.of("MAT-RM-002", "6 軸高精度抗震溫補慣性測量陀螺儀 IMU", "ICM-42688-P QFN24", "原物料 (RAW_MATERIAL)", "PCS",
						BigDecimal.valueOf(185.0), "正常使用中 (ACTIVE)"),
				List.of("MAT-RM-003", "高精度數位式絕對大氣氣壓計傳感器", "BMP390 LGA10", "原物料 (RAW_MATERIAL)", "PCS",
						BigDecimal.valueOf(95.0), "正常使用中 (ACTIVE)"),
				List.of("MAT-RM-004", "貼片厚膜精密電阻 10K Ohm 1% 0402", "RC0402FR-0710KL", "原物料 (RAW_MATERIAL)", "PCS",
						BigDecimal.valueOf(0.05), "正常使用中 (ACTIVE)"),
				List.of("MAT-RM-005", "貼片積層多層陶瓷電容 0.1uF 50V X7R 0402", "CC0402KRX7R9BB104", "原物料 (RAW_MATERIAL)", "PCS",
						BigDecimal.valueOf(0.06), "正常使用中 (ACTIVE)"),
				List.of("MAT-RM-006", "沉金四層高頻低阻抗阻抗控制 PCB 空板", "FR-4 TG170 1.2mm", "原物料 (RAW_MATERIAL)", "PCS",
						BigDecimal.valueOf(85.0), "正常使用中 (ACTIVE)"),
				List.of("MAT-RM-007", "2212 920KV 外轉子無刷直流馬達", "BLDC-2212-920KV", "原物料 (RAW_MATERIAL)", "PCS",
						BigDecimal.valueOf(280.0), "正常使用中 (ACTIVE)"),
				List.of("MAT-RM-008", "30A 高頻 PWM 無刷電調驅動晶片模組", "ESC-BLHELI-32-30A", "原物料 (RAW_MATERIAL)", "PCS",
						BigDecimal.valueOf(190.0), "正常使用中 (ACTIVE)"),
				List.of("MAT-RM-009", "9450 自鎖式高強度碳纖維正反三葉螺旋槳", "CF-PROP-9450-PAIR", "原物料 (RAW_MATERIAL)", "SET",
						BigDecimal.valueOf(120.0), "正常使用中 (ACTIVE)"),
				List.of("MAT-RM-010", "SONY 1/2 吋 4800 萬像素堆疊式 CMOS 感光元件", "IMX586 BGA-CSP", "原物料 (RAW_MATERIAL)", "PCS",
						BigDecimal.valueOf(1250.0), "正常使用中 (ACTIVE)"),
				List.of("MAT-RM-016", "3K 斜紋全碳纖維一體成型四軸輕量化機架", "CF-FRAME-QUAD-450", "原物料 (RAW_MATERIAL)", "SET",
						BigDecimal.valueOf(1800.0), "正常使用中 (ACTIVE)"),
				List.of("MAT-RM-018", "M3*8mm 航太級高強度 TC4 鈦合金內六角螺絲", "TC4-HEX-M3-8", "原物料 (RAW_MATERIAL)", "PCS",
						BigDecimal.valueOf(4.5), "正常使用中 (ACTIVE)"));

		WriteSheetCommand sheet3 = WriteSheetCommand.builder()
				.sheetName("物料主檔對照表")
				.headers(matHeaders)
				.rows(matRows)
				.freezeHeader(true)
				.headerFillColor("385723")
				.headerFontColor("FFFFFF")
				.build();

		WriteExcelCommand command = WriteExcelCommand.builder()
				.applicationName("ExcelModule-BOM-Generator")
				.applicationVersion("1.0")
				.sheets(List.of(sheet1, sheet2, sheet3))
				.build();

		// When - 產製二進位 Excel 數據
		byte[] excelBytes = excelWriterPort.write(command);
		assertThat(excelBytes).isNotEmpty();

		// 落地於 src/main/resources/templates/BOM_IMPORT.xlsx
		Path targetPath = Paths.get("src/main/resources/templates/BOM_IMPORT.xlsx");
		Files.createDirectories(targetPath.getParent());
		Files.write(targetPath, excelBytes);

		// 另外複製一份作為 BOM_TEMPLATE.xlsx
		Path templateCopy = Paths.get("src/main/resources/templates/BOM_TEMPLATE.xlsx");
		Files.write(templateCopy, excelBytes);

		// 另外複製一份至 doc/template/BOM_IMPORT.xlsx (若被外部程式如 Excel 開啟鎖定則安全容錯)
		Path docTemplateDir = Paths.get("doc/template");
		Files.createDirectories(docTemplateDir);
		try {
			Files.write(docTemplateDir.resolve("BOM_IMPORT.xlsx"), excelBytes);
		} catch (IOException e) {
			System.err.println("提示：doc/template/BOM_IMPORT.xlsx 檔案被外部軟體開啟鎖定，略過文件目錄複製: " + e.getMessage());
		}

		// Then - 讀回驗證三個工作表完整性
		ExcelGottenResult readResult = excelReaderPort.read(excelBytes);
		assertThat(readResult.getSheets()).hasSize(3);
		assertThat(readResult.getSheets().get(0).getSheetName()).isEqualTo("多階層BOM展開清單");
		assertThat(readResult.getSheets().get(0).getRows()).hasSize(treeRows.size());
		assertThat(readResult.getSheets().get(1).getSheetName()).isEqualTo("單階BOM匯入清單");
		assertThat(readResult.getSheets().get(1).getRows()).hasSize(importRows.size());
		assertThat(readResult.getSheets().get(2).getSheetName()).isEqualTo("物料主檔對照表");
		assertThat(readResult.getSheets().get(2).getRows()).hasSize(matRows.size());

		System.out.println("成功生成 BOM 測試範本 Excel 檔案：");
		System.out.println("  1. " + targetPath.toAbsolutePath());
		System.out.println("  2. " + templateCopy.toAbsolutePath());
		System.out.println("  3. " + docTemplateDir.resolve("BOM_IMPORT.xlsx").toAbsolutePath());
	}
}
