package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.delay
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.theme.AppThemeStyle
import com.example.R
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.BusinessViewModel
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    viewModel: BusinessViewModel,
    currentTheme: AppThemeStyle,
    onThemeChange: (AppThemeStyle) -> Unit
) {
    // Dialog States
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    
    // Passcode Delete States
    var showPasscodeDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<Any?>(null) }
    var deleteActionType by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("abraham_decor_prefs", Context.MODE_PRIVATE) }

    // Settings States
    var showSettingsDialog by remember { mutableStateOf(false) }
    var textScale by remember { mutableStateOf(sharedPrefs.getFloat("text_scale_multiplier", 1.0f)) }
    var showHeroBanner by remember { mutableStateOf(sharedPrefs.getBoolean("show_hero_banner", true)) }
    var showSummaryCards by remember { mutableStateOf(sharedPrefs.getBoolean("show_summary_cards", true)) }
    var showHistoryBtn by remember { mutableStateOf(sharedPrefs.getBoolean("show_history_btn", true)) }
    var showGuideBtn by remember { mutableStateOf(sharedPrefs.getBoolean("show_guide_btn", true)) }
    var showAddSaleBtn by remember { mutableStateOf(sharedPrefs.getBoolean("show_add_sale_btn", true)) }
    var showNoticeBanner by remember { mutableStateOf(sharedPrefs.getBoolean("show_notice_banner", true)) }
    var deletionPasscode by remember { mutableStateOf(sharedPrefs.getString("deletion_passcode", "1234") ?: "1234") }
    var isHeaderExpanded by remember { mutableStateOf(true) }

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

    val todaySalesRaw = sales.filter { it.timestamp >= todayStart }.sumOf { it.totalPrice }
    val todayExpensesRaw = expenses.filter { it.timestamp >= todayStart }.sumOf { it.amount }
    val todayDebtRegistered = debtTransactions.filter { it.timestamp >= todayStart && it.type == "ዕዳ" }.sumOf { it.amount }
    val todayDebtPaid = debtTransactions.filter { it.timestamp >= todayStart && it.type == "ክፍያ" }.sumOf { it.amount }

    val todaySalesTotal = todaySalesRaw + todayDebtPaid
    val todayExpensesTotal = todayExpensesRaw + todayDebtRegistered
    val totalDebtOutstanding = customers.sumOf { it.totalDebt }
    val lowStockCount = inventoryItems.filter { it.quantity <= it.minStockAlert }.size

    val weeklyStart = remember(sales) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -7)
        }.timeInMillis
    }

    val monthlyStart = remember(sales) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -30)
        }.timeInMillis
    }

    val totalInventoryAsset = inventoryItems.sumOf { it.quantity * it.purchasePrice }
    val weeklySales = sales.filter { it.timestamp >= weeklyStart }.sumOf { it.totalPrice }
    val monthlySales = sales.filter { it.timestamp >= monthlyStart }.sumOf { it.totalPrice }

    val currentDensity = LocalDensity.current
    val customDensity = remember(currentDensity, textScale) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * textScale
        )
    }

    CompositionLocalProvider(LocalDensity provides customDensity) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "የሒሳብ መዝገብ አያያዝ",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Ledger & Financial Record Keeping",
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
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text("$lowStockCount", modifier = Modifier.padding(2.dp), fontSize = 10.sp)
                            }
                        }
                        if (showHistoryBtn) {
                            IconButton(onClick = { showHistoryDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "የስራ ታሪክ መዝገብ",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (showGuideBtn) {
                            IconButton(onClick = { showGuideDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "የአጠቃቀም መመሪያ እና ዲዛይን",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "ማስተካከያ (Settings)",
                                tint = MaterialTheme.colorScheme.primary
                            )
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
                // Collapsible Header Wrapper
                AnimatedVisibility(
                    visible = isHeaderExpanded,
                    enter = expandVertically(animationSpec = tween(500)) + fadeIn(animationSpec = tween(400)),
                    exit = shrinkVertically(animationSpec = tween(500)) + fadeOut(animationSpec = tween(400))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { change, dragAmount ->
                                    change.consume()
                                    if (dragAmount < -10f) {
                                        isHeaderExpanded = false
                                    }
                                }
                            }
                    ) {
                        // Interactive Hero Welcome Banner & Style Selector
                        if (showHeroBanner) {
                            DashboardHeroBanner(
                                currentTheme = currentTheme,
                                onThemeChange = onThemeChange
                            )
                        }

                        // Summary Cards Block (Universal Header)
                        if (showSummaryCards) {
                            SummaryDashboardRow(
                                todaySales = todaySalesTotal,
                                todayExpenses = todayExpensesTotal,
                                outstandingDebt = totalDebtOutstanding,
                                lowStockCount = lowStockCount,
                                currentTheme = currentTheme,
                                todaySalesRaw = todaySalesRaw,
                                todayExpensesRaw = todayExpensesRaw,
                                todayDebtRegistered = todayDebtRegistered,
                                todayDebtPaid = todayDebtPaid,
                                totalInventoryAsset = totalInventoryAsset,
                                weeklySales = weeklySales,
                                monthlySales = monthlySales
                            )
                        }
                    }
                }

                // Interactive Drag / Pull Handle to Expand or Collapse the Dashboard Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .clickable { isHeaderExpanded = !isHeaderExpanded }
                        .padding(vertical = 4.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, dragAmount ->
                                change.consume()
                                if (dragAmount < -8f) {
                                    isHeaderExpanded = false
                                } else if (dragAmount > 8f) {
                                    isHeaderExpanded = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Drag Handle Line indicator
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isHeaderExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isHeaderExpanded) "ደብቅ" else "ዘርጋ",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHeaderExpanded) "የዳሽቦርድ መረጃዎችን ደብቅ (ለማሳነስ ወደላይ ይሳቡ / ይጫኑ)" else "የዳሽቦርድ መረጃዎችን ዘርጋ (ለመዘርጋት ወደታች ይሳቡ / ይጫኑ)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(bottom = 2.dp)
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
                        onAddSale = { name, qty, unit, sellPrice, cust ->
                            viewModel.addSale(itemName = name, quantity = qty, unit = unit, unitPrice = sellPrice, customerName = cust, isRubber = false)
                        },
                        onDeleteSale = {
                            itemToDelete = it
                            deleteActionType = "SALE"
                            showPasscodeDialog = true
                        },
                        showAddBtn = showAddSaleBtn,
                        showNoticeBanner = showNoticeBanner
                    )
                    1 -> InventoryScreen(
                        items = inventoryItems,
                        transactions = stockTransactions,
                        onAddItem = { name, qty, unit, alert, buyPrice ->
                            viewModel.addInventoryItem(name, qty, unit, alert, buyPrice)
                        },
                        onAdjustStock = { id, type, qty, note ->
                            viewModel.adjustStock(id, type, qty, note)
                        },
                        onDeleteItem = {
                            itemToDelete = it
                            deleteActionType = "INVENTORY_ITEM"
                            showPasscodeDialog = true
                        }
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
                        onDeleteCustomer = {
                            itemToDelete = it
                            deleteActionType = "CUSTOMER"
                            showPasscodeDialog = true
                        }
                    )
                    3 -> RubberSalesScreen(
                        sales = rubberSales,
                        inventory = inventoryItems,
                        onAddRubberSale = { name, qty, sellPrice, cust ->
                            viewModel.addSale(itemName = name, quantity = qty, unit = "ሜትር", unitPrice = sellPrice, customerName = cust, isRubber = true)
                        },
                        onDeleteSale = {
                            itemToDelete = it
                            deleteActionType = "SALE"
                            showPasscodeDialog = true
                        },
                        showAddBtn = showAddSaleBtn
                    )
                    4 -> ExpensesScreen(
                        expenses = expenses,
                        debtTransactions = debtTransactions,
                        onAddExpense = { title, category, amount ->
                            viewModel.addExpense(title, category, amount)
                        },
                        onDeleteExpense = {
                            itemToDelete = it
                            deleteActionType = "EXPENSE"
                            showPasscodeDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showHistoryDialog) {
        AllHistoryDialog(
            sales = sales,
            stockTransactions = stockTransactions,
            debtTransactions = debtTransactions,
            expenses = expenses,
            customers = customers,
            inventoryItems = inventoryItems,
            onDeleteSale = {
                itemToDelete = it
                deleteActionType = "SALE"
                showPasscodeDialog = true
            },
            onDeleteStockTx = {
                itemToDelete = it
                deleteActionType = "STOCK_TX"
                showPasscodeDialog = true
            },
            onDeleteDebtTx = {
                itemToDelete = it
                deleteActionType = "DEBT_TX"
                showPasscodeDialog = true
            },
            onDeleteExpense = {
                itemToDelete = it
                deleteActionType = "EXPENSE"
                showPasscodeDialog = true
            },
            onDismiss = { showHistoryDialog = false }
        )
    }

    if (showGuideDialog) {
        GuideAndThemeDialog(
            currentTheme = currentTheme,
            onThemeChange = onThemeChange,
            onDismiss = { showGuideDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentTheme = currentTheme,
            onThemeChange = onThemeChange,
            textScale = textScale,
            onTextScaleChange = { newScale ->
                textScale = newScale
                sharedPrefs.edit().putFloat("text_scale_multiplier", newScale).apply()
            },
            showHeroBanner = showHeroBanner,
            onShowHeroBannerChange = { newVal ->
                showHeroBanner = newVal
                sharedPrefs.edit().putBoolean("show_hero_banner", newVal).apply()
            },
            showSummaryCards = showSummaryCards,
            onShowSummaryCardsChange = { newVal ->
                showSummaryCards = newVal
                sharedPrefs.edit().putBoolean("show_summary_cards", newVal).apply()
            },
            showHistoryBtn = showHistoryBtn,
            onShowHistoryBtnChange = { newVal ->
                showHistoryBtn = newVal
                sharedPrefs.edit().putBoolean("show_history_btn", newVal).apply()
            },
            showGuideBtn = showGuideBtn,
            onShowGuideBtnChange = { newVal ->
                showGuideBtn = newVal
                sharedPrefs.edit().putBoolean("show_guide_btn", newVal).apply()
            },
            showAddSaleBtn = showAddSaleBtn,
            onShowAddSaleBtnChange = { newVal ->
                showAddSaleBtn = newVal
                sharedPrefs.edit().putBoolean("show_add_sale_btn", newVal).apply()
            },
            showNoticeBanner = showNoticeBanner,
            onShowNoticeBannerChange = { newVal ->
                showNoticeBanner = newVal
                sharedPrefs.edit().putBoolean("show_notice_banner", newVal).apply()
            },
            deletionPasscode = deletionPasscode,
            onDeletionPasscodeChange = { newPass ->
                deletionPasscode = newPass
                sharedPrefs.edit().putString("deletion_passcode", newPass).apply()
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showPasscodeDialog) {
        var enteredPasscode by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showPasscodeDialog = false
                itemToDelete = null
                deleteActionType = null
                enteredPasscode = ""
                isError = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AccentRed,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ማጥፊያ የይለፉ ቃል ማረጋገጫ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "ይህንን መረጃ በቋሚነት ለማጥፋት እባክዎ የይለፉ ቃል ያስገቡ።",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (deletionPasscode == "1234") "(ነባሪ የይለፉ ቃል: 1234)" else "(የተቀየረ የይለፉ ቃል)",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = enteredPasscode,
                        onValueChange = {
                            if (it.length <= 8) {
                                enteredPasscode = it
                                isError = false
                            }
                        },
                        label = { Text("የይለፉ ቃል (Passcode)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        isError = isError,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isError) {
                        Text(
                            text = "የተሳሳተ የይለፉ ቃል! እባክዎ እንደገና ይሞክሩ።",
                            color = AccentRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredPasscode == deletionPasscode) {
                            val item = itemToDelete
                            when (deleteActionType) {
                                "SALE" -> if (item is Sale) viewModel.deleteSale(item)
                                "STOCK_TX" -> if (item is StockTransaction) viewModel.deleteStockTransaction(item)
                                "DEBT_TX" -> if (item is DebtTransaction) viewModel.deleteDebtTransaction(item)
                                "EXPENSE" -> if (item is Expense) viewModel.deleteExpense(item)
                                "INVENTORY_ITEM" -> if (item is InventoryItem) viewModel.deleteInventoryItem(item)
                                "CUSTOMER" -> if (item is Customer) viewModel.deleteCustomer(item)
                            }
                            showPasscodeDialog = false
                            itemToDelete = null
                            deleteActionType = null
                            enteredPasscode = ""
                            isError = false
                        } else {
                            isError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("አረጋግጥ (Confirm)")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasscodeDialog = false
                        itemToDelete = null
                        deleteActionType = null
                        enteredPasscode = ""
                        isError = false
                    }
                ) {
                    Text("ተመለስ (Cancel)")
                }
            }
        )
    }
    }
}

// --- UNIVERSAL SUMMARY HEADER COMPONENT ---
@Composable
fun SummaryDashboardRow(
    todaySales: Double,
    todayExpenses: Double,
    outstandingDebt: Double,
    lowStockCount: Int,
    currentTheme: AppThemeStyle,
    todaySalesRaw: Double = 0.0,
    todayExpensesRaw: Double = 0.0,
    todayDebtRegistered: Double = 0.0,
    todayDebtPaid: Double = 0.0,
    totalInventoryAsset: Double = 0.0,
    weeklySales: Double = 0.0,
    monthlySales: Double = 0.0
) {
    val todayNet = todaySales - todayExpenses
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Today's Net Remaining Balance (ከወጪ ቀሪ) Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "የእለቱ ጠቅላላ ቀሪ (ከወጪ ቀሪ)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${formatCurrency(todayNet)} Br",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (todayNet >= 0) AccentGreen else AccentRed
                    )
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            (if (todayNet >= 0) AccentGreen else AccentRed).copy(alpha = 0.12f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (todayNet >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (todayNet >= 0) AccentGreen else AccentRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Today's Sales Card
            SummaryCard(
                title = "የዛሬ ሽያጭ",
                value = "${formatCurrency(todaySales)} Br",
                icon = Icons.Default.ShoppingCart,
                accentColor = AccentGreen,
                modifier = Modifier.weight(1f),
                subtitle = "ሽያጭ: ${formatCurrency(todaySalesRaw)} + ክፍያ: ${formatCurrency(todayDebtPaid)}"
            )

            // Today's Expenses Card
            SummaryCard(
                title = "የዛሬ ወጪ",
                value = "${formatCurrency(todayExpenses)} Br",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = if (todayExpenses > 0) AccentRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f),
                subtitle = "ወጪ: ${formatCurrency(todayExpensesRaw)} + ዕዳ: ${formatCurrency(todayDebtRegistered)}"
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Outstanding Debt Card
            SummaryCard(
                title = "የደንበኞች ዕዳ",
                value = "${formatCurrency(outstandingDebt)} Br",
                icon = Icons.Default.People,
                accentColor = if (outstandingDebt > 0) AccentOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            )

            // Low Stock Items Card
            SummaryCard(
                title = "ክምችት ያለቀባቸው",
                value = if (lowStockCount > 0) "$lowStockCount ዕቃዎች" else "ሁሉም ሙሉ ነው",
                icon = Icons.Default.Layers,
                accentColor = if (lowStockCount > 0) AccentRed else AccentGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Total Warehouse Asset Card (በመጋዘን ያለ ሀብት)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "በመጋዘን ያለ ጠቅላላ ሀብት (Inventory Asset)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${formatCurrency(totalInventoryAsset)} Br",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Weekly Sales Card
            SummaryCard(
                title = "የሳምንት ሽያጭ (7 ቀናት)",
                value = "${formatCurrency(weeklySales)} Br",
                icon = Icons.Default.ShoppingCart,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            // Monthly Sales Card
            SummaryCard(
                title = "የወር ሽያጭ (30 ቀናት)",
                value = "${formatCurrency(monthlySales)} Br",
                icon = Icons.Default.TrendingUp,
                accentColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun DashboardHeroBanner(
    currentTheme: AppThemeStyle,
    onThemeChange: (AppThemeStyle) -> Unit
) {
    val bannerImages = remember {
        listOf(
            R.drawable.img_tech_workspace_1782899106032,
            R.drawable.img_server_cloud_1782899117474,
            R.drawable.img_digital_circuit_1782899129008,
            R.drawable.img_coding_screen_1782899142256
        )
    }

    var currentImageIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            currentImageIndex = (currentImageIndex + 1) % bannerImages.size
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "text_animation")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset"
    )

    val textScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "text_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            // Header Image Banner with dark scrim overlay and smooth Crossfade transition
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Crossfade(
                    targetState = currentImageIndex,
                    animationSpec = tween(durationMillis = 1200),
                    modifier = Modifier.fillMaxSize(),
                    label = "image_carousel"
                ) { index ->
                    Image(
                        painter = painterResource(id = bannerImages[index]),
                        contentDescription = "ውሽዬ ሶፍትዌር ሶልሺን ቴክ ባነር",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Black.copy(alpha = 0.78f)
                                )
                            )
                        )
                )

                // Text overlay with animations
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .offset(y = floatOffset.dp)
                            .scale(textScale),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ውሽዬ ሶፍትዌር ሶልሺን",
                            color = Color(0xFF00F0FF), // Neon tech cyan
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black,
                                    offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                    blurRadius = 4f
                                )
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "+251911029070",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black,
                                    offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                    blurRadius = 3f
                                )
                            )
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "የእለት ሽያጭ፣ መጋዘን እና የዕዳ መከታተያ ሲስተም",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black,
                                    offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                    blurRadius = 2f
                                )
                            )
                        )
                    }
                }
            }

            // Theme Switcher row under the image
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ቀለም ይቀይሩ (Theme):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = currentTheme.amharicName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AppThemeStyle.values().forEach { style ->
                        val isSelected = currentTheme == style
                        val accentColor = when(style) {
                            AppThemeStyle.CLASSIC_GOLD -> Color(0xFFFFB703)
                            AppThemeStyle.NEON_RACER -> Color(0xFF00F0FF)
                            AppThemeStyle.ROYAL_CARBON -> Color(0xFF3B82F6)
                            AppThemeStyle.LUXURY_LEATHER -> Color(0xFFE5A93C)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) accentColor.copy(alpha = 0.15f)
                                    else Color.Gray.copy(alpha = 0.05f)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) accentColor else Color.Gray.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { onThemeChange(style) }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(accentColor, RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = style.amharicName,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
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
    onDeleteSale: (Sale) -> Unit,
    showAddBtn: Boolean = true,
    showNoticeBanner: Boolean = true
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

                if (showAddBtn) {
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
            }

            // Linkage notice banner
            if (showNoticeBanner) {
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
                sales = sales,
                onDeleteSale = onDeleteSale,
                onDismiss = { showAddDialog = false },
                onConfirm = { name, qty, unit, sellPrice, customer ->
                    onAddSale(name, qty, unit, sellPrice, customer)
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "መጠን: ${formatQty(sale.quantity)} ${sale.unit}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "የተገዛበት: ${formatCurrency(sale.purchasePrice)} Br",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "የተሸጠበት: ${formatCurrency(sale.unitPrice)} Br",
                        fontSize = 11.sp,
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
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatCurrency(sale.totalPrice)} Br",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = AccentGreen
                )
                var showDeleteConfirm by remember { mutableStateOf(false) }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Sale",
                        tint = AccentRed.copy(alpha = 0.7f)
                    )
                }
                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text("የመሰረዝ ማረጋገጫ", fontWeight = FontWeight.Bold) },
                        text = { Text("እርግጠኛ ነዎት ይህንን የሽያጭ መዝገብ መሰረዝ ይፈልጋሉ?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteConfirm = false
                                    onDelete()
                                }
                            ) {
                                Text("አዎ ሰርዝ", color = AccentRed)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text("ተው")
                            }
                        }
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
    onAddItem: (String, Double, String, Double, Double) -> Unit,
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
            items = items,
            onDeleteItem = onDeleteItem,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, qty, unit, minAlert, buyPrice ->
                onAddItem(name, qty, unit, minAlert, buyPrice)
            }
        )
    }

    if (selectedItemForAdjust != null) {
        val item = selectedItemForAdjust!!
        val filteredTxs = transactions.filter { it.itemId == item.id }
        AdjustStockDialog(
            itemName = item.itemName,
            unit = item.unit,
            type = adjustType,
            transactions = filteredTxs,
            onDismiss = { selectedItemForAdjust = null },
            onConfirm = { qty, note ->
                onAdjustStock(item.id, adjustType, qty, note)
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
                        "የተገዛበት ዋጋ: ${formatCurrency(item.purchasePrice)} Br | የደህንነት ወሰን መጠን (Min): ${formatQty(item.minStockAlert)} ${item.unit}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "የተመዘገበበት ቀን፡ ${formatDate(item.timestamp)}",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                var showDeleteConfirm by remember { mutableStateOf(false) }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Item",
                        tint = AccentRed.copy(alpha = 0.5f)
                    )
                }
                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text("የመሰረዝ ማረጋገጫ", fontWeight = FontWeight.Bold) },
                        text = { Text("እርግጠኛ ነዎት ይህንን ዕቃ ከመጋዘን መሰረዝ ይፈልጋሉ?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteConfirm = false
                                    onDelete()
                                }
                            ) {
                                Text("አዎ ሰርዝ", color = AccentRed)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text("ተው")
                            }
                        }
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
                Text(text = formatDate(tx.timestamp), fontSize = 12.sp, color = Color.Gray.copy(alpha = 0.8f))
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
            customers = customers,
            onDeleteCustomer = onDeleteCustomer,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, debt ->
                onAddCustomer(name, phone, debt)
            }
        )
    }

    if (selectedCustomerForAdjust != null) {
        val cust = selectedCustomerForAdjust!!
        val customerTxs = transactions.filter { it.customerId == cust.id }
        AdjustDebtDialog(
            customerName = cust.name,
            type = adjustType,
            transactions = customerTxs,
            onDismiss = { selectedCustomerForAdjust = null },
            onConfirm = { amount, note ->
                onAdjustDebt(cust.id, adjustType, amount, note)
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
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "የተመዘገበበት ቀን፡ ${formatDate(customer.timestamp)}",
                        fontSize = 12.5.sp,
                        color = Color.Gray.copy(alpha = 0.8f)
                    )
                }
                var showDeleteConfirm by remember { mutableStateOf(false) }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed.copy(alpha = 0.5f))
                }
                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text("የመሰረዝ ማረጋገጫ", fontWeight = FontWeight.Bold) },
                        text = { Text("እርግጠኛ ነዎት ይህንን ደንበኛ ማህደር መሰረዝ ይፈልጋሉ?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteConfirm = false
                                    onDelete()
                                }
                            ) {
                                Text("አዎ ሰርዝ", color = AccentRed)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text("ተው")
                            }
                        }
                    )
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
    onDeleteSale: (Sale) -> Unit,
    showAddBtn: Boolean = true
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

            if (showAddBtn) {
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
            sales = sales,
            onDeleteSale = onDeleteSale,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, qty, _, sellPrice, customer ->
                onAddRubberSale(name, qty, sellPrice, customer)
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
    debtTransactions: List<com.example.data.model.DebtTransaction> = emptyList(),
    onAddExpense: (String, String, Double) -> Unit,
    onDeleteExpense: (Expense) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    val storeExpensesSum = expenses.sumOf { it.amount }
    val customerDebtSum = debtTransactions.filter { it.type == "ዕዳ" }.sumOf { it.amount }
    val totalExpenseCombined = storeExpensesSum + customerDebtSum

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
            Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("አጠቃላይ የወጪዎች ድምር (የሱቅ + ደንበኛ ዕዳ)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("${formatCurrency(totalExpenseCombined)} Br", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AccentRed)
                }
                if (customerDebtSum > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• መደበኛ ወጪዎች: ${formatCurrency(storeExpensesSum)} Br | • የተመዘገበ ዕዳ: ${formatCurrency(customerDebtSum)} Br",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
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
            expenses = expenses,
            onDeleteExpense = onDeleteExpense,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, category, amount ->
                onAddExpense(title, category, amount)
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
                    fontSize = 12.sp,
                    color = Color.Gray.copy(alpha = 0.8f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatCurrency(expense.amount)} Br",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = AccentRed
                )
                var showDeleteConfirm by remember { mutableStateOf(false) }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Expense", tint = AccentRed.copy(alpha = 0.5f))
                }
                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text("የመሰረዝ ማረጋገጫ", fontWeight = FontWeight.Bold) },
                        text = { Text("እርግጠኛ ነዎት ይህንን የወጪ መዝገብ መሰረዝ ይፈልጋሉ?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteConfirm = false
                                    onDelete()
                                }
                            ) {
                                Text("አዎ ሰርዝ", color = AccentRed)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text("ተው")
                            }
                        }
                    )
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
    sales: List<Sale>,
    onDeleteSale: (Sale) -> Unit,
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

    QuarterScreenBottomSheetDialog(onDismiss = onDismiss, scrollable = false) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.Start
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
                                showInventoryDropdown = matchingItems.isNotEmpty()
                            },
                            label = { Text("የዕቃው/የአገልግሎቱ ስም") },
                            trailingIcon = {
                                IconButton(onClick = { showInventoryDropdown = !showInventoryDropdown }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "ከመጋዘን ምረጥ"
                                    )
                                }
                            },
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
                            matchingItems.take(20).forEach { invItem ->
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

                    // Selling Price Row (የተሸጠበት ዋጋ)
                    OutlinedTextField(
                        value = unitPriceStr,
                        onValueChange = { unitPriceStr = it },
                        label = { Text("የተሸጠበት ዋጋ (Birr)") },
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
                                    // Clear form inputs for continuous entries
                                    itemName = ""
                                    quantityStr = ""
                                    unitPriceStr = ""
                                    customerName = ""
                                }
                            },
                            enabled = itemName.isNotBlank() && (unitPriceStr.toDoubleOrNull() ?: 0.0) > 0
                        ) {
                            Text("መዝግብ")
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isRubberOnly) "ዛሬ የተመዘገቡ የጎማ ሽያጮች" else "ዛሬ የተመዘገቡ ሽያጮች / ስራዎች",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (sales.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("እስካሁን ምንም የተመዘገበ የለም", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                items(sales) { sale ->
                    SaleCard(sale = sale, onDelete = { onDeleteSale(sale) })
                }
            }
        }
    }
}

@Composable
fun AddInventoryDialog(
    items: List<InventoryItem>,
    onDeleteItem: (InventoryItem) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, Double, Double) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var qtyStr by remember { mutableStateOf("") }
    var purchasePriceStr by remember { mutableStateOf("") }
    var minAlertStr by remember { mutableStateOf("5") }

    val units = listOf("ቁጥር", "ሜትር")
    var selectedUnit by remember { mutableStateOf(units[0]) }
    var showUnitDropdown by remember { mutableStateOf(false) }

    QuarterScreenBottomSheetDialog(onDismiss = onDismiss, scrollable = false) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.Start
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
                        value = purchasePriceStr,
                        onValueChange = { purchasePriceStr = it },
                        label = { Text("የተገዛበት ዋጋ (Birr)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

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
                                val buyPrice = purchasePriceStr.toDoubleOrNull() ?: 0.0
                                if (itemName.isNotBlank() && qty >= 0) {
                                    onConfirm(itemName, qty, selectedUnit, alert, buyPrice)
                                    // Clear form for continuous entries
                                    itemName = ""
                                    qtyStr = ""
                                    purchasePriceStr = ""
                                    minAlertStr = "5"
                                }
                            },
                            enabled = itemName.isNotBlank() && (qtyStr.toDoubleOrNull() ?: -1.0) >= 0
                        ) {
                            Text("ዕቃውን መዝግብ")
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "በመጋዘን ውስጥ ያሉ ዕቃዎች",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("እስካሁን ምንም ዕቃ አልተመዘገበም", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                items(items) { item ->
                    InventoryCard(
                        item = item,
                        onStockIn = {},
                        onStockOut = {},
                        onDelete = { onDeleteItem(item) }
                    )
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
    transactions: List<StockTransaction>,
    onDismiss: () -> Unit,
    onConfirm: (Double, String?) -> Unit
) {
    var qtyStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    QuarterScreenBottomSheetDialog(onDismiss = onDismiss, scrollable = false) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.Start
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
                                    // Clear form for continuous entries
                                    qtyStr = ""
                                    note = ""
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

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "የዚህ ዕቃ የክምችት እንቅስቃሴ ታሪክ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("እስካሁን ምንም ታሪክ የለም", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                items(transactions) { tx ->
                    StockTransactionCard(tx = tx, itemName = itemName)
                }
            }
        }
    }
}

@Composable
fun AddCustomerDialog(
    customers: List<Customer>,
    onDeleteCustomer: (Customer) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var initialDebtStr by remember { mutableStateOf("") }

    QuarterScreenBottomSheetDialog(onDismiss = onDismiss, scrollable = false) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.Start
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
                                    onConfirm(name, phone.ifBlank { null }, debt)
                                    // Clear form for continuous entries
                                    name = ""
                                    phone = ""
                                    initialDebtStr = ""
                                }
                            },
                            enabled = name.isNotBlank()
                        ) {
                            Text("መዝግብ")
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "የተመዘገቡ ደንበኞች ማህደር",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (customers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("እስካሁን ምንም ደንበኛ አልተመዘገበም", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                items(customers) { customer ->
                    CustomerDebtCard(
                        customer = customer,
                        onAddDebt = {},
                        onAddPayment = {},
                        onViewDetails = {},
                        onDelete = { onDeleteCustomer(customer) }
                    )
                }
            }
        }
    }
}

@Composable
fun AdjustDebtDialog(
    customerName: String,
    type: String, // "ዕዳ" or "ክፍያ"
    transactions: List<DebtTransaction>,
    onDismiss: () -> Unit,
    onConfirm: (Double, String?) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    QuarterScreenBottomSheetDialog(onDismiss = onDismiss, scrollable = false) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.Start
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
                                    // Clear form for continuous entries
                                    amountStr = ""
                                    note = ""
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

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "የዚህ ደንበኛ የዕዳ / የክፍያ ታሪክ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("እስካሁን ምንም ታሪክ የለም", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
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
                            Text(text = formatDate(tx.timestamp), fontSize = 12.sp, color = Color.Gray.copy(alpha = 0.8f))
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
    }
}

@Composable
fun CustomerTxDetailsDialog(
    customer: Customer,
    transactions: List<DebtTransaction>,
    onDismiss: () -> Unit
) {
    QuarterScreenBottomSheetDialog(onDismiss = onDismiss, scrollable = false) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
            horizontalAlignment = Alignment.Start
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
                                Text(text = formatDate(tx.timestamp), fontSize = 12.sp, color = Color.Gray.copy(alpha = 0.8f))
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

@Composable
fun AddExpenseDialog(
    expenses: List<Expense>,
    onDeleteExpense: (Expense) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }

    val categories = listOf("የቤት ኪራይ", "ደመወዝ", "ዕቃዎች መግዣ", "መብራት/ውሃ/ስልክ", "ምግብ/ሻይ", "ሌላ ወጪ")
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    QuarterScreenBottomSheetDialog(onDismiss = onDismiss, scrollable = false) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.Start
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
                                    // Clear form for continuous entries
                                    title = ""
                                    amountStr = ""
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

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "የተመዘገቡ ወጪዎች",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (expenses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("እስካሁን ምንም ወጪ አልተመዘገበም", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                items(expenses) { expense ->
                    ExpenseCard(expense = expense, onDelete = { onDeleteExpense(expense) })
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllHistoryDialog(
    sales: List<Sale>,
    stockTransactions: List<StockTransaction>,
    debtTransactions: List<DebtTransaction>,
    expenses: List<Expense>,
    customers: List<Customer>,
    inventoryItems: List<InventoryItem>,
    onDeleteSale: (Sale) -> Unit,
    onDeleteStockTx: (StockTransaction) -> Unit,
    onDeleteDebtTx: (DebtTransaction) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onDismiss: () -> Unit
) {
    // Collect and sort history items
    val combinedHistory = remember(sales, stockTransactions, debtTransactions, expenses, customers, inventoryItems) {
        val list = mutableListOf<HistoryItem>()
        sales.forEach { list.add(HistoryItem.SaleItem(it)) }
        stockTransactions.forEach { tx ->
            val itemName = inventoryItems.find { it.id == tx.itemId }?.itemName ?: "ያልታወቀ ዕቃ"
            list.add(HistoryItem.StockTxItem(tx, itemName))
        }
        debtTransactions.forEach { tx ->
            val customerName = customers.find { it.id == tx.customerId }?.name ?: "ያልታወቀ ደንበኛ"
            list.add(HistoryItem.DebtTxItem(tx, customerName))
        }
        expenses.forEach { list.add(HistoryItem.ExpenseItem(it)) }
        list.sortByDescending { it.timestamp }
        list
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedYear by remember { mutableStateOf("ሁሉም ዓመታት") }
    var selectedMonth by remember { mutableStateOf("ሁሉም ወራት") }
    var selectedDay by remember { mutableStateOf("ሁሉም ቀናት") }

    val filteredHistory = remember(combinedHistory, searchQuery, selectedYear, selectedMonth, selectedDay) {
        combinedHistory.filter { item ->
            // 1. Text Search matching
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val query = searchQuery.trim().lowercase()
                when (item) {
                    is HistoryItem.SaleItem -> {
                        item.sale.itemName.lowercase().contains(query) ||
                        (item.sale.customerName?.lowercase()?.contains(query) ?: false)
                    }
                    is HistoryItem.StockTxItem -> {
                        item.itemName.lowercase().contains(query) ||
                        (item.tx.note?.lowercase()?.contains(query) ?: false)
                    }
                    is HistoryItem.DebtTxItem -> {
                        item.customerName.lowercase().contains(query) ||
                        (item.tx.note?.lowercase()?.contains(query) ?: false)
                    }
                    is HistoryItem.ExpenseItem -> {
                        item.expense.title.lowercase().contains(query) ||
                        item.expense.category.lowercase().contains(query)
                    }
                }
            }

            // 2. Date Filtering matching (Day, Month, Year)
            val matchesDate = if (selectedYear == "ሁሉም ዓመታት" && selectedMonth == "ሁሉም ወራት" && selectedDay == "ሁሉም ቀናት") {
                true
            } else {
                val cal = Calendar.getInstance().apply { timeInMillis = item.timestamp }
                val yearVal = cal.get(Calendar.YEAR)
                val monthVal = cal.get(Calendar.MONTH) + 1 // 1-indexed
                val dayVal = cal.get(Calendar.DAY_OF_MONTH)

                val yearMatches = if (selectedYear == "ሁሉም ዓመታት") true else yearVal.toString() == selectedYear
                val monthMatches = if (selectedMonth == "ሁሉም ወራት") {
                    true
                } else {
                    val expectedMonthIndex = when (selectedMonth) {
                        "ጥር (1)" -> 1
                        "የካቲት (2)" -> 2
                        "መጋቢት (3)" -> 3
                        "ሚያዝያ (4)" -> 4
                        "ግንቦት (5)" -> 5
                        "ሰኔ (6)" -> 6
                        "ሐምሌ (7)" -> 7
                        "ነሐሴ (8)" -> 8
                        "መስከረም (9)" -> 9
                        "ጥቅምት (10)" -> 10
                        "ኅዳር (11)" -> 11
                        "ታኅሣሥ (12)" -> 12
                        else -> 0
                    }
                    monthVal == expectedMonthIndex
                }
                val dayMatches = if (selectedDay == "ሁሉም ቀናት") true else dayVal.toString() == selectedDay

                yearMatches && monthMatches && dayMatches
            }

            matchesSearch && matchesDate
        }
    }

    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ሙሉ የታሪክ መዝገብ",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("ታሪክ መፈለጊያ (ስም/ማስታወሻ...)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Date Selectors: Year, Month, Day
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Year Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(selectedYear, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("ሁሉም ዓመታት", fontSize = 11.sp) }, onClick = { selectedYear = "ሁሉም ዓመታት"; expanded = false })
                            listOf("2024", "2025", "2026", "2027", "2028").forEach { yr ->
                                DropdownMenuItem(text = { Text(yr, fontSize = 11.sp) }, onClick = { selectedYear = yr; expanded = false })
                            }
                        }
                    }

                    // Month Dropdown
                    Box(modifier = Modifier.weight(1.2f)) {
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(selectedMonth, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("ሁሉም ወራት", fontSize = 11.sp) }, onClick = { selectedMonth = "ሁሉም ወራት"; expanded = false })
                            listOf(
                                "ጥር (1)", "የካቲት (2)", "መጋቢት (3)", "ሚያዝያ (4)", "ግንቦት (5)", "ሰኔ (6)",
                                "ሐምሌ (7)", "ነሐሴ (8)", "መስከረም (9)", "ጥቅምት (10)", "ኅዳር (11)", "ታኅሣሥ (12)"
                            ).forEach { mo ->
                                DropdownMenuItem(text = { Text(mo, fontSize = 11.sp) }, onClick = { selectedMonth = mo; expanded = false })
                            }
                        }
                    }

                    // Day Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(selectedDay, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("ሁሉም ቀናት", fontSize = 11.sp) }, onClick = { selectedDay = "ሁሉም ቀናት"; expanded = false })
                            (1..31).forEach { d ->
                                DropdownMenuItem(text = { Text("$d", fontSize = 11.sp) }, onClick = { selectedDay = d.toString(); expanded = false })
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Reset filters if any are active
                if (searchQuery.isNotEmpty() || selectedYear != "ሁሉም ዓመታት" || selectedMonth != "ሁሉም ወራት" || selectedDay != "ሁሉም ቀናት") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                searchQuery = ""
                                selectedYear = "ሁሉም ዓመታት"
                                selectedMonth = "ሁሉም ወራት"
                                selectedDay = "ሁሉም ቀናት"
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("ማጣሪያዎቹን አጽዳ (Reset Filter)", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                if (filteredHistory.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ከተመረጡት ማጣሪያዎች ጋር የሚስማማ ታሪክ አልተገኘም",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = {
                                searchQuery = ""
                                selectedYear = "ሁሉም ዓመታት"
                                selectedMonth = "ሁሉም ወራት"
                                selectedDay = "ሁሉም ቀናት"
                            }) {
                                Text("ሁሉንም አሳይ")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredHistory) { item ->
                            when (item) {
                                is HistoryItem.SaleItem -> {
                                    HistoryCard(
                                        title = "ሽያጭ: ${item.sale.itemName}",
                                        subtitle = "${formatQty(item.sale.quantity)} ${item.sale.unit} @ የተሸጠበት: ${formatCurrency(item.sale.unitPrice)} Br (የተገዛበት: ${formatCurrency(item.sale.purchasePrice)} Br)",
                                        amount = "+${formatCurrency(item.sale.totalPrice)} Br",
                                        amountColor = AccentGreen,
                                        timestamp = item.sale.timestamp,
                                        icon = Icons.Default.ShoppingCart,
                                        tag = if (item.sale.isRubber) "ጎማ" else "የእለት",
                                        tagColor = MaterialTheme.colorScheme.primary,
                                        onDelete = { onDeleteSale(item.sale) }
                                    )
                                }
                                is HistoryItem.StockTxItem -> {
                                    val isIn = item.tx.type == "ገቢ"
                                    HistoryCard(
                                        title = "የመጋዘን እንቅስቃሴ: ${item.itemName}",
                                        subtitle = "አይነት: ${item.tx.type} (${item.tx.note ?: ""})",
                                        amount = "${if (isIn) "+" else "-"}${formatQty(item.tx.quantity)}",
                                        amountColor = if (isIn) AccentGreen else AccentRed,
                                        timestamp = item.tx.timestamp,
                                        icon = Icons.Default.Layers,
                                        tag = "ክምችት",
                                        tagColor = MaterialTheme.colorScheme.secondary,
                                        onDelete = { onDeleteStockTx(item.tx) }
                                    )
                                }
                                is HistoryItem.DebtTxItem -> {
                                    val isDebt = item.tx.type == "ዕዳ"
                                    HistoryCard(
                                        title = "የዕዳ መዝገብ: ${item.customerName}",
                                        subtitle = "አይነት: ${item.tx.type} (${item.tx.note ?: ""})",
                                        amount = "${if (isDebt) "+" else "-"}${formatCurrency(item.tx.amount)} Br",
                                        amountColor = if (isDebt) AccentRed else AccentGreen,
                                        timestamp = item.tx.timestamp,
                                        icon = Icons.Default.People,
                                        tag = "ዕዳ",
                                        tagColor = if (isDebt) AccentRed else AccentGreen,
                                        onDelete = { onDeleteDebtTx(item.tx) }
                                    )
                                }
                                is HistoryItem.ExpenseItem -> {
                                    HistoryCard(
                                        title = "ወጪ: ${item.expense.title}",
                                        subtitle = "ዘርፍ: ${item.expense.category}",
                                        amount = "-${formatCurrency(item.expense.amount)} Br",
                                        amountColor = AccentRed,
                                        timestamp = item.expense.timestamp,
                                        icon = Icons.Default.AccountBalanceWallet,
                                        tag = "ወጪ",
                                        tagColor = AccentRed,
                                        onDelete = { onDeleteExpense(item.expense) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class HistoryItem(val timestamp: Long) {
    class SaleItem(val sale: Sale) : HistoryItem(sale.timestamp)
    class StockTxItem(val tx: StockTransaction, val itemName: String) : HistoryItem(tx.timestamp)
    class DebtTxItem(val tx: DebtTransaction, val customerName: String) : HistoryItem(tx.timestamp)
    class ExpenseItem(val expense: Expense) : HistoryItem(expense.timestamp)
}

@Composable
fun HistoryCard(
    title: String,
    subtitle: String,
    amount: String,
    amountColor: Color,
    timestamp: Long,
    icon: ImageVector,
    tag: String,
    tagColor: Color,
    onDelete: (() -> Unit)? = null
) {
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
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(tagColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tagColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text(tag, fontSize = 8.sp) },
                            modifier = Modifier.height(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = subtitle, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = formatDate(timestamp), fontSize = 12.sp, color = Color.Gray.copy(alpha = 0.8f))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = amount,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = amountColor
                )
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "ሰርዝ",
                            tint = AccentRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideAndThemeDialog(
    currentTheme: AppThemeStyle,
    onThemeChange: (AppThemeStyle) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column {
                // Header Image Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_car_decor_banner_1782811501631),
                        contentDescription = "Car Decor Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Dark overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                    )
                    // Text and close button over image
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.BottomStart),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "የሒሳብ መዝገብ አያያዝ",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "የአጠቃቀም መመሪያ እና የዲዛይን ምርጫ",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Scrollable Contents
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Part 1: Theme Selector
                    item {
                        Text(
                            text = "🎨 የመተግበሪያውን ዲዛይን ይምረጡ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ለስራዎ የሚስማማውን የመኪና ዲኮር ቀለም ጭብጥ ይምረጡ።",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Grid of themes
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppThemeStyle.values().forEach { style ->
                                val isSelected = currentTheme == style
                                val (accentColor, cardBg) = when(style) {
                                    AppThemeStyle.CLASSIC_GOLD -> Color(0xFFFFB703) to Color(0xFF1B1E23)
                                    AppThemeStyle.NEON_RACER -> Color(0xFF00F0FF) to Color(0xFF12181F)
                                    AppThemeStyle.ROYAL_CARBON -> Color(0xFF3B82F6) to Color(0xFF152033)
                                    AppThemeStyle.LUXURY_LEATHER -> Color(0xFFE5A93C) to Color(0xFF1C1614)
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onThemeChange(style) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) cardBg else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) accentColor else Color.Gray.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(accentColor, RoundedCornerShape(12.dp))
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = style.amharicName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = style.displayName,
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { onThemeChange(style) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Divider
                    item {
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    }

                    // Part 2: User Guide in Amharic
                    item {
                        Text(
                            text = "📖 የአጠቃቀም መመሪያ (Amharic User Guide)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val guides = listOf(
                            Triple("🛒 የእለት ሽያጭ መቆጣጠሪያ", "በዚህ ገጽ ላይ የእለት ሽያጮችን ይመዝግቡ። ዕቃ ስም ሲያስገቡ በራስ-ሰር ከመጋዘን ክምችት ላይ ይቀንሳል። ደንበኛ ስም ከተጠቀሰ በኋላ እዳ ካለበት በደንበኛ ዕዳ ስር ማያያዝ ይችላሉ።", "ሽያጮችን መመዝገብ"),
                            Triple("📦 የመጋዘን ቁጥጥር", "መጋዘን ውስጥ አዳዲስ የመኪና ዲኮር ዕቃዎችን መመዝገብ፣ አሁን ያለውን ክምችት መጠን (Stock Level) ማየት፣ እና ዕቃዎችን 'ገቢ' ወይም 'ወጪ' በማድረግ ክምችቱን ማስተካከል ይችላሉ። ክምችቱ ከአነስተኛ የደህንነት ወሰን በታች ሲወርድ መተግበሪያው ቀይ የማስጠንቀቂያ ቀለም ያሳያል።", "ክምችት መቆጣጠር"),
                            Triple("👥 የደንበኞች ዕዳ እና ክፍያ", "ደንበኞችን በስም እና ስልክ ቁጥር ይመዝግቡ። ደንበኛው ያለበትን ጠቅላላ ዕዳ እና የፈጸመውን ክፍያ ታሪክ ዝርዝር ሁኔታ እዚህ ማየት እና ማስተካከል ይችላሉ።", "እዳ ማስተዳደር"),
                            Triple("🚗 የጎማ ሽያጭ", "የጎማ ሽያጮች በሜትር እና በተለየ ዘርፍ መመዝገብ ስላለባቸው፣ በዚህ ገጽ ላይ በቀላሉ የሽያጭ መጠን እና ዋጋን በሜትር ማስላት ይችላሉ።", "የጎማ ሽያጭ"),
                            Triple("💸 የእለት ወጪዎች", "ለስራው የሚወጡ ማናቸውንም ወጪዎች (ለምчнее የቤት ኪራይ፣ ደመወዝ፣ መብራት/ውሃ፣ ዕቃ መግዣ) በመመዝገብ በየእለቱ የሚወጣውን ገንዘብ ይከታተሉ።", "ወጪ መቆጣጠር"),
                            Triple("🕰️ የታሪክ መዝገብ", "ሁሉንም በስра ላይ የተከናወኑ ሽያጭዎችን፣ ክምችት ማስተካከያዎችን፣ የዕዳ ዝርዝሮችን፣ እና ወጪዎችን በአንድ ላይ በቅደም ተከተል ለመመልከት በስተቀኝ ያለውን የታሪክ ምልክት (History icon) ይጫኑ።", "ታሪክ መመልከት")
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            guides.forEach { (title, desc, tag) ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = desc,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuarterScreenBottomSheetDialog(
    onDismiss: () -> Unit,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "ተመለስ",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ተመለስ",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .let { mod ->
                            if (scrollable) mod.verticalScroll(rememberScrollState()) else mod
                        },
                    horizontalAlignment = Alignment.Start
                ) {
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentTheme: AppThemeStyle,
    onThemeChange: (AppThemeStyle) -> Unit,
    textScale: Float,
    onTextScaleChange: (Float) -> Unit,
    showHeroBanner: Boolean,
    onShowHeroBannerChange: (Boolean) -> Unit,
    showSummaryCards: Boolean,
    onShowSummaryCardsChange: (Boolean) -> Unit,
    showHistoryBtn: Boolean,
    onShowHistoryBtnChange: (Boolean) -> Unit,
    showGuideBtn: Boolean,
    onShowGuideBtnChange: (Boolean) -> Unit,
    showAddSaleBtn: Boolean,
    onShowAddSaleBtnChange: (Boolean) -> Unit,
    showNoticeBanner: Boolean,
    onShowNoticeBannerChange: (Boolean) -> Unit,
    deletionPasscode: String,
    onDeletionPasscodeChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Custom Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "ዝጋ",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "የመተግበሪያ ማስተካከያ (Settings)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Section 1: Font and number size changer
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TextFields,
                                    contentDescription = "Font Size",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "1. የጽሑፍ እና ቁጥሮች መጠን (Font Size)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Text scale options
                            val scaleOptions = listOf(
                                Triple(0.85f, "አነስተኛ (Small)", "85%"),
                                Triple(1.0f, "መደበኛ (Normal)", "100%"),
                                Triple(1.15f, "ትልቅ (Large)", "115%"),
                                Triple(1.3f, "በጣም ትልቅ (Extra Large)", "130%")
                            )

                            scaleOptions.forEach { (scale, label, percent) ->
                                val isSelected = textScale == scale
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onTextScaleChange(scale) }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { onTextScaleChange(scale) }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = label,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                    Text(
                                        text = percent,
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            // Preview text
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "የፊደል መጠን ቅድመ-እይታ (Live Preview):",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "የሒሳብ መዝገብ አያያዝ - 123,456.00 ብር",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // Section 2: UI Design Theme Selector
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Themes",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "2. የመተግበሪያው ዲዛይን ቀለም (UI Themes)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            AppThemeStyle.values().forEach { style ->
                                val isSelected = currentTheme == style
                                val accentColor = when (style) {
                                    AppThemeStyle.CLASSIC_GOLD -> Color(0xFFFFB703)
                                    AppThemeStyle.NEON_RACER -> Color(0xFF00FFCC)
                                    AppThemeStyle.ROYAL_CARBON -> Color(0xFF2E6FF2)
                                    AppThemeStyle.LUXURY_LEATHER -> Color(0xFFC68B59)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                            else Color.Transparent
                                        )
                                        .clickable { onThemeChange(style) }
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(accentColor, RoundedCornerShape(8.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = style.amharicName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = style.displayName,
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onThemeChange(style) }
                                    )
                                }
                            }
                        }
                    }

                    // Section 3: Dashboard Element Visibility Control
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Visibility",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "3. የዳሽቦርድ ማሳያ ምርጫዎች (Show/Hide Elements)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            val toggles = listOf(
                                ToggleItem("የጀርባ ምስል ባነር (Welcome Banner)", showHeroBanner, onShowHeroBannerChange),
                                ToggleItem("የእለት መረጃዎች (Summary Statistics Cards)", showSummaryCards, onShowSummaryCardsChange),
                                ToggleItem("የታሪክ ቁልፍ በራስጌ (History Icon Button)", showHistoryBtn, onShowHistoryBtnChange),
                                ToggleItem("የመመሪያ ቁልፍ በራስጌ (Guide Icon Button)", showGuideBtn, onShowGuideBtnChange),
                                ToggleItem("የሽያጭ መመዝገቢያ ቁልፎች (Add Entry Buttons)", showAddSaleBtn, onShowAddSaleBtnChange),
                                ToggleItem("የክምችት ማሳሰቢያ ፅሁፍ (Stock Linkage Notice)", showNoticeBanner, onShowNoticeBannerChange)
                            )

                            toggles.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { item.onCheckedChange(!item.checked) }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.label,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = item.checked,
                                        onCheckedChange = item.onCheckedChange
                                    )
                                }
                            }
                        }
                    }

                    // Section 4: Deletion Passcode Changer
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Passcode Change",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "4. መረጃዎችን ለመሰረዝ የሚጠየቅ የይለፍ ቃል",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            var tempPasscode by remember { mutableStateOf(deletionPasscode) }
                            var showPassError by remember { mutableStateOf(false) }
                            var showSuccessMsg by remember { mutableStateOf(false) }

                            OutlinedTextField(
                                value = tempPasscode,
                                onValueChange = {
                                    if (it.length <= 8) {
                                        tempPasscode = it
                                        showPassError = false
                                        showSuccessMsg = false
                                    }
                                },
                                label = { Text("አዲስ የይለፍ ቃል") },
                                placeholder = { Text("ለምሳሌ፡ 1234") },
                                singleLine = true,
                                isError = showPassError,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            if (showPassError) {
                                Text(
                                    text = "እባክዎ ትክክለኛ የይለፍ ቃል ያስገቡ (ባዶ መሆን የለበትም)",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            
                            if (showSuccessMsg) {
                                Text(
                                    text = "የይለፍ ቃሉ በተሳካ ሁኔታ ተቀይሯል!",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Button(
                                onClick = {
                                    if (tempPasscode.trim().isNotEmpty()) {
                                        onDeletionPasscodeChange(tempPasscode.trim())
                                        showSuccessMsg = true
                                        showPassError = false
                                    } else {
                                        showPassError = true
                                        showSuccessMsg = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("የይለፍ ቃል ቀይር (Save New Password)")
                            }
                        }
                    }

                    // Section 5: About App/Instruction section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "About",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "5. ስለ መተግበሪያው አሰራር (About App)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "የሒሳብ መዝገብ አያያዝ መተግበሪያ የዕለት ሽያጭን፣ የመጋዘን ክምችትን፣ ዕዳዎችን እና ሌሎች የንግድ ፋይናንስ እንቅስቃሴዎችን ለመቆጣጠር የተዘጋጀ ዘመናዊ ሲስተም ነው።",
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "📌 ዋና ዋና ተግባራት:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            val points = listOf(
                                "• **ክምችት መቆጣጠር**: በመጋዘን ያሉ ዕቃዎችን በክምችት መጠን እና በአነስተኛ ደህንነት ወሰን ይከታተላል።",
                                "• **እዳና ክፍያ**: የደንበኞችን ዕዳ በስም መዝግቦ የእለት ክፍያዎችን በራስ-ሰር ደምሮ ያስተካክላል።",
                                "• **የጎማ ሽያጭ**: የጎማ ሽያጮችን በሜትር በቀላሉ ያሰላል።",
                                "• **ታሪክ መዝገብ**: ሁሉንም ክንውኖች ሙሉ ታሪክ በሰከንድ ያስቀምጣል።"
                            )

                            points.forEach { pt ->
                                Text(
                                    text = pt,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "ስሪት (Version): 1.0.0\nየተገነባው ለ: የሒሳብ መዝገብ አያያዝ እና ፋይናንስ ቁጥጥር",
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ToggleItem(
    val label: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)


