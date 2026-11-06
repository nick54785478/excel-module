package com.example.demo.presentation.assembler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.demo.application.shared.model.OrderModel;
import com.example.demo.application.shared.model.ProductRecordModel;
import com.example.demo.presentation.resource.in.ExportOrderResource;
import com.example.demo.presentation.resource.in.ExportOrderResource.OrderItemResource;
import com.example.demo.presentation.resource.in.ExportProductResource;
import com.example.demo.presentation.resource.in.ExportProductResource.ProductItemResource;
import com.example.demo.presentation.resource.out.OrderImportedResource;
import com.example.demo.presentation.resource.out.OrderImportedResource.OrderRetrievedResource;
import com.example.demo.presentation.resource.out.ProductImportedResource;
import com.example.demo.presentation.resource.out.ProductImportedResource.ProductRetrievedResource;

/**
 * 宣告式物件映射表現層資源組裝器 (Presentation Resource Assembler)
 * <p>
 * 遵循 Clean Architecture 職責，扮演表現層防腐層 (Anti-Corruption Layer, ACL)。
 * 負責 Request Resource 與 Application Model 之間的互相轉換，確保 Resource 與 Model 維持純資料載體。
 */
@Component
public class ExcelBeanMappingResourceAssembler {

	/**
	 * 將訂單匯出請求資源轉換為 {@link OrderModel} 清單
	 *
	 * @param resource 訂單匯出請求資源
	 * @return 訂單模型清單
	 */
	public List<OrderModel> toOrderModels(ExportOrderResource resource) {
		if (resource == null || resource.orders() == null || resource.orders().isEmpty()) {
			return Collections.emptyList();
		}

		List<OrderModel> models = new ArrayList<>(resource.orders().size());
		for (OrderItemResource item : resource.orders()) {
			if (item != null) {
				models.add(new OrderModel(
						item.orderNo(),
						item.customerName(),
						item.amount(),
						item.quantity(),
						item.orderDate(),
						item.status()
				));
			}
		}
		return models;
	}

	/**
	 * 將已解析的 {@link OrderModel} 清單轉換為回應資源 {@link OrderImportedResource}
	 *
	 * @param models    訂單模型清單
	 * @param sheetName 工作表名稱
	 * @return 訂單匯入回應資源
	 */
	public OrderImportedResource toOrderImportedResource(List<OrderModel> models, String sheetName) {
		if (models == null || models.isEmpty()) {
			return new OrderImportedResource(0, sheetName != null ? sheetName : "訂單匯入清單", Collections.emptyList());
		}

		List<OrderRetrievedResource> items = new ArrayList<>(models.size());
		for (OrderModel model : models) {
			if (model != null) {
				items.add(new OrderRetrievedResource(
						model.getOrderNo(),
						model.getCustomerName(),
						model.getAmount(),
						model.getQuantity(),
						model.getOrderDate(),
						model.getStatus()
				));
			}
		}
		return new OrderImportedResource(items.size(), sheetName != null ? sheetName : "訂單匯入清單", items);
	}

	/**
	 * 將商品匯出請求資源轉換為 {@link ProductRecordModel} 清單
	 *
	 * @param resource 商品匯出請求資源
	 * @return 商品 Record 模型清單
	 */
	public List<ProductRecordModel> toProductModels(ExportProductResource resource) {
		if (resource == null || resource.products() == null || resource.products().isEmpty()) {
			return Collections.emptyList();
		}

		List<ProductRecordModel> models = new ArrayList<>(resource.products().size());
		for (ProductItemResource item : resource.products()) {
			if (item != null) {
				models.add(new ProductRecordModel(
						item.productCode(),
						item.productName(),
						item.price(),
						item.onSale()
				));
			}
		}
		return models;
	}

	/**
	 * 將已解析的 {@link ProductRecordModel} 清單轉換為回應資源 {@link ProductImportedResource}
	 *
	 * @param models    商品模型清單
	 * @param sheetName 工作表名稱
	 * @return 商品匯入回應資源
	 */
	public ProductImportedResource toProductImportedResource(List<ProductRecordModel> models, String sheetName) {
		if (models == null || models.isEmpty()) {
			return new ProductImportedResource(0, sheetName != null ? sheetName : "商品主檔清單", Collections.emptyList());
		}

		List<ProductRetrievedResource> items = new ArrayList<>(models.size());
		for (ProductRecordModel model : models) {
			if (model != null) {
				items.add(new ProductRetrievedResource(
						model.productCode(),
						model.productName(),
						model.price(),
						model.onSale()
				));
			}
		}
		return new ProductImportedResource(items.size(), sheetName != null ? sheetName : "商品主檔清單", items);
	}
}
