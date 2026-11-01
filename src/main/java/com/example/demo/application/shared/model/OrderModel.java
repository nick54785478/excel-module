package com.example.demo.application.shared.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import com.example.demo.application.annotation.ExcelColumn;
import com.example.demo.application.annotation.ExcelSheet;

/**
 * 訂單展示模型 (Standard JavaBean Demo Model)
 * <p>
 * 遵循維度一「宣告式物件映射」設計，以註解標註工作表與各欄位屬性。
 * 用於展示標準 JavaBean（包含無參建構子、全參建構子與 Getter/Setter）的雙向 Excel 解析與匯出。
 */
@ExcelSheet(name = "訂單匯入清單")
public class OrderModel {

	@ExcelColumn(name = "訂單編號", index = 0)
	private String orderNo;

	@ExcelColumn(name = "訂購人", index = 1, defaultValue = "匿名訪客")
	private String customerName;

	@ExcelColumn(name = "購買金額", index = 2, format = "#,##0.00")
	private BigDecimal amount;

	@ExcelColumn(name = "數量", index = 3)
	private Integer quantity;

	@ExcelColumn(name = "下單日期", index = 4, format = "yyyy-MM-dd")
	private LocalDate orderDate;

	@ExcelColumn(name = "訂單狀態", index = 5)
	private String status;

	public OrderModel() {
	}

	public OrderModel(String orderNo, String customerName, BigDecimal amount, Integer quantity, LocalDate orderDate, String status) {
		this.orderNo = orderNo;
		this.customerName = customerName;
		this.amount = amount;
		this.quantity = quantity;
		this.orderDate = orderDate;
		this.status = status;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public LocalDate getOrderDate() {
		return orderDate;
	}

	public void setOrderDate(LocalDate orderDate) {
		this.orderDate = orderDate;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		OrderModel that = (OrderModel) o;
		return Objects.equals(orderNo, that.orderNo) &&
				Objects.equals(customerName, that.customerName) &&
				Objects.equals(amount, that.amount) &&
				Objects.equals(quantity, that.quantity) &&
				Objects.equals(orderDate, that.orderDate) &&
				Objects.equals(status, that.status);
	}

	@Override
	public int hashCode() {
		return Objects.hash(orderNo, customerName, amount, quantity, orderDate, status);
	}

	@Override
	public String toString() {
		return "OrderModel{" +
				"orderNo='" + orderNo + '\'' +
				", customerName='" + customerName + '\'' +
				", amount=" + amount +
				", quantity=" + quantity +
				", orderDate=" + orderDate +
				", status='" + status + '\'' +
				'}';
	}
}

