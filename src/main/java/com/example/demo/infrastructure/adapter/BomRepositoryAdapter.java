package com.example.demo.infrastructure.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.demo.application.port.out.BomRepositoryPort;
import com.example.demo.domain.bom.aggregate.root.Bom;
import com.example.demo.domain.bom.aggregate.vo.BomId;
import com.example.demo.domain.bom.aggregate.vo.BomVersion;
import com.example.demo.domain.material.aggregate.vo.MaterialId;
import com.example.demo.infrastructure.persistence.bom.entity.BomJpaEntity;
import com.example.demo.infrastructure.persistence.bom.repository.BomJpaRepository;

/**
 * 物料清單儲存庫輸出適配器 (BOM Repository Outbound Adapter)
 * <p>
 * 遵循六角形架構規範，實作應用層之 {@link BomRepositoryPort}，
 * 協調 Spring Data JPA {@link BomJpaRepository} 進行產品結構 BOM 及其內部各階子項之持久化操作。
 * <p>
 * <b>事務職責劃分</b>：作為純粹的 Outbound Adapter，本類別不宣告 `@Transactional` 事務邊界。
 * 業務事務邊界與 Unit of Work 統一由 Application Service (CommandService / QueryService) 明確宣告管轄。
 */
@Component
public class BomRepositoryAdapter implements BomRepositoryPort {

	private final BomJpaRepository bomJpaRepository;

	/**
	 * 建構 BOM 儲存庫適配器。
	 *
	 * @param bomJpaRepository BOM Spring Data JPA 儲存庫
	 */
	public BomRepositoryAdapter(BomJpaRepository bomJpaRepository) {
		this.bomJpaRepository = Objects.requireNonNull(bomJpaRepository, "BomJpaRepository 不得為空");
	}

	@Override
	public Optional<Bom> findById(BomId id) {
		Objects.requireNonNull(id, "BOM 識別碼不得為空");
		return bomJpaRepository.findById(id.value())
				.map(BomJpaEntity::toDomain);
	}

	@Override
	public Optional<Bom> findByParentMaterialIdAndVersion(MaterialId parentMaterialId, BomVersion version) {
		Objects.requireNonNull(parentMaterialId, "母件物料識別碼不得為空");
		Objects.requireNonNull(version, "BOM 版本號不得為空");
		return bomJpaRepository.findByParentMaterialIdAndBomVersion(parentMaterialId.value(), version.value())
				.map(BomJpaEntity::toDomain);
	}

	@Override
	public List<Bom> findByParentMaterialId(MaterialId parentMaterialId) {
		Objects.requireNonNull(parentMaterialId, "母件物料識別碼不得為空");
		List<BomJpaEntity> entities = bomJpaRepository.findByParentMaterialId(parentMaterialId.value());
		List<Bom> domains = new ArrayList<>(entities.size());
		for (BomJpaEntity entity : entities) {
			domains.add(entity.toDomain());
		}
		return domains;
	}

	@Override
	public List<Bom> findAll() {
		List<BomJpaEntity> entities = bomJpaRepository.findAll();
		List<Bom> domains = new ArrayList<>(entities.size());
		for (BomJpaEntity entity : entities) {
			domains.add(entity.toDomain());
		}
		return domains;
	}

	@Override
	public Bom save(Bom bom) {
		Objects.requireNonNull(bom, "待儲存之 BOM 聚合根不得為空");
		BomJpaEntity entity = BomJpaEntity.fromDomain(bom);
		BomJpaEntity saved = bomJpaRepository.save(entity);
		return saved.toDomain();
	}

	@Override
	public void deleteById(BomId id) {
		Objects.requireNonNull(id, "欲刪除之 BOM 識別碼不得為空");
		bomJpaRepository.deleteById(id.value());
	}

	@Override
	public boolean existsByParentMaterialIdAndVersion(MaterialId parentMaterialId, BomVersion version) {
		Objects.requireNonNull(parentMaterialId, "母件物料識別碼不得為空");
		Objects.requireNonNull(version, "BOM 版本號不得為空");
		return bomJpaRepository.existsByParentMaterialIdAndBomVersion(parentMaterialId.value(), version.value());
	}
}

