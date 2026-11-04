package com.example.demo.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 非同步工作排程與執行緒池配置類 (Async Configuration)
 * <p>
 * 遵循 Clean Architecture 配置層規範 (AGENTS.md 規則 40，集中於 {@code config/} 套件)。
 * 提供 Spring {@code @Async} 非同步長任務所需之專屬執行緒池，
 * 確保大數據 Excel 匯出與匯入任務在獨立背景執行緒中流暢運作，避免耗盡主執行緒。
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {

	/**
	 * 配置 Excel 非同步長任務專屬執行緒池
	 *
	 * @return 配置完成之 TaskExecutor
	 */
	@Bean(name = "excelTaskExecutor")
	public Executor excelTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(16);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("excel-async-task-");
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(60);
		executor.initialize();
		return executor;
	}
}

