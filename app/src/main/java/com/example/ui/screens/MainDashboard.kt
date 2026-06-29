package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Customer
import com.example.data.model.DebtTransaction
import com.example.data.model.Expense
import com.example.data.model.InventoryItem
import com.example.data.model.Sale
import com.example.data.model.StockTransaction
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AccentOrange
import com.example.ui.viewmodel.BusinessViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(viewModel: BusinessViewModel) {
    // Tab States
    var selectedTab by remember { mutableStateOf(0) }
    val tabNames = listOf(
        "እለት ሽያጭ" to Icons.Default.ShoppingCart,
        "መጋዘን" to Icons.Default.Layers,
        "ደንበኛ ዕዳ" to Icons.Default.People,
        "የጎማ ሽያጭ" to Icons.Default.DirectionsCar,
        "እለት ወጪ" to Icons.Default.AccountBalanceWallet
    )

    // Data from StateFlow
    val sales by viewModel.sales.collectAsStateWithLifecycle()
    val generalSales by viewModel.generalSales.collectAsStateWithLifecycle()
    val rubberSales by viewModel.rubberSales.collectAsStateWithLifecycle()
    val inventoryItems by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val stockTransactions by viewModel.stockTransactions.collectAsStateWithLifecycle()
    val debtTransactions by viewModel.debtTransactions.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    // Snackbar Host
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Auto show message from ViewModel
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Calculations for Today's Stats Card
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val todaySalesTotal = sales.filter { it.timestamp >= todayStart }.sumOf { it.totalPrice }
    val todayExpensesTotal = expenses.filter { it.timestamp >= todayStart }.sumOf { it.amount }
    val totalDebtOutstanding = customers.sumOf { it.totalDebt }
    val lowStockCount = inventoryItems.filter { it.quantity <= it.minStockAlert }.size

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "አብርሃም የመኪና ዲኮር",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Abraham Car Decor & Accessories",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    if (lowStockCount > 0) {
                        Badge(
                            containerColor = AccentRed,
                            contentColor = Color.White,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text("$lowStockCount ዕቃዎች ክምችት አልቋል!", modifier = Modifier.padding(4.dp), fontSize = 10.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                tabNames.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = {
                            Text(
                                label,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.testTag("tab_button_$index")
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Summary Cards Block (Universal Header)
            SummaryDashboardRow(
                todaySales = todaySalesTotal,
                todayExpenses = todayExpensesTotal,
                outstandingDebt = totalDebtOutstanding,
                lowStockCount = lowStockCount
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Switch Screen Contents Based on Selected Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> DailySalesScreen(
                        sales = generalSales,
                        inventory = inventoryItems,
                        onAddSale = { name, qty, unit, price, cust ->
                            viewModel.addSale(name, qty, unit, price, cust, isRubber = false)
                        },
                        onDeleteSale = { viewModel.deleteSale(it) }
                    )
                    1 -> InventoryScreen(
                        items = inventoryItems,
                        transactions = stockTransactions,
                        onAddItem = { name, qty, unit, alert ->
                            viewModel.addInventoryItem(name, qty, unit, alert)
                        },
                        onAdjustStock = { id, type, qty, note ->
                            viewModel.adjustStock(id, type, qty, note)
                        },
                        onDeleteItem = { viewModel.deleteInventoryItem(it) }
                    )
                    2 -> CustomerDebtScreen(
                        customers = customers,
                        transactions = debtTransactions,
                        onAddCustomer = { name, phone, initialDebt ->
                            viewModel.addCustomer(name, phone, initialDebt)
                        },
                        onAdjustDebt = { id, type, amount, note ->
                            viewModel.recordDebtChange(id, type, amount, note)
                        },
                        onDeleteCustomer = { viewModel.deleteCustomer(it) }
                    )
                    3 -> RubberSalesScreen(
                        sales = rubberSales,
                        inventory = inventoryItems,
                        onAddRubberSale = { name, qty, price, cust ->
                            viewModel.addSale(name, qty, "ሜትር", price, cust, isRubber = true)
                        },
                        onDeleteSale = { viewModel.deleteSale(it) }
                    )
                    4 -> ExpensesScreen(
                        expenses = expenses,
                        onAddExpense = { title, category, amount ->
                            viewModel.addExpense(title, category, amount)
                        },
                        onDeleteExpense = { viewModel.deleteExpense(it) }
                    )
                }
            }
        }
    }
}

// --- UNIVERSAL SUMMARY HEADER COMPONENT ---
@Composable
fun SummaryDashboardRow(
    todaySales: Double,
    todayExpenses: Double,
    outstandingDebt: Double,
    lowStockCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sales Card
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text("የዛሬ ሸያጭ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatCurrency(todaySales)} Br",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen
                )
            }
        }

        // Expenses Card
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text("የዛሬ ወጪ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatCurrency(todayExpenses)} Br",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (todayExpenses > 0) AccentRed else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Debt Card
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text("አጠቃላይ ዕዳ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatCurrency(outstandingDebt)} Br",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (outstandingDebt > 0) AccentOrange else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}


// ==========================================
// 1. DAILY SALES TAB (የእለት ሸያጭ)
// ==========================================
@Composable
fun DailySalesScreen(
    sales: List<Sale>,
    inventory: List<InventoryItem>,
    onAddSale: (String, Double, String, Double, String?) -> Unit,
    onDeleteSale: (Sale) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("የእለት ሽያጭ መዝገብ", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Daily Accessories & Labor Sales", fontSize = 12.sp, color = Color.Gray)
                }

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_sale_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ሽያጭ መዝግብ", fontSize = 13.sp)
                }
            }

            // Linkage notice banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "የሽያጭ ዕቃ ስም በመጋዘን ካለው ጋር ሲመሳሰል ክምችት በራሱ ይቀንሳል!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }

            if (sales.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.ShoppingCart,
                    title = "ምንም የተመዘገበ ሽያጭ የለም",
                    description = "ከላይ 'ሽያጭ መዝግብ' በመንካት የእለት ሽያጭ መመዝገብ ይችላሉ።"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sales) { sale ->
                        SaleCard(sale = sale, onDelete = { onDeleteSale(sale) })
                    }
                }
            }
        }

        if (showAddDialog) {
            AddSaleDialog(
                inventory = inventory,
                isRubberOnly = false,
                onDismiss = { showAddDialog = false },
                onConfirm = { name, qty, unit, price, customer ->
                    onAddSale(name, qty, unit, price, customer)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun SaleCard(sale: Sale, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sale.itemName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (sale.isRubber) {
                        Spacer(modifier = Modifier.width(6.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text("ጎማ", fontSize = 9.sp) },
                            modifier = Modifier.height(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "መጠን: ${formatQty(sale.quantity)} ${sale.unit}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "ዋጋ: ${formatCurrency(sale.unitPrice)} Br",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                if (sale.customerName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "ደንበኛ: ${sale.customerName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatDate(sale.timestamp),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatCurrency(sale.totalPrice)} Br",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = AccentGreen
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Sale",
                        tint = AccentRed.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}


// ==========================================
// 2. WAREHOUSE STOCK TAB (የመጋዘን ወጭ ገቢ)
// ==========================================
@Composable
fun InventoryScreen(
    items: List<InventoryItem>,
    transactions: List<StockTransaction>,
    onAddItem: (String, Double, String, Double) -> Unit,
    onAdjustStock: (Int, String, Double, String?) -> Unit,
    onDeleteItem: (InventoryItem) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedItemForAdjust by remember { mutableStateOf<InventoryItem?>(null) }
    var adjustType by remember { mutableStateOf("ገቢ") } // "ገቢ" (In) or "ወጭ" (Out)
    
    // Sub-tab for inventory list vs history
    var subTabSelected by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Sub-tabs Row
        TabRow(
            selectedTabIndex = subTabSelected,
            containerColor = Color.Transparent,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Tab(
                selected = subTabSelected == 0,
                onClick = { subTabSelected = 0 },
                text = { Text("የመጋዘን ክምችት", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = subTabSelected == 1,
                onClick = { subTabSelected = 1 },
                text = { Text("የክምችት ታሪክ", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            )
        }

        if (subTabSelected == 0) {
            // Inventory Stock List
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ዕቃ ፈልግ...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_inventory_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("አዲስ ዕቃ", fontSize = 13.sp)
                }
            }

            val filteredItems = items.filter {
                it.itemName.contains(searchQuery, ignoreCase = true)
            }

            if (filteredItems.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Layers,
                    title = "ምንም ዕቃ አልተገኘም",
                    description = searchQuery.let {
                        if (it.isEmpty()) "በመጋዘን ውስጥ ምንም ዕቃ አልተመዘገበም። 'አዲስ ዕቃ' የሚለውን ቁልፍ በመጫን ይመዝግቡ።"
                        else "ለ '$it' ፍለጋ ምንም ውጤት አልተገኘም።"
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems) { item ->
                        InventoryCard(
                            item = item,
                            onStockIn = {
                                selectedItemForAdjust = item
                                adjustType = "ገቢ"
                            },
                            onStockOut = {
                                selectedItemForAdjust = item
                                adjustType = "ወጭ"
                            },
                            onDelete = { onDeleteItem(item) }
                        )
                    }
                }
            }
        } else {
            // Stock transaction history
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("የመጋዘን ወጪ ገቢ ታሪክ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            if (transactions.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.History,
                    title = "ምንም ታሪክ የለም",
                    description = "ዕቃ ገቢ ሲያደርጉ ወይም ሽያጭ ሲያከናውኑ ታሪኩ እዚህ ይመዘገባል።"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions) { tx ->
                        val item = items.find { it.id == tx.itemId }
                        StockTransactionCard(tx = tx, itemName = item?.itemName ?: "የማይታወቅ ዕቃ")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddInventoryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, qty, unit, minAlert ->
                onAddItem(name, qty, unit, minAlert)
                showAddDialog = false
            }
        )
    }

    if (selectedItemForAdjust != null) {
        val item = selectedItemForAdjust!!
        AdjustStockDialog(
            itemName = item.itemName,
            unit = item.unit,
            type = adjustType,
            onDismiss = { selectedItemForAdjust = null },
            onConfirm = { qty, note ->
                onAdjustStock(item.id, adjustType, qty, note)
                selectedItemForAdjust = null
            }
        )
    }
}

@Composable
fun InventoryCard(
    item: InventoryItem,
    onStockIn: () -> Unit,
    onStockOut: () -> Unit,
    onDelete: () -> Unit
) {
    val isLowStock = item.quantity <= item.minStockAlert

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLowStock) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.itemName, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "የደህንነት ወሰን መጠን (Min): ${formatQty(item.minStockAlert)} ${item.unit}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Item",
                        tint = AccentRed.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock Level Display
                Column {
                    Text("አሁን ያለው ክምችት", fontSize = 11.sp, color = Color.Gray)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatQty(item.quantity),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isLowStock) AccentRed else AccentGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = item.unit, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 3.dp))
                    }
                }

                // Quick Stock-In / Stock-Out buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onStockOut,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                        border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.4f)),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Out", size = 16.dp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("ወጪ (-)", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onStockIn,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "In", size = 16.dp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("ገቢ (+)", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun Icon(imageVector: ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = Modifier.size(size)
    )
}

@Composable
fun StockTransactionCard(tx: StockTransaction, itemName: String) {
    val isStockIn = tx.type == "ገቢ"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicator Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isStockIn) AccentGreen.copy(alpha = 0.15f)
                        else AccentRed.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isStockIn) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = tx.type,
                    tint = if (isStockIn) AccentGreen else AccentRed
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = itemName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (tx.note != null) {
                    Text(text = tx.note, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(text = formatDate(tx.timestamp), fontSize = 9.sp, color = Color.Gray.copy(alpha = 0.7f))
            }

            Text(
                text = "${if (isStockIn) "+" else "-"}${formatQty(tx.quantity)}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isStockIn) AccentGreen else AccentRed
            )
        }
    }
}


// ==========================================
// 3. CUSTOMER DEBT TAB (የደምበኛ ዕዳ)
// ==========================================
@Composable
fun CustomerDebtScreen(
    customers: List<Customer>,
    transactions: List<com.example.data.model.DebtTransaction>,
    onAddCustomer: (String, String?, Double) -> Unit,
    onAdjustDebt: (Int, String, Double, String?) -> Unit,
    onDeleteCustomer: (Customer) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCustomerForDetails by remember { mutableStateOf<Customer?>(null) }
    var selectedCustomerForAdjust by remember { mutableStateOf<Customer?>(null) }
    var adjustType by remember { mutableStateOf("ዕዳ") } // "ዕዳ" (Add Debt) or "ክፍያ" (Payment)

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("የደንበኞች ዕዳ እና ክፍያ መዝገብ", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Customer Debt & Payment Ledger", fontSize = 12.sp, color = Color.Gray)
            }

            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("add_customer_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("አዲስ ደንበኛ", fontSize = 13.sp)
            }
        }

        if (customers.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.People,
                title = "ምንም ደንበኛ አልተመዘገበም",
                description = "ዕዳ ያለበትን ወይም ለመመዝገብ የፈለጉትን ደንበኛ እዚህ 'አዲስ ደንበኛ' በመንካት ያስገቡ።"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(customers) { customer ->
                    CustomerDebtCard(
                        customer = customer,
                        onAddDebt = {
                            selectedCustomerForAdjust = customer
                            adjustType = "ዕዳ"
                        },
                        onAddPayment = {
                            selectedCustomerForAdjust = customer
                            adjustType = "ክፍያ"
                        },
                        onViewDetails = { selectedCustomerForDetails = customer },
                        onDelete = { onDeleteCustomer(customer) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddCustomerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, debt ->
                onAddCustomer(name, phone, debt)
                showAddDialog = false
            }
        )
    }

    if (selectedCustomerForAdjust != null) {
        val cust = selectedCustomerForAdjust!!
        AdjustDebtDialog(
            customerName = cust.name,
            type = adjustType,
            onDismiss = { selectedCustomerForAdjust = null },
            onConfirm = { amount, note ->
                onAdjustDebt(cust.id, adjustType, amount, note)
                selectedCustomerForAdjust = null
            }
        )
    }

    if (selectedCustomerForDetails != null) {
        val cust = selectedCustomerForDetails!!
        val customerTxs = transactions.filter { it.customerId == cust.id }
        CustomerTxDetailsDialog(
            customer = cust,
            transactions = customerTxs,
            onDismiss = { selectedCustomerForDetails = null }
        )
    }
}

@Composable
fun CustomerDebtCard(
    customer: Customer,
    onAddDebt: () -> Unit,
    onAddPayment: () -> Unit,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = customer.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    if (customer.phone != null) {
                        Text(text = "ስልክ: ${customer.phone}", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed.copy(alpha = 0.5f))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("ያለበት ጠቅላላ ዕዳ", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = "${formatCurrency(customer.totalDebt)} Br",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (customer.totalDebt > 0) AccentOrange else Color.Gray
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onAddPayment,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen),
                        border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Pay", size = 16.dp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("ክፍያ (-)", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onAddDebt,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Debt", size = 16.dp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("ዕዳ መዝግብ (+)", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}


// ==========================================
// 4. RUBBER SALES TAB (የጎማ ሽያጭ - በሜትር)
// ==========================================
@Composable
fun RubberSalesScreen(
    sales: List<Sale>,
    inventory: List<InventoryItem>,
    onAddRubberSale: (String, Double, Double, String?) -> Unit,
    onDeleteSale: (Sale) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    val totalRubberMeters = sales.sumOf { it.quantity }
    val totalRubberRevenue = sales.sumOf { it.totalPrice }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("የጎማ ሽያጭ መመዝገቢያ (በሜትር)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Rubber strips & Sealants (Sold per Meter)", fontSize = 12.sp, color = Color.Gray)
            }

            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("add_rubber_sale_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("ጎማ ሽያጭ", fontSize = 13.sp)
            }
        }

        // Summary Rubber metrics card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ጠቅላላ የተሸጠ ርዝመት", fontSize = 11.sp, color = Color.Gray)
                    Text("${formatQty(totalRubberMeters)} ሜትር", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
                Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color.LightGray))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ጠቅላላ የጎማ ገቢ", fontSize = 11.sp, color = Color.Gray)
                    Text("${formatCurrency(totalRubberRevenue)} Br", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = AccentGreen)
                }
            }
        }

        if (sales.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.DirectionsCar,
                title = "ምንም የጎማ ሽያጭ አልተመዘገበም",
                description = "የመኪና በር ጎማ፣ የመስኮት ወይም የሻንጣ ጎማ ሽያጮችን 'ጎማ ሽያጭ' የሚለውን በመጫን በሜትር ወይም በቁጥር መመዝገብ ይችላሉ።"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sales) { sale ->
                    SaleCard(sale = sale, onDelete = { onDeleteSale(sale) })
                }
            }
        }
    }

    if (showAddDialog) {
        AddSaleDialog(
            inventory = inventory,
            isRubberOnly = true,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, qty, _, price, customer ->
                onAddRubberSale(name, qty, price, customer)
                showAddDialog = false
            }
        )
    }
}


// ==========================================
// 5. EXPENSES TAB (የለት ወጪ)
// ==========================================
@Composable
fun ExpensesScreen(
    expenses: List<Expense>,
    onAddExpense: (String, String, Double) -> Unit,
    onDeleteExpense: (Expense) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val totalExpense = expenses.sumOf { it.amount }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("የእለት ወጪ መመዝገቢያ", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Daily Store Expenses Tracker", fontSize = 12.sp, color = Color.Gray)
            }

            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                modifier = Modifier.testTag("add_expense_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("ወጪ መዝግብ", fontSize = 13.sp)
            }
        }

        // Total Expenses Highlight
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.05f)),
            border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("አጠቃላይ የወጪዎች ድምር", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("${formatCurrency(totalExpense)} Br", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AccentRed)
            }
        }

        if (expenses.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.AccountBalanceWallet,
                title = "ምንም ወጪ አልተመዘገበም",
                description = "እንደ ኪራይ፣ ደመወዝ፣ ወይም የቁሳቁስ መግዣ ወጪዎችን 'ወጪ መዝግብ' በመጫን መመዝገብ ይችላሉ።"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(expenses) { expense ->
                    ExpenseCard(expense = expense, onDelete = { onDeleteExpense(expense) })
                }
            }
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, category, amount ->
                onAddExpense(title, category, amount)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ExpenseCard(expense: Expense, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = expense.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                SuggestionChip(
                    onClick = {},
                    label = { Text(expense.category, fontSize = 11.sp) },
                    modifier = Modifier.height(26.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDate(expense.timestamp),
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatCurrency(expense.amount)} Br",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = AccentRed
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Expense", tint = AccentRed.copy(alpha = 0.5f))
                }
            }
        }
    }
}


// ==========================================
// REUSABLE DIALOGS AND INPUT FORMS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSaleDialog(
    inventory: List<InventoryItem>,
    isRubberOnly: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, Double, String?) -> Unit
) {
    // Basic fields
    var itemName by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("") }
    var unitPriceStr by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    
    // Units
    val units = if (isRubberOnly) listOf("ሜትር", "ቁጥር") else listOf("ቁጥር", "ሜትር", "ስራ")
    var selectedUnit by remember { mutableStateOf(units.first()) }
    var showUnitDropdown by remember { mutableStateOf(false) }

    // Warehouse inventory auto-completion dropdown
    var showInventoryDropdown by remember { mutableStateOf(false) }
    val matchingItems = inventory.filter {
        it.itemName.contains(itemName, ignoreCase = true) &&
        (!isRubberOnly || it.itemName.contains("ጎማ", ignoreCase = true) || it.itemName.contains("rubber", ignoreCase = true))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isRubberOnly) "የጎማ ሽያጭ መመዝገቢያ" else "የእለት ሽያጭ መመዝገቢያ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                // Item Name (With Dropdown for matching warehouse item)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = {
                            itemName = it
                            showInventoryDropdown = it.isNotEmpty() && matchingItems.isNotEmpty()
                        },
                        label = { Text("የዕቃው/የአገልግሎቱ ስም") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Auto-complete Warehouse Items list
                    DropdownMenu(
                        expanded = showInventoryDropdown,
                        onDismissRequest = { showInventoryDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Text(
                            "ከመጋዘን ውስጥ ይምረጡ (ለመቀነስ) :",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        matchingItems.take(4).forEach { invItem ->
                            DropdownMenuItem(
                                text = { Text("${invItem.itemName} (ክምችት: ${formatQty(invItem.quantity)} ${invItem.unit})") },
                                onClick = {
                                    itemName = invItem.itemName
                                    selectedUnit = invItem.unit
                                    showInventoryDropdown = false
                                }
                            )
                        }
                    }
                }

                // Quantity and Unit Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("መጠን") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    // Unit selector dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("መለኪያ") },
                            trailingIcon = {
                                IconButton(onClick = { showUnitDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Units")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = showUnitDropdown,
                            onDismissRequest = { showUnitDropdown = false }
                        ) {
                            units.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        selectedUnit = u
                                        showUnitDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Unit Price
                OutlinedTextField(
                    value = unitPriceStr,
                    onValueChange = { unitPriceStr = it },
                    label = { Text("የአንዱ ዋጋ (Birr)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Optional Customer Name
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("የደንበኛ ስም (አማራጭ)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ተመለስ", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val qty = quantityStr.toDoubleOrNull() ?: 1.0
                            val price = unitPriceStr.toDoubleOrNull() ?: 0.0
                            if (itemName.isNotBlank() && price > 0) {
                                onConfirm(itemName, qty, selectedUnit, price, customerName)
                            }
                        },
                        enabled = itemName.isNotBlank() && (unitPriceStr.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text("መዝግብ")
                    }
                }
            }
        }
    }
}

@Composable
fun AddInventoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, Double) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var qtyStr by remember { mutableStateOf("") }
    var minAlertStr by remember { mutableStateOf("5") }

    val units = listOf("ቁጥር", "ሜትር")
    var selectedUnit by remember { mutableStateOf(units[0]) }
    var showUnitDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "አዲስ ዕቃ መጋዘን ውስጥ መመዝገቢያ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("የዕቃው ስም (ለምሳሌ፡ የበር ጎማ)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it },
                        label = { Text("የመጀመሪያ መጠን") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("መለኪያ") },
                            trailingIcon = {
                                IconButton(onClick = { showUnitDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Units")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = showUnitDropdown,
                            onDismissRequest = { showUnitDropdown = false }
                        ) {
                            units.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        selectedUnit = u
                                        showUnitDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = minAlertStr,
                    onValueChange = { minAlertStr = it },
                    label = { Text("ዝቅተኛ የክምችት ማሳሰቢያ መጠን (Min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ተመለስ", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val qty = qtyStr.toDoubleOrNull() ?: 0.0
                            val alert = minAlertStr.toDoubleOrNull() ?: 5.0
                            if (itemName.isNotBlank() && qty >= 0) {
                                onConfirm(itemName, qty, selectedUnit, alert)
                            }
                        },
                        enabled = itemName.isNotBlank() && (qtyStr.toDoubleOrNull() ?: -1.0) >= 0
                    ) {
                        Text("ዕቃውን መዝግብ")
                    }
                }
            }
        }
    }
}

@Composable
fun AdjustStockDialog(
    itemName: String,
    unit: String,
    type: String, // "ገቢ" or "ወጭ"
    onDismiss: () -> Unit,
    onConfirm: (Double, String?) -> Unit
) {
    var qtyStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (type == "ገቢ") "ክምችት ገቢ (ጨምር)" else "ክምችት ወጪ (ቀንስ)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (type == "ገቢ") AccentGreen else AccentRed
                )
                Text(text = "ዕቃ: $itemName", fontWeight = FontWeight.Medium, fontSize = 14.sp)

                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it },
                    label = { Text("መጠን በ $unit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ምክንያት/ማስታወሻ (አማራጭ)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ተመለስ", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val qty = qtyStr.toDoubleOrNull() ?: 0.0
                            if (qty > 0) {
                                onConfirm(qty, if (note.isBlank()) null else note)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "ገቢ") AccentGreen else AccentRed
                        ),
                        enabled = (qtyStr.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text("አረጋግጥ")
                    }
                }
            }
        }
    }
}

@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var initialDebtStr by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "አዲስ ደንበኛ መመዝገቢያ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("የደንበኛ ሙሉ ስም") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("ስልክ ቁጥር (አማራጭ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = initialDebtStr,
                    onValueChange = { initialDebtStr = it },
                    label = { Text("የመጀመሪያ ዕዳ (ካለ በ Birr)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ተመለስ", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val debt = initialDebtStr.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank()) {
                                onConfirm(name, phone, debt)
                            }
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("መዝግብ")
                    }
                }
            }
        }
    }
}

@Composable
fun AdjustDebtDialog(
    customerName: String,
    type: String, // "ዕዳ" or "ክፍያ"
    onDismiss: () -> Unit,
    onConfirm: (Double, String?) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (type == "ዕዳ") "ዕዳ መመዝገቢያ (ጨምር)" else "የዕዳ ክፍያ መመዝገቢያ (ቀንስ)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (type == "ዕዳ") AccentOrange else AccentGreen
                )
                Text(text = "ደንበኛ: $customerName", fontWeight = FontWeight.Medium, fontSize = 14.sp)

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("ገንዘብ መጠን (Birr)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ማስታወሻ/ምክንያት (ለምሳሌ፡ የበር ጎማ ዕዳ)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ተመለስ", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            if (amount > 0) {
                                onConfirm(amount, if (note.isBlank()) null else note)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "ዕዳ") AccentOrange else AccentGreen
                        ),
                        enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text("መዝግብ")
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerTxDetailsDialog(
    customer: Customer,
    transactions: List<DebtTransaction>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 500.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "${customer.name} - የዕዳ ታሪክ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (customer.phone != null) {
                    Text("ስልክ: ${customer.phone}", fontSize = 12.sp, color = Color.Gray)
                }
                Text(
                    "አሁን ያለበት አጠቃላይ ዕዳ: ${formatCurrency(customer.totalDebt)} Br",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("ምንም የዕዳ/የክፍያ ታሪክ የለም", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(transactions) { tx ->
                            val isDebt = tx.type == "ዕዳ"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isDebt) AccentOrange.copy(alpha = 0.06f)
                                        else AccentGreen.copy(alpha = 0.06f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isDebt) "ዕዳ ተመዘገበ" else "ክፍያ ተፈጸመ",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isDebt) AccentOrange else AccentGreen
                                    )
                                    if (tx.note != null) {
                                        Text(text = tx.note, fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                    Text(text = formatDate(tx.timestamp), fontSize = 9.sp, color = Color.Gray)
                                }
                                Text(
                                    text = "${if (isDebt) "+" else "-"}${formatCurrency(tx.amount)} Br",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDebt) AccentOrange else AccentGreen
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("እሺ")
                }
            }
        }
    }
}

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }

    val categories = listOf("የቤት ኪራይ", "ደመወዝ", "ዕቃዎች መግዣ", "መብራት/ውሃ/ስልክ", "ምግብ/ሻይ", "ሌላ ወጪ")
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "የእለት ወጪ መመዝገቢያ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = AccentRed
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("የወጪው ርዕስ/ማብራሪያ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("የወጪ አይነት") },
                        trailingIcon = {
                            IconButton(onClick = { showCategoryDropdown = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Category")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("የገንዘብ መጠን (Birr)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ተመለስ", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && amount > 0) {
                                onConfirm(title, selectedCategory, amount)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        enabled = title.isNotBlank() && (amountStr.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text("ወጪውን መዝግብ")
                    }
                }
            }
        }
    }
}


// ==========================================
// DECORATIVE AND UTILITY VIEWS
// ==========================================

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Empty State",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

// Utility Formatting Functions
fun formatCurrency(amount: Double): String {
    return String.format(Locale.US, "%,.2f", amount)
}

fun formatQty(qty: Double): String {
    return if (qty % 1.0 == 0.0) {
        qty.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", qty)
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
