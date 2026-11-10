package com.example.demo.application.port.out.bom;

import java.util.List;
import java.util.Optional;

import com.example.demo.domain.bom.aggregate.root.Bom;
import com.example.demo.domain.bom.aggregate.vo.BomId;
import com.example.demo.domain.bom.aggregate.vo.BomVersion;
import com.example.demo.domain.material.aggregate.vo.MaterialId;

/**
 * 物料清單儲存庫輸出埠 (BOM Repository Outbound Port)
 * <p>
 * 遵循六角形架構規範，代表應用層對外要求持久化設施提供的物料清單 (BOM) 配方操作合約。
 * <p>
 * <b>嚴禁依賴任何技術類物件</b>：傳參及回傳型別嚴格限定為領域聚合根 {@link Bom}、實質物件與基礎型別。
 */
public interface BomRepositoryPort {

	/**
	 * 依據 BOM 識別碼查詢 BOM 聚合根。
	 *
	 * @param id BOM 識別碼實質物件
	 * @return BOM 聚合根 Optional 包裝
	 */
	Optional<Bom> findById(BomId id);

	/**
	 * 依據母件物料識別碼與版本號查詢特定版本之 BOM。
	 *
	 * @param parentMaterialId 母件物料識別碼
	 * @param version BOM 版本號
	 * @return BOM 聚合根 Optional 包裝
	 */
	Optional<Bom> findByParentMaterialIdAndVersion(MaterialId parentMaterialId, BomVersion version);

	/**
	 * 依據母件物料識別碼查詢其所屬之所有版本 BOM。
	 *
	 * @param parentMaterialId 母件物料識別碼
	 * @return 該母件所屬之 BOM 聚合根清單
	 */
	List<Bom> findByParentMaterialId(MaterialId parentMaterialId);

	/**
	 * 查詢系統中所有 BOM 聚合根。
	 *
	 * @return 所有 BOM 聚合根清單
	 */
	List<Bom> findAll();

	/**
	 * 儲存或更新 BOM 聚合根（包含其內部所有子件 BomItem）。
	 *
	 * @param bom BOM 聚合根
	 * @return 持久化後之 BOM 聚合根
	 */
	Bom save(Bom bom);

	/**
	 * 依據 BOM 識別碼刪除 BOM。
	 *
	 * @param id BOM 識別碼實質物件
	 */
	void deleteById(BomId id);

	/**
	 * 檢查指定母件與版本之 BOM 是否已存在。
	 *
	 * @param parentMaterialId 母件物料識別碼
	 * @param version BOM 版本號
	 * @return 若存在回傳 {@code true}，否則回傳 {@code false}
	 */
	boolean existsByParentMaterialIdAndVersion(MaterialId parentMaterialId, BomVersion version);
}

