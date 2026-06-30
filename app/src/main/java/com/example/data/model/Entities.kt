package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val itemName: String,
    val quantity: Double,
    val unit: String, // "ሜትር" (Meter), "ቁጥር" (Piece/Count), "ስራ" (Service)
    val unitPrice: Double,
    val totalPrice: Double,
    val customerName: String? = null,
    val isRubber: Boolean = false,
    val purchasePrice: Double = 0.0
)

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val itemName: String,
    val quantity: Double, // Stock level
    val unit: String, // "ሜትር", "ቁጥር"
    val minStockAlert: Double = 5.0,
    val purchasePrice: Double = 0.0
)

@Entity(tableName = "stock_transactions")
data class StockTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "ገቢ" (Stock In) or "ወጭ" (Stock Out)
    val quantity: Double,
    val note: String? = null
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String? = null,
    val totalDebt: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "debt_transactions")
data class DebtTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "ዕዳ" (Incurred Debt) or "ክፍያ" (Payment Received)
    val amount: Double,
    val note: String? = null
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val category: String, // "የቤት ኪራይ", "ደመወዝ", "ዕቃዎች መግዣ", "መብራት/ውሃ", "ምግብ/መስተንግዶ", "ሌላ ወጭ"
    val amount: Double
)
