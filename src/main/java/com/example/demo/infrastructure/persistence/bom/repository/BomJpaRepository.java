package com.example.demo.infrastructure.persistence.bom.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.infrastructure.persistence.bom.entity.BomJpaEntity;

/**
 * 物料清單 Spring Data JPA 儲存庫介面 (BOM Spring Data JPA Repository)
 * <p>
 * 提供產品結構清單資料表 {@code t_bom} 之 CRUD 與版本衍生查詢操作。
 * 使用 {@link EntityGraph} 確保查詢 BOM 聚合根時，其內部所有子件 BomItem 一次性即時載入，
 * 避免 N+1 查詢問題並防止離開交易後引發 {@code LazyInitializationException}。
 */
@Repository
public interface BomJpaRepository extends JpaRepository<BomJpaEntity, String> {

	/**
	 * 依據識別碼查詢 BOM 聚合根及其內部子項實體。
	 *
	 * @param id BOM 識別碼
	 * @return BOM JPA 實體 Optional 包裝
	 */
	@Override
	@EntityGraph(attributePaths = {"items"})
	Optional<BomJpaEntity> findById(String id);

	/**
	 * 查詢全系統所有 BOM 聚合根及其內部子項實體。
	 *
	 * @return BOM JPA 實體清單
	 */
	@Override
	@EntityGraph(attributePaths = {"items"})
	List<BomJpaEntity> findAll();

	/**
	 * 依據母件物料識別碼與版本號查詢 BOM 實體。
	 *
	 * @param parentMaterialId 母件物料識別碼
	 * @param bomVersion BOM 版本號
	 * @return BOM JPA 實體 Optional 包裝
	 */
	@EntityGraph(attributePaths = {"items"})
	Optional<BomJpaEntity> findByParentMaterialIdAndBomVersion(String parentMaterialId, String bomVersion);

	/**
	 * 依據母件物料識別碼查詢其所屬之所有版本 BOM 實體。
	 *
	 * @param parentMaterialId 母件物料識別碼
	 * @return BOM JPA 實體清單
	 */
	@EntityGraph(attributePaths = {"items"})
	List<BomJpaEntity> findByParentMaterialId(String parentMaterialId);

	/**
	 * 檢查指定母件與版本號之 BOM 是否已存在。
	 *
	 * @param parentMaterialId 母件物料識別碼
	 * @param bomVersion BOM 版本號
	 * @return 若存在回傳 {@code true}，否則回傳 {@code false}
	 */
	boolean existsByParentMaterialIdAndBomVersion(String parentMaterialId, String bomVersion);
}

