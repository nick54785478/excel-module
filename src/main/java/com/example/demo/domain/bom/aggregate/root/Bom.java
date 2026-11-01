package com.example.demo.domain.bom.aggregate.root;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import com.example.demo.domain.bom.aggregate.entity.BomItem;
import com.example.demo.domain.bom.aggregate.vo.BomId;
import com.example.demo.domain.bom.aggregate.vo.BomItemId;
import com.example.demo.domain.bom.aggregate.vo.BomStatus;
import com.example.demo.domain.bom.aggregate.vo.BomVersion;
import com.example.demo.domain.bom.aggregate.vo.ItemSequence;
import com.example.demo.domain.bom.aggregate.vo.Quantity;
import com.example.demo.domain.bom.aggregate.vo.ScrapRate;
import com.example.demo.domain.bom.event.BomApprovedEvent;
import com.example.demo.domain.bom.event.BomCreatedEvent;
import com.example.demo.domain.bom.event.BomItemAddedEvent;
import com.example.demo.domain.bom.event.BomItemRemovedEvent;
import com.example.demo.domain.bom.exception.CircularBomReferenceException;
import com.example.demo.domain.bom.exception.DuplicateBomItemException;
import com.example.demo.domain.bom.exception.IllegalBomOperationException;
import com.example.demo.domain.bom.exception.InvalidBomParentCategoryException;
import com.example.demo.domain.material.aggregate.vo.MaterialCategory;
import com.example.demo.domain.material.aggregate.vo.MaterialId;

/**
 * 物料清單聚合根 (BOM Aggregate Root)
 * <p>
 * 遵循 DDD (領域驅動設計) 規範，代表產品結構樹（Bill of Materials）的核心聚合根。
 * <p>
 * 負責維護母件物料與各階層子料件之組成關係，保護以下商業不變性（Domain Invariants）：
 * <ul>
 *     <li><b>母件分類限制</b>：只有成品 (FG) 與半成品 (SFG) 允許建立 BOM 配方，原物料 (RM) 嚴禁作為母件。</li>
 *     <li><b>防循環引用</b>：嚴禁母件將自己加入子件清單，亦嚴禁間接遞迴包含自身。</li>
 *     <li><b>子件唯一性</b>：同一個 BOM 結構內子料件不得重複。</li>
 *     <li><b>生命週期不可變性</b>：一旦經工程審核通過 (APPROVED)，結構立即凍結，不可再新增或刪除子件。</li>
 * </ul>
 * 內部嚴格禁止使用 Lombok 註解，完全由純 Java 行為方法確保封裝。
 */
public class Bom {

	private final BomId id;
	private final MaterialId parentMaterialId;
	private final BomVersion version;
	private String description;
	private Quantity baseQuantity;
	private BomStatus status;
	private final List<BomItem> items = new ArrayList<>();

	/**
	 * 暫存待發布之領域事件清單
	 */
	private final List<Object> domainEvents = new ArrayList<>();

	/**
	 * 私有建構子，封裝建構細節。
	 */
	private Bom(
			BomId id,
			MaterialId parentMaterialId,
			BomVersion version,
			String description,
			Quantity baseQuantity,
			BomStatus status,
			List<BomItem> initialItems
	) {
		this.id = Objects.requireNonNull(id, "BOM 識別碼不得為空");
		this.parentMaterialId = Objects.requireNonNull(parentMaterialId, "母件物料識別碼不得為空");
		this.version = Objects.requireNonNull(version, "BOM 版本號不得為空");
		this.description = description != null ? description.trim() : "";
		this.baseQuantity = baseQuantity != null ? baseQuantity : Quantity.ONE;
		this.status = Objects.requireNonNull(status, "BOM 狀態不得為空");
		if (initialItems != null) {
			this.items.addAll(initialItems);
		}
	}

	/**
	 * 建立新 BOM 配方清單之靜態工廠方法。
	 *
	 * @param parentMaterialId 母件物料識別碼
	 * @param parentCategory 母件物料分類
	 * @param version BOM 版本號
	 * @param description 配方說明描述
	 * @param baseQuantity 母件基準數量 (預設為 1)
	 * @return 初始建立之草稿狀態 BOM 聚合根
	 * @throws InvalidBomParentCategoryException 當母件分類為原物料時拋出
	 */
	public static Bom create(
			MaterialId parentMaterialId,
			MaterialCategory parentCategory,
			BomVersion version,
			String description,
			Quantity baseQuantity
	) {
		Objects.requireNonNull(parentCategory, "母件物料分類不得為空");
		if (!parentCategory.canHaveBom()) {
			throw new InvalidBomParentCategoryException(
					"物料分類 [" + parentCategory.getDisplayName() + "] 屬於不可拆解之原物料，嚴禁作為母件建立 BOM 清單"
			);
		}

		BomId id = BomId.generate();
		BomVersion effectiveVersion = version != null ? version : BomVersion.DEFAULT;
		Quantity effectiveBaseQty = baseQuantity != null ? baseQuantity : Quantity.ONE;

		Bom bom = new Bom(
				id,
				parentMaterialId,
				effectiveVersion,
				description,
				effectiveBaseQty,
				BomStatus.DRAFT,
				Collections.emptyList()
		);
		bom.recordEvent(new BomCreatedEvent(id, parentMaterialId, effectiveVersion));
		return bom;
	}

	/**
	 * 從持久化設施重建既有 BOM 聚合根 (Reconstitution Factory)。
	 *
	 * @param id BOM 識別碼
	 * @param parentMaterialId 母件物料識別碼
	 * @param version 版本號
	 * @param description 說明
	 * @param baseQuantity 母件基數
	 * @param status 狀態
	 * @param items 子項清單
	 * @return 重建之 BOM 聚合根
	 */
	public static Bom reconstitute(
			BomId id,
			MaterialId parentMaterialId,
			BomVersion version,
			String description,
			Quantity baseQuantity,
			BomStatus status,
			List<BomItem> items
	) {
		return new Bom(id, parentMaterialId, version, description, baseQuantity, status, items);
	}

	/**
	 * 新增子件項目至物料清單。
	 *
	 * @param childMaterialId 子件物料識別碼
	 * @param quantity 單機用量
	 * @param scrapRate 損耗率
	 * @param positionCode 安裝位置代碼
	 * @param remark 備註說明
	 * @return 新增之 BOM 明細實體
	 * @throws IllegalBomOperationException 當 BOM 非草稿狀態時拋出
	 * @throws CircularBomReferenceException 當子件為母件本身時拋出
	 * @throws DuplicateBomItemException 當子件已存在時拋出
	 */
	public BomItem addItem(
			MaterialId childMaterialId,
			Quantity quantity,
			ScrapRate scrapRate,
			String positionCode,
			String remark
	) {
		assertDraftStatus("新增子料件");
		Objects.requireNonNull(childMaterialId, "子件物料識別碼不得為空");

		if (childMaterialId.equals(this.parentMaterialId)) {
			throw new CircularBomReferenceException(
					"母件物料 [" + this.parentMaterialId + "] 不可作為自身 BOM 之子項，防止直接循環引用"
			);
		}

		if (containsChildMaterial(childMaterialId)) {
			throw new DuplicateBomItemException("子料件 [" + childMaterialId + "] 已存在於此 BOM 清單中，不得重複加入");
		}

		ItemSequence nextSequence = calculateNextSequence();
		BomItemId itemId = BomItemId.generate();
		BomItem item = new BomItem(
				itemId,
				childMaterialId,
				nextSequence,
				quantity,
				scrapRate,
				positionCode,
				remark
		);

		this.items.add(item);
		recordEvent(new BomItemAddedEvent(this.id, itemId, childMaterialId, quantity));
		return item;
	}

	/**
	 * 自清單中移除指定子項。
	 *
	 * @param itemId 子項識別碼
	 * @throws IllegalBomOperationException 當 BOM 非草稿狀態時拋出
	 */
	public void removeItem(BomItemId itemId) {
		assertDraftStatus("移除子料件");
		Objects.requireNonNull(itemId, "欲移除之子項識別碼不得為空");

		BomItem target = null;
		for (BomItem item : this.items) {
			if (item.getId().equals(itemId)) {
				target = item;
				break;
			}
		}

		if (target != null) {
			this.items.remove(target);
			recordEvent(new BomItemRemovedEvent(this.id, target.getId(), target.getChildMaterialId()));
		}
	}

	/**
	 * 審核通過並生效 BOM 清單。
	 * <p>
	 * 審核通過後狀態變更為 {@link BomStatus#APPROVED}，進入凍結狀態，發布 {@link BomApprovedEvent}。
	 *
	 * @throws IllegalBomOperationException 當 BOM 非草稿或清單為空時拋出
	 */
	public void approve() {
		assertDraftStatus("審核生效");
		if (this.items.isEmpty()) {
			throw new IllegalBomOperationException("不得審核未包含任何子項零件之空 BOM 清單");
		}
		this.status = BomStatus.APPROVED;
		recordEvent(new BomApprovedEvent(this.id, this.parentMaterialId, this.version));
	}

	/**
	 * 作廢此 BOM 配方清單。
	 */
	public void obsolete() {
		this.status = BomStatus.OBSOLETE;
	}

	/**
	 * 遞迴驗證是否存在多階循環引用 (Circular Reference Check)。
	 *
	 * @param ancestorMaterialIds 所有上層祖先母件識別碼集合
	 * @throws CircularBomReferenceException 當檢測到任何子項存在於祖先集合中時拋出
	 */
	public void checkCircularReference(Set<MaterialId> ancestorMaterialIds) {
		if (ancestorMaterialIds == null || ancestorMaterialIds.isEmpty()) {
			return;
		}

		if (ancestorMaterialIds.contains(this.parentMaterialId)) {
			throw new CircularBomReferenceException(
					"檢測到 BOM 循環引用：母件 [" + this.parentMaterialId + "] 已存在於上層祖先節點鏈中"
			);
		}

		for (BomItem item : this.items) {
			if (ancestorMaterialIds.contains(item.getChildMaterialId())) {
				throw new CircularBomReferenceException(
						"檢測到 BOM 循環引用：子件 [" + item.getChildMaterialId() + "] 存在於上層祖先節點鏈中"
				);
			}
		}
	}

	/**
	 * 檢查是否已包含指定之子料件。
	 *
	 * @param childMaterialId 子件識別碼
	 * @return 若已存在回傳 {@code true}
	 */
	public boolean containsChildMaterial(MaterialId childMaterialId) {
		for (BomItem item : this.items) {
			if (item.getChildMaterialId().equals(childMaterialId)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 取得所有未發布之領域事件（唯讀檢視）。
	 *
	 * @return 領域事件不可變清單
	 */
	public List<Object> getDomainEvents() {
		return Collections.unmodifiableList(this.domainEvents);
	}

	/**
	 * 清除已收集之領域事件。
	 */
	public void clearDomainEvents() {
		this.domainEvents.clear();
	}

	protected void recordEvent(Object event) {
		if (event != null) {
			this.domainEvents.add(event);
		}
	}

	private void assertDraftStatus(String operationName) {
		if (this.status != BomStatus.DRAFT) {
			throw new IllegalBomOperationException(
					"當前 BOM 狀態為 [" + this.status.getDisplayName() + "]，不允許執行 [" + operationName + "] 操作"
			);
		}
	}

	private ItemSequence calculateNextSequence() {
		if (this.items.isEmpty()) {
			return ItemSequence.initial();
		}
		int maxSequence = 0;
		for (BomItem item : this.items) {
			if (item.getItemSequence().value() > maxSequence) {
				maxSequence = item.getItemSequence().value();
			}
		}
		return ItemSequence.of(maxSequence + ItemSequence.DEFAULT_STEP);
	}

	public BomId getId() {
		return id;
	}

	public MaterialId getParentMaterialId() {
		return parentMaterialId;
	}

	public BomVersion getVersion() {
		return version;
	}

	public String getDescription() {
		return description;
	}

	public Quantity getBaseQuantity() {
		return baseQuantity;
	}

	public BomStatus getStatus() {
		return status;
	}

	/**
	 * 取得子件清單之不可變唯讀視圖。
	 *
	 * @return 子項清單
	 */
	public List<BomItem> getItems() {
		return Collections.unmodifiableList(items);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Bom bom = (Bom) o;
		return Objects.equals(id, bom.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "Bom{" +
				"id=" + id +
				", parentMaterialId=" + parentMaterialId +
				", version=" + version +
				", status=" + status +
				", itemsCount=" + items.size() +
				'}';
	}
}

