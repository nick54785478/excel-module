package com.example.demo.application.port.out.bom;

import java.util.List;
import java.util.Optional;

import com.example.demo.domain.material.aggregate.root.Material;
import com.example.demo.domain.material.aggregate.vo.MaterialCategory;
import com.example.demo.domain.material.aggregate.vo.MaterialCode;
import com.example.demo.domain.material.aggregate.vo.MaterialId;

/**
 * 物料儲存庫輸出埠 (Material Repository Outbound Port)
 * <p>
 * 遵循六角形架構規範，代表應用層對外要求持久化設施提供的物料主檔操作合約。
 * <p>
 * <b>嚴禁依賴任何技術類物件</b>：傳參及回傳型別嚴格限定為領域聚合根 {@link Material}、實質物件與基礎型別。
 */
public interface MaterialRepositoryPort {

	/**
	 * 依據物料識別碼查詢物料。
	 *
	 * @param id 物料識別碼實質物件
	 * @return 物料聚合根 Optional 包裝
	 */
	Optional<Material> findById(MaterialId id);

	/**
	 * 依據物料料號查詢物料。
	 *
	 * @param code 物料料號實質物件
	 * @return 物料聚合根 Optional 包裝
	 */
	Optional<Material> findByCode(MaterialCode code);

	/**
	 * 查詢系統中所有物料主檔。
	 *
	 * @return 物料聚合根清單
	 */
	List<Material> findAll();

	/**
	 * 依據物料分類查詢物料清單。
	 *
	 * @param category 物料分類 (成品/半成品/原物料)
	 * @return 符合分類之物料聚合根清單
	 */
	List<Material> findByCategory(MaterialCategory category);

	/**
	 * 儲存或更新物料聚合根。
	 *
	 * @param material 物料聚合根
	 * @return 持久化後之物料聚合根
	 */
	Material save(Material material);

	/**
	 * 依據物料識別碼刪除物料。
	 *
	 * @param id 物料識別碼實質物件
	 */
	void deleteById(MaterialId id);

	/**
	 * 檢查指定料號是否存在。
	 *
	 * @param code 物料料號
	 * @return 若存在回傳 {@code true}，否則回傳 {@code false}
	 */
	boolean existsByCode(MaterialCode code);
}

