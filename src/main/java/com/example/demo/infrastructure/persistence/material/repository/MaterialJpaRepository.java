package com.example.demo.infrastructure.persistence.material.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.material.aggregate.vo.MaterialCategory;
import com.example.demo.infrastructure.persistence.material.entity.MaterialJpaEntity;

/**
 * 物料 Spring Data JPA 儲存庫介面 (Material Spring Data JPA Repository)
 * <p>
 * 提供物料主檔資料表 {@code t_material} 之 CRUD 與客製化查詢衍生方法。
 */
@Repository
public interface MaterialJpaRepository extends JpaRepository<MaterialJpaEntity, String> {

	/**
	 * 依據物料料號查詢物料實體。
	 *
	 * @param materialCode 物料料號字串
	 * @return 物料 JPA 實體 Optional 包裝
	 */
	Optional<MaterialJpaEntity> findByMaterialCode(String materialCode);

	/**
	 * 依據物料分類查詢物料清單。
	 *
	 * @param category 物料分類枚舉
	 * @return 符合分類之物料 JPA 實體清單
	 */
	List<MaterialJpaEntity> findByCategory(MaterialCategory category);

	/**
	 * 檢查指定料號是否存在。
	 *
	 * @param materialCode 物料料號字串
	 * @return 若存在回傳 {@code true}，否則回傳 {@code false}
	 */
	boolean existsByMaterialCode(String materialCode);
}

