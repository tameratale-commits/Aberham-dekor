package com.example.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.Sale
import com.example.data.model.InventoryItem
import com.example.data.model.StockTransaction
import com.example.data.model.Customer
import com.example.data.model.DebtTransaction
import com.example.data.model.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE isRubber = :isRubber ORDER BY timestamp DESC")
    fun getSalesByRubber(isRubber: Boolean): Flow<List<Sale>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale)

    @Delete
    suspend fun deleteSale(sale: Sale)

    @Query("SELECT * FROM sales WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getSalesBetween(start: Long, end: Long): Flow<List<Sale>>
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY itemName ASC")
    fun getAllInventoryItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getInventoryItemById(id: Int): InventoryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItem): Long

    @Update
    suspend fun updateInventoryItem(item: InventoryItem)

    @Delete
    suspend fun deleteInventoryItem(item: InventoryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockTransaction(tx: StockTransaction)

    @Query("SELECT * FROM stock_transactions WHERE itemId = :itemId ORDER BY timestamp DESC")
    fun getStockTransactions(itemId: Int): Flow<List<StockTransaction>>

    @Query("SELECT * FROM stock_transactions ORDER BY timestamp DESC")
    fun getAllStockTransactions(): Flow<List<StockTransaction>>

    // Combined helper to perform stock adjustment and log the transaction
    @Transaction
    suspend fun adjustStock(itemId: Int, type: String, quantity: Double, note: String?) {
        val item = getInventoryItemById(itemId) ?: return
        val newQty = if (type == "ገቢ") {
            item.quantity + quantity
        } else {
            maxOf(0.0, item.quantity - quantity)
        }
        updateInventoryItem(item.copy(quantity = newQty))
        insertStockTransaction(StockTransaction(itemId = itemId, type = type, quantity = quantity, note = note))
    }
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Int): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtTransaction(tx: DebtTransaction)

    @Query("SELECT * FROM debt_transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getDebtTransactionsByCustomer(customerId: Int): Flow<List<DebtTransaction>>

    @Query("SELECT * FROM debt_transactions ORDER BY timestamp DESC")
    fun getAllDebtTransactions(): Flow<List<DebtTransaction>>

    @Transaction
    suspend fun recordDebtChange(customerId: Int, type: String, amount: Double, note: String?) {
        val customer = getCustomerById(customerId) ?: return
        val newDebt = if (type == "ዕዳ") {
            customer.totalDebt + amount
        } else {
            maxOf(0.0, customer.totalDebt - amount)
        }
        updateCustomer(customer.copy(totalDebt = newDebt))
        insertDebtTransaction(DebtTransaction(customerId = customerId, type = type, amount = amount, note = note))
    }
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getExpensesBetween(start: Long, end: Long): Flow<List<Expense>>
}

@Database(
    entities = [
        Sale::class,
        InventoryItem::class,
        StockTransaction::class,
        Customer::class,
        DebtTransaction::class,
        Expense::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun saleDao(): SaleDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun customerDao(): CustomerDao
    abstract fun expenseDao(): ExpenseDao
}
