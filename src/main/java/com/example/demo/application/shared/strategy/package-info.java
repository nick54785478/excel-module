/**
 * 應用層共享裝飾與排版策略套件 (Application Shared Decorator & Styling Strategies)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，本套件封裝由應用層主導的策略外掛介面 (Policy / SPI)。
 * 與 {@code application.port.out} 中的通訊埠 (Outbound Ports) 進行清楚的職責分離：
 * <ul>
 *   <li>{@code port.out.ExcelReportDecoratorFactoryPort} 作為連通基礎設施的邊界孔道 (Outbound Port)。</li>
 *   <li>本套件中的 {@link com.example.demo.application.shared.strategy.ExcelReportDecoratorStrategy}
 *       作為策略模式 (Strategy Pattern) 中的策略規格契約，由工廠進行統一管理與執行期動態路由。</li>
 * </ul>
 */
package com.example.demo.application.shared.strategy;

