package com.srbd.testrc

import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.PurchasesConfiguration

import com.revenuecat.purchases.CustomerInfo
//import com.revenuecat.purchases.Entitlement
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val expenseRepository = ExpenseRepository(applicationContext)
        val revenueCatManager = RevenueCatManager(applicationContext)

        setContent {
            ExpenseTrackerApp(
                expenseRepository = expenseRepository,
                revenueCatManager = revenueCatManager,
            )
        }
    }
}

@Composable
private fun ExpenseTrackerApp(
    expenseRepository: ExpenseRepository,
    revenueCatManager: RevenueCatManager,
) {
    var expenses by remember { mutableStateOf(expenseRepository.getExpenses()) }
    var selectedTheme by remember { mutableStateOf(expenseRepository.selectedTheme()) }
    var revenueCatStatus by remember {
        mutableStateOf(
            RevenueCatStatus(
                isConfigured = false,
                hasThemeAccess = false,
                message = "RevenueCat not checked yet.",
            )
        )
    }
    val scope = rememberCoroutineScope()
    val activity = LocalActivity.current

    LaunchedEffect(Unit) {
        revenueCatStatus = revenueCatManager.initialize()
        if (revenueCatStatus.isConfigured) {
            revenueCatStatus = revenueCatManager.refreshCustomerInfo()
        }
    }

    AppTheme(selectedTheme, revenueCatStatus.hasThemeAccess) {
        ExpenseTrackerScreen(
            expenses = expenses,
            selectedTheme = selectedTheme,
            hasThemeAccess = revenueCatStatus.hasThemeAccess,
            revenueCatStatus = revenueCatStatus,
            storeName = revenueCatManager.storeName,
            productId = revenueCatManager.productId,
            entitlementId = revenueCatManager.entitlementId,
            onAddExpense = { amount, category, note, date ->
                expenses = expenseRepository.addExpense(amount, category, note, date)
            },
            onThemeSelected = { theme ->
                selectedTheme = theme
                expenseRepository.saveSelectedTheme(theme)
            },
            onSubscribe = {
                scope.launch {
                    if (activity == null) {
                        revenueCatStatus = revenueCatStatus.copy(
                            message = "Purchase cannot start because no Activity is available.",
                        )
                        return@launch
                    }
                    revenueCatStatus = revenueCatStatus.copy(message = "Opening purchase flow...")
                    revenueCatStatus = revenueCatManager.purchaseTheme(activity)
                }
            },
            onRestore = {
                scope.launch {
                    revenueCatStatus = revenueCatStatus.copy(message = "Checking RevenueCat...")
                    revenueCatStatus = revenueCatManager.restorePurchases()
                }
            },
        )
    }
}

@Composable
private fun AppTheme(
    selectedTheme: AppThemeChoice,
    hasThemeAccess: Boolean,
    content: @Composable () -> Unit,
) {
    val scheme = when (if (hasThemeAccess) selectedTheme else AppThemeChoice.Default) {
        AppThemeChoice.Dark -> darkColorScheme(
            primary = Color(0xFFB9FBC0),
            secondary = Color(0xFF98F5E1),
            surface = Color(0xFF151817),
            background = Color(0xFF0E1110),
        )
        AppThemeChoice.Blue -> lightColorScheme(
            primary = Color(0xFF0057B8),
            secondary = Color(0xFF38BDF8),
            surface = Color(0xFFF5FAFF),
            background = Color(0xFFEAF4FF),
        )
        AppThemeChoice.Green -> lightColorScheme(
            primary = Color(0xFF166534),
            secondary = Color(0xFF84CC16),
            surface = Color(0xFFF7FBEF),
            background = Color(0xFFEFF8E5),
        )
        AppThemeChoice.Default -> lightColorScheme(
            primary = Color(0xFF9A3412),
            secondary = Color(0xFFEAB308),
            surface = Color(0xFFFFFBF2),
            background = Color(0xFFFFF7E6),
        )
    }

    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
private fun ExpenseTrackerScreen(
    expenses: List<Expense>,
    selectedTheme: AppThemeChoice,
    hasThemeAccess: Boolean,
    revenueCatStatus: RevenueCatStatus,
    storeName: String,
    productId: String,
    entitlementId: String,
    onAddExpense: (Double, ExpenseCategory, String, String) -> Unit,
    onThemeSelected: (AppThemeChoice) -> Unit,
    onSubscribe: () -> Unit,
    onRestore: () -> Unit,
) {
    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)
                    )
                )
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    HeaderCard(expenses)
                }
                item {
                    AddExpenseCard(onAddExpense)
                }
                item {
                    SubscriptionCard(
                        selectedTheme = selectedTheme,
                        hasThemeAccess = hasThemeAccess,
                        revenueCatStatus = revenueCatStatus,
                        storeName = storeName,
                        productId = productId,
                        entitlementId = entitlementId,
                        onThemeSelected = onThemeSelected,
                        onSubscribe = onSubscribe,
                        onRestore = onRestore,
                    )
                }
                item {
                    Text(
                        text = "Expenses",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (expenses.isEmpty()) {
                    item { EmptyExpenseCard() }
                } else {
                    items(expenses, key = { it.id }) { expense ->
                        ExpenseRow(expense)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(expenses: List<Expense>) {
    val total = expenses.sumOf { it.amount }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(Modifier.padding(22.dp)) {
            Text("Simple Expense Tracker", color = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.height(10.dp))
            Text(
                text = formatMoney(total),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "Total expense amount across ${expenses.size} entries",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseCard(onAddExpense: (Double, ExpenseCategory, String, String) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(ExpenseRepository.today()) }
    var category by remember { mutableStateOf(ExpenseCategory.Food) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Card(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add Daily Expense", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded },
            ) {
                OutlinedTextField(
                    value = category.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    ExpenseCategory.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.name) },
                            onClick = {
                                category = option
                                categoryExpanded = false
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Optional note") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date, yyyy-MM-dd") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        error = "Enter a valid amount greater than 0."
                    } else {
                        onAddExpense(amount, category, note, date)
                        amountText = ""
                        note = ""
                        date = ExpenseRepository.today()
                        error = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add Expense")
            }
        }
    }
}

@Composable
private fun SubscriptionCard(
    selectedTheme: AppThemeChoice,
    hasThemeAccess: Boolean,
    revenueCatStatus: RevenueCatStatus,
    storeName: String,
    productId: String,
    entitlementId: String,
    onThemeSelected: (AppThemeChoice) -> Unit,
    onSubscribe: () -> Unit,
    onRestore: () -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Theme Subscription", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Store: $storeName")
                }
                Text(if (hasThemeAccess) "Active" else "Locked", fontWeight = FontWeight.Bold)
            }
            Text("Product: $productId | Entitlement: $entitlementId")
            Text(revenueCatStatus.message)
            if (hasThemeAccess) {
                Text("Select a subscriber theme:")
                listOf(AppThemeChoice.Dark, AppThemeChoice.Blue, AppThemeChoice.Green).forEach { theme ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedTheme == theme, onClick = { onThemeSelected(theme) })
                        Text(theme.label)
                    }
                }
            } else {
                Text("Subscribe to unlock Dark, Blue, and Green themes.")
                Button(onClick = onSubscribe, modifier = Modifier.fillMaxWidth()) {
                    Text("Subscribe")
                }
            }
            OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                Text("Restore / Check Subscription")
            }
        }
    }
}

@Composable
private fun ExpenseRow(expense: Expense) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(expense.category.name, fontWeight = FontWeight.Bold)
                Text(expense.date)
                if (expense.note.isNotBlank()) Text(expense.note)
            }
            Spacer(Modifier.width(12.dp))
            Text(formatMoney(expense.amount), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyExpenseCard() {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)) {
        Text(
            text = "No expenses yet. Add your first daily expense above.",
            modifier = Modifier.padding(18.dp),
        )
    }
}

private fun formatMoney(value: Double): String {
    return NumberFormat.getCurrencyInstance(Locale.US).format(value)
}
