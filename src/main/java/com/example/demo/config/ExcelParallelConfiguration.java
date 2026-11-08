package com.example.demo.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 高並發平行與分片 Excel 匯出執行緒池設定 (Excel Parallel Export Thread Pool Configuration)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 遵循 AGENTS.md 規則 40~42，作為專案配置層 (Configuration Layer - The Dirtiest Layer)。<br>
 * 統一集中管理多執行緒查詢與活頁簿並行分片產製所需之底層 {@link ExecutorService} Bean。<br>
 * 嚴禁將此類底層執行緒池建構技術細節散落於 Domain 或 Application 業務層中。
 */
@Configuration
public class ExcelParallelConfiguration {

	/**
	 * 註冊平行管線與分片匯出專用執行緒池
	 *
	 * @return 執行緒池實例 (支援優雅關閉)
	 */
	@Bean(name = "excelParallelExecutorService", destroyMethod = "shutdown")
	public ExecutorService excelParallelExecutorService() {
		int cpuCores = Runtime.getRuntime().availableProcessors();
		int corePoolSize = Math.max(4, cpuCores);
		int maxPoolSize = Math.max(8, cpuCores * 2);

		ThreadFactory threadFactory = new ThreadFactory() {
			private final AtomicInteger counter = new AtomicInteger(1);

			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, "excel-parallel-worker-" + counter.getAndIncrement());
				thread.setDaemon(true); // 守護執行緒，不阻礙 JVM 關機
				return thread;
			}
		};

		return new ThreadPoolExecutor(
				corePoolSize,
				maxPoolSize,
				60L,
				TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(500),
				threadFactory,
				new ThreadPoolExecutor.CallerRunsPolicy() // 隊列滿時降級由呼叫者執行緒執行，保障不丟失任務
		);
	}
}

