/**
 * 領域層 (Domain Layer)
 * <p>
 * 遵循 DDD 領域驅動設計原則：
 * <ul>
 *   <li><b>核心領域至上</b>：領域模型封裝完整的商業規則與領域邏輯。</li>
 *   <li><b>零框架與零外層依賴</b>：本套件內部為純 Java (Pure Java)，絕對不可依賴 Spring 框架、JPA 註解或資料庫技術，嚴禁依賴 application 或 infrastructure 層。</li>
 *   <li><b>嚴格禁止 Lombok 侵入</b>：Aggregate 內部嚴禁使用 Lombok (如 {@code @Data}, {@code @Getter}, {@code @Setter})，以確保封裝性不被破壞。</li>
 * </ul>
 */
package com.example.demo.domain;
