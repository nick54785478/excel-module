package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * AOP 宣告式切面配置類 (AOP Configuration)
 * <p>
 * 遵循 Clean Architecture 配置層規範 (AGENTS.md 規則 40，集中於 {@code config/} 套件)。
 * 啟用 Spring AOP 與 AspectJ 註解自動代理，確保 {@code @TrackExcelTask} 切面正確攔截各層 Adapter 之業務呼叫。
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AopConfiguration {
}

