package com.example.demo.infrastructure.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.demo.application.port.out.MaterialRepositoryPort;
import com.example.demo.domain.material.aggregate.root.Material;
import com.example.demo.domain.material.aggregate.vo.MaterialCategory;
import com.example.demo.domain.material.aggregate.vo.MaterialCode;
import com.example.demo.domain.material.aggregate.vo.MaterialId;
import com.example.demo.infrastructure.persistence.material.entity.MaterialJpaEntity;
import com.example.demo.infrastructure.persistence.material.repository.MaterialJpaRepository;

/**
 * 物料儲存庫輸出適配器 (Material Repository Outbound Adapter)
 * <p>
 * 遵循六角形架構規範，實作應用層之 {@link MaterialRepositoryPort}，
 * 協調 Spring Data JPA {@link MaterialJpaRepository} 進行物料主檔持久化操作，
 * 確保領域層與應用層不受技術 ORM 框架侵入。
 * <p>
 * <b>事務職責劃分</b>：作為純粹的 Outbound Adapter，本類別不宣告 `@Transactional` 事務邊界。
 * 業務事務邊界與 Unit of Work 統一由 Application Service (CommandService / QueryService) 明確宣告管轄。
 */
@Component
public class MaterialRepositoryAdapter implements MaterialRepositoryPort {

	private final MaterialJpaRepository materialJpaRepository;

	/**
	 * 建構物料儲存庫適配器。
	 *
	 * @param materialJpaRepository 物料 Spring Data JPA 儲存庫
	 */
	public MaterialRepositoryAdapter(MaterialJpaRepository materialJpaRepository) {
		this.materialJpaRepository = Objects.requireNonNull(materialJpaRepository, "MaterialJpaRepository 不得為空");
	}

	@Override
	public Optional<Material> findById(MaterialId id) {
		Objects.requireNonNull(id, "物料識別碼不得為空");
		return materialJpaRepository.findById(id.value())
				.map(MaterialJpaEntity::toDomain);
	}

	@Override
	public Optional<Material> findByCode(MaterialCode code) {
		Objects.requireNonNull(code, "物料料號不得為空");
		return materialJpaRepository.findByMaterialCode(code.value())
				.map(MaterialJpaEntity::toDomain);
	}

	@Override
	public List<Material> findAll() {
		List<MaterialJpaEntity> entities = materialJpaRepository.findAll();
		List<Material> domains = new ArrayList<>(entities.size());
		for (MaterialJpaEntity entity : entities) {
			domains.add(entity.toDomain());
		}
		return domains;
	}

	@Override
	public List<Material> findByCategory(MaterialCategory category) {
		Objects.requireNonNull(category, "物料分類不得為空");
		List<MaterialJpaEntity> entities = materialJpaRepository.findByCategory(category);
		List<Material> domains = new ArrayList<>(entities.size());
		for (MaterialJpaEntity entity : entities) {
			domains.add(entity.toDomain());
		}
		return domains;
	}

	@Override
	public Material save(Material material) {
		Objects.requireNonNull(material, "待儲存之物料聚合根不得為空");
		MaterialJpaEntity entity = MaterialJpaEntity.fromDomain(material);
		MaterialJpaEntity saved = materialJpaRepository.save(entity);
		return saved.toDomain();
	}

	@Override
	public void deleteById(MaterialId id) {
		Objects.requireNonNull(id, "欲刪除之物料識別碼不得為空");
		materialJpaRepository.deleteById(id.value());
	}

	@Override
	public boolean existsByCode(MaterialCode code) {
		Objects.requireNonNull(code, "物料料號不得為空");
		return materialJpaRepository.existsByMaterialCode(code.value());
	}
}

