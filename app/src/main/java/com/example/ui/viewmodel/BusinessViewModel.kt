package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.db.AppDatabase
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.InventoryItem
import com.example.data.model.Sale
import com.example.data.repository.BusinessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BusinessViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "abraham_decor_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    private val repository: BusinessRepository by lazy {
        BusinessRepository(
            saleDao = database.saleDao(),
            inventoryDao = database.inventoryDao(),
            customerDao = database.customerDao(),
            expenseDao = database.expenseDao()
        )
    }

    // Expose Data Flows
    val sales: StateFlow<List<Sale>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val generalSales: StateFlow<List<Sale>> = repository.generalSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rubberSales: StateFlow<List<Sale>> = repository.rubberSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryItems: StateFlow<List<InventoryItem>> = repository.allInventoryItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockTransactions: StateFlow<List<com.example.data.model.StockTransaction>> = repository.allStockTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debtTransactions: StateFlow<List<com.example.data.model.DebtTransaction>> = repository.allDebtTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Feedback State
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    // --- ACTIONS ---

    // 1. Add Sale
    private fun playWarningSound() {
        try {
            val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100)
            toneGen.startTone(android.media.ToneGenerator.TONE_SUP_ERROR, 350)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addSale(
        itemName: String,
        quantity: Double,
        unit: String,
        unitPrice: Double,
        customerName: String?,
        isRubber: Boolean
    ) {
        viewModelScope.launch {
            val trimmedName = itemName.trim()
            // Auto Stock Deduction & Find Purchase Price (If matches inventory item name)
            val items = repository.allInventoryItems.first()
            val matchingItem = items.find { it.itemName.trim().equals(trimmedName, ignoreCase = true) }
            val determinedPurchasePrice = matchingItem?.purchasePrice ?: 0.0

            if (matchingItem != null) {
                // Check if selling quantity exceeds available warehouse stock
                if (quantity > matchingItem.quantity) {
                    playWarningSound()
                    _message.value = "ስህተት፡ የተሸጠው መጠን ($quantity) መጋዘን ውስጥ ካለው ክምችት (${matchingItem.quantity}) ይበልጣል! ማስገባት አይቻልም።"
                    return@launch
                }
                // Check if selling price is below purchase price
                if (unitPrice < matchingItem.purchasePrice) {
                    playWarningSound()
                    _message.value = "ስህተት፡ የተሸጠበት ዋጋ (${unitPrice} Br) እቃው ከተገዛበት ዋጋ (${matchingItem.purchasePrice} Br) በታች ነው! ማስገባት አይቻልም።"
                    return@launch
                }
            }

            val totalPrice = quantity * unitPrice
            val sale = Sale(
                itemName = trimmedName,
                quantity = quantity,
                unit = unit,
                unitPrice = unitPrice,
                purchasePrice = determinedPurchasePrice,
                totalPrice = totalPrice,
                customerName = if (customerName?.isNotBlank() == true) customerName else null,
                isRubber = isRubber
            )
            repository.insertSale(sale)

            if (matchingItem != null) {
                repository.adjustStock(
                    itemId = matchingItem.id,
                    type = "ወጭ",
                    quantity = quantity,
                    note = "በሽያጭ የተቀነሰ (${if (isRubber) "የጎማ ሽያጭ" else "የእለት ሽያጭ"})"
                )
                _message.value = "ሽያጭ ተመዝግቧል። የመጋዘን ክምችት በ ${quantity} $unit ቀንሷል።"
            } else {
                _message.value = "ሽያጭ በተሳካ ሁኔታ ተመዝግቧል"
            }

            // Auto Debt Creation if Customer specified and not paid fully
            // For now we assume if they select a customer name, we can ask if they want to put it as debt.
            // But we can also add a toggle or helper. Let's make it explicit via UI.
        }
    }

    fun deleteSale(sale: Sale) {
        viewModelScope.launch {
            repository.deleteSale(sale)
            _message.value = "ሽያጩ ተሰርዟል"
        }
    }

    // 2. Add/Adjust Warehouse Inventory
    fun addInventoryItem(itemName: String, quantity: Double, unit: String, minAlert: Double, purchasePrice: Double) {
        viewModelScope.launch {
            val items = repository.allInventoryItems.first()
            val exists = items.any { it.itemName.equals(itemName, ignoreCase = true) }
            if (exists) {
                _message.value = "ይህ ዕቃ ቀድሞውኑ በመጋዘን ውስጥ አለ!"
                return@launch
            }

            val newItem = InventoryItem(
                itemName = itemName,
                quantity = 0.0,
                unit = unit,
                minStockAlert = minAlert,
                purchasePrice = purchasePrice
            )
            val id = repository.insertInventoryItem(newItem)
            // Log transaction
            repository.adjustStock(id.toInt(), "ገቢ", quantity, "መጋዘን መጀመሪያ ሲመዘገብ የነበረ ክምችት")
            _message.value = "አዲስ ዕቃ መጋዘን ውስጥ ተመዝግቧል"
        }
    }

    fun adjustStock(itemId: Int, type: String, quantity: Double, note: String?) {
        viewModelScope.launch {
            repository.adjustStock(itemId, type, quantity, note)
            _message.value = "ክምችት ማስተካከያ ተመዝግቧል (${if (type == "ገቢ") "ገቢ" else "ወጭ"})"
        }
    }

    fun deleteInventoryItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.deleteInventoryItem(item)
            _message.value = "ዕቃው ከመጋዘን ተሰርዟል"
        }
    }

    // 3. Customer & Debt Management
    fun addCustomer(name: String, phone: String?, initialDebt: Double) {
        viewModelScope.launch {
            val customer = Customer(
                name = name,
                phone = if (phone?.isNotBlank() == true) phone else null,
                totalDebt = 0.0 // Managed through transactions
            )
            val customerId = repository.insertCustomer(customer)
            if (initialDebt > 0) {
                repository.recordDebtChange(customerId.toInt(), "ዕዳ", initialDebt, "የመጀመሪያ ዕዳ")
            }
            _message.value = "አዲስ ደንበኛ ተመዝግቧል"
        }
    }

    fun recordDebtChange(customerId: Int, type: String, amount: Double, note: String?) {
        viewModelScope.launch {
            repository.recordDebtChange(customerId, type, amount, note)
            _message.value = if (type == "ዕዳ") "አዲስ ዕዳ ተመዝግቧል" else "ክፍያ ተመዝግቧል"
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            _message.value = "ደንበኛ ተሰርዟል"
        }
    }

    // 4. Daily Expenses
    fun addExpense(title: String, category: String, amount: Double) {
        viewModelScope.launch {
            val expense = Expense(
                title = title,
                category = category,
                amount = amount
            )
            repository.insertExpense(expense)
            _message.value = "ወጪ ተመዝግቧል"
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _message.value = "ወጪው ተሰርዟል"
        }
    }
}

class BusinessViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BusinessViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BusinessViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
