package com.example.data.repository

import com.example.data.db.SaleDao
import com.example.data.db.InventoryDao
import com.example.data.db.CustomerDao
import com.example.data.db.ExpenseDao
import com.example.data.model.Sale
import com.example.data.model.InventoryItem
import com.example.data.model.StockTransaction
import com.example.data.model.Customer
import com.example.data.model.DebtTransaction
import com.example.data.model.Expense
import kotlinx.coroutines.flow.Flow

class BusinessRepository(
    private val saleDao: SaleDao,
    private val inventoryDao: InventoryDao,
    private val customerDao: CustomerDao,
    private val expenseDao: ExpenseDao
) {
    val allSales: Flow<List<Sale>> = saleDao.getAllSales()
    val rubberSales: Flow<List<Sale>> = saleDao.getSalesByRubber(true)
    val generalSales: Flow<List<Sale>> = saleDao.getSalesByRubber(false)
    val allInventoryItems: Flow<List<InventoryItem>> = inventoryDao.getAllInventoryItems()
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val allStockTransactions: Flow<List<StockTransaction>> = inventoryDao.getAllStockTransactions()
    val allDebtTransactions: Flow<List<DebtTransaction>> = customerDao.getAllDebtTransactions()

    suspend fun insertSale(sale: Sale) {
        saleDao.insertSale(sale)
    }

    suspend fun deleteSale(sale: Sale) {
        saleDao.deleteSale(sale)
    }

    fun getSalesBetween(start: Long, end: Long): Flow<List<Sale>> {
        return saleDao.getSalesBetween(start, end)
    }

    suspend fun insertInventoryItem(item: InventoryItem): Long {
        return inventoryDao.insertInventoryItem(item)
    }

    suspend fun updateInventoryItem(item: InventoryItem) {
        inventoryDao.updateInventoryItem(item)
    }

    suspend fun deleteInventoryItem(item: InventoryItem) {
        inventoryDao.deleteInventoryItem(item)
    }

    suspend fun adjustStock(itemId: Int, type: String, quantity: Double, note: String?) {
        inventoryDao.adjustStock(itemId, type, quantity, note)
    }

    fun getStockTransactions(itemId: Int): Flow<List<StockTransaction>> {
        return inventoryDao.getStockTransactions(itemId)
    }

    suspend fun insertCustomer(customer: Customer): Long {
        return customerDao.insertCustomer(customer)
    }

    suspend fun updateCustomer(customer: Customer) {
        customerDao.updateCustomer(customer)
    }

    suspend fun deleteCustomer(customer: Customer) {
        customerDao.deleteCustomer(customer)
    }

    suspend fun recordDebtChange(customerId: Int, type: String, amount: Double, note: String?) {
        customerDao.recordDebtChange(customerId, type, amount, note)
    }

    fun getDebtTransactionsByCustomer(customerId: Int): Flow<List<DebtTransaction>> {
        return customerDao.getDebtTransactionsByCustomer(customerId)
    }

    suspend fun insertExpense(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    fun getExpensesBetween(start: Long, end: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesBetween(start, end)
    }
}
