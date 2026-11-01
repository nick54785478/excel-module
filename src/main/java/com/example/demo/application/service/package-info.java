/**
 * 應用層服務 (Application Services)
 * <p>
 * 實作 {@code port/in/} 中的 UseCase 介面，負責協調 Domain Aggregate 與 Application Outbound Port ({@code port/out/})。
 * 命名規範：以 {@code ApplicationService} (無 CQRS) 或 {@code CommandService} / {@code QueryService} (CQRS) 結尾。
 */
package com.example.demo.application.service;
