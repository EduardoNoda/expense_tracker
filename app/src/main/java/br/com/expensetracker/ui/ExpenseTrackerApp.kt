package br.com.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.expensetracker.viewmodel.SimpleItem
import br.com.expensetracker.viewmodel.SummaryViewModel

data class TempExpenseData(
    val amount: Long,
    val catId: Int,
    val payId: Int,
    val installments: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerApp(viewModel: SummaryViewModel) {
    val state by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // --- ESTADOS DE CONFIRMAÇÃO ---
    var showConfirmRevenueDialog by remember { mutableStateOf<Pair<String, Long>?>(null) }

    // CORREÇÃO: Usando a classe de dados para suportar os 4 parâmetros
    var pendingExpenseConfirmation by remember { mutableStateOf<TempExpenseData?>(null) }

    var showAddCardDialog by remember { mutableStateOf(false) }

    // --- POPUP DE CONFIRMAÇÃO (RECEITA) ---
    if (showConfirmRevenueDialog != null) {
        val (name, amount) = showConfirmRevenueDialog!!
        AlertDialog(
            onDismissRequest = { showConfirmRevenueDialog = null },
            title = { Text("Confirmar Receita") },
            text = { Text("Deseja inserir a receita '$name' no valor de ${MoneyFormatter.format(amount)}?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.confirmAddRevenue(name, amount)
                    showConfirmRevenueDialog = null
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmRevenueDialog = null }) { Text("Cancelar") }
            }
        )
    }

    // --- POPUP DE CONFIRMAÇÃO (DESPESA) ---
    // Agora isso funciona corretamente com parcelas
    if (pendingExpenseConfirmation != null) {
        val data = pendingExpenseConfirmation!!

        AlertDialog(
            onDismissRequest = { pendingExpenseConfirmation = null },
            title = { Text("Confirmar Despesa") },
            text = {
                Column {
                    Text("Deseja lançar um gasto de ${MoneyFormatter.format(data.amount)}?")
                    if (data.installments > 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Parcelado em ${data.installments}x", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    // Chama a ViewModel para salvar de verdade
                    viewModel.confirmAddExpense(data.amount, data.catId, data.payId, data.installments)
                    pendingExpenseConfirmation = null
                    // O viewModel.confirmAddExpense já fecha o BottomSheet internamente
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingExpenseConfirmation = null }) { Text("Cancelar") }
            }
        )
    }

    // --- DIALOG ADICIONAR CARTÃO ---
    if (showAddCardDialog) {
        AddCardDialog(
            onDismiss = { showAddCardDialog = false },
            onConfirm = { name, closing, due ->
                viewModel.addNewCard(name, closing, due)
                showAddCardDialog = false
            }
        )
    }

    // --- BOTTOM SHEETS ---
    if (state.isAddingRevenue) {
        ModalBottomSheet(onDismissRequest = { viewModel.closeAddRevenueDialog() }, sheetState = sheetState) {
            AddRevenueSheetContent(
                onCancel = { viewModel.closeAddRevenueDialog() },
                onConfirm = { name, amount ->
                    showConfirmRevenueDialog = Pair(name, amount)
                }
            )
        }
    }

    if (state.isAddingExpense) {
        ModalBottomSheet(onDismissRequest = { viewModel.closeAddExpenseDialog() }, sheetState = sheetState) {
            AddExpenseSheetContent(
                categories = state.categories,
                paymentMethods = state.paymentMethods,
                onCancel = { viewModel.closeAddExpenseDialog() },
                // CORREÇÃO: Agora preenchemos o objeto temporário em vez de salvar direto
                onConfirm = { amount, catId, payId, installments ->
                    pendingExpenseConfirmation = TempExpenseData(amount, catId, payId, installments)
                }
            )
        }
    }

    Scaffold(
        topBar = {
            CustomTopBar(
                month = state.currentMonth,
                year = state.currentYear,
                onPrev = { viewModel.prevMonth() },
                onNext = { viewModel.nextMonth() },
                onAddCardClick = { showAddCardDialog = true }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddRevenueDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) { Icon(Icons.Default.Add, "Nova Receita") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            HeaderSummary(state.totalRevenue, state.totalExpense, state.balance)
            Spacer(modifier = Modifier.height(24.dp))
            Text("Receitas do Mês", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.revenues) { revenue ->
                    RevenueCard(revenue, onAddExpenseClick = { viewModel.openAddExpenseDialog(revenue.id) })
                }
            }
        }
    }
}

// --- DIALOGO PARA ADICIONAR CARTÃO ---
@Composable
fun AddCardDialog(onDismiss: () -> Unit, onConfirm: (String, Int, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var closingDay by remember { mutableStateOf("") }
    var dueDay by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Cartão de Crédito") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome (ex: Nubank)") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = closingDay, onValueChange = { if(it.length <= 2) closingDay = it.filter { c->c.isDigit() } },
                        label = { Text("Fecha dia") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = dueDay, onValueChange = { if(it.length <= 2) dueDay = it.filter { c->c.isDigit() } },
                        label = { Text("Vence dia") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val c = closingDay.toIntOrNull() ?: 0
                val d = dueDay.toIntOrNull() ?: 0
                if (name.isNotEmpty() && c in 1..31 && d in 1..31) onConfirm(name, c, d)
            }) { Text("Adicionar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun HeaderSummary(revenue: Long, expense: Long, balance: Long) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Saldo Total", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(MoneyFormatter.format(balance), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                    color = if (balance >= 0) Color(0xFF1B5E20) else Color(0xFFB71C1C))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryItem("Receitas", revenue, Color(0xFF2E7D32))
                SummaryItem("Despesas", expense, Color(0xFFC62828))
            }
        }
    }
}

// --- CONTEÚDO DO SHEET: RECEITA ---
@Composable
fun AddRevenueSheetContent(
    onCancel: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountCents by remember { mutableLongStateOf(0L) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Nova Receita", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = name, onValueChange = { name = it }, label = { Text("Descrição (ex: Salário)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        MoneyInput(valueCents = amountCents, onValueChange = { amountCents = it }, label = "Valor da Receita", modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text("Cancelar") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { if (name.isNotBlank() && amountCents > 0) onConfirm(name, amountCents) }, enabled = name.isNotBlank() && amountCents > 0) {
                Text("Salvar Receita")
            }
        }
    }
}

// --- CONTEÚDO DO SHEET: DESPESA ---
@Composable
fun AddExpenseSheetContent(
    categories: List<SimpleItem>,
    paymentMethods: List<SimpleItem>,
    onCancel: () -> Unit,
    onConfirm: (Long, Int, Int, Int) -> Unit
) {
    var amountCents by remember { mutableLongStateOf(0L) }
    var selectedCat by remember { mutableStateOf<SimpleItem?>(null) }
    var selectedPay by remember { mutableStateOf<SimpleItem?>(null) }

    // Estado do parcelamento como Texto para permitir digitação
    var installmentsText by remember { mutableStateOf("1") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Adicionar Despesa", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        MoneyInput(
            valueCents = amountCents,
            onValueChange = { amountCents = it },
            label = "Valor Total",
            modifier = Modifier.fillMaxWidth()
        )

        AppDropdown(
            label = "Categoria",
            items = categories,
            selectedItem = selectedCat,
            onItemSelected = { selectedCat = it }
        )

        AppDropdown(
            label = "Forma de Pagamento",
            items = paymentMethods,
            selectedItem = selectedPay,
            onItemSelected = { selectedPay = it }
        )

        // Seção de Parcelamento (Híbrida: Botões + Texto)
        if (selectedPay != null && selectedPay!!.id > 1) {
            Column {
                Text("Parcelamento", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botão Menos
                    FilledTonalIconButton(
                        onClick = {
                            val current = installmentsText.toIntOrNull() ?: 1
                            if (current > 1) installmentsText = (current - 1).toString()
                        }
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, "-")
                    }

                    // Campo de Texto Central
                    OutlinedTextField(
                        value = installmentsText,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } && it.length <= 3) {
                                installmentsText = it
                            }
                        },
                        label = { Text("Qtd") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        )
                    )

                    // Botão Mais
                    FilledTonalIconButton(
                        onClick = {
                            val current = installmentsText.toIntOrNull() ?: 1
                            if (current < 999) installmentsText = (current + 1).toString()
                        }
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, "+")
                    }
                }

                // Mostra valor da parcela
                val count = installmentsText.toIntOrNull() ?: 1
                if (count > 0 && amountCents > 0) {
                    Text(
                        text = "Valor da parcela: ${MoneyFormatter.format(amountCents / count)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text("Cancelar") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val finalInstallments = installmentsText.toIntOrNull() ?: 1
                    if (amountCents > 0 && selectedCat != null && selectedPay != null && finalInstallments > 0) {
                        onConfirm(amountCents, selectedCat!!.id, selectedPay!!.id, finalInstallments)
                    }
                },
                enabled = amountCents > 0 && selectedCat != null && selectedPay != null
            ) {
                Text("Lançar Gasto")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDropdown(label: String, items: List<SimpleItem>, selectedItem: SimpleItem?, onItemSelected: (SimpleItem) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedItem?.name ?: "", onValueChange = {}, label = { Text(label) }, readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(), colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (items.isEmpty()) { DropdownMenuItem(text = { Text("Carregando...") }, onClick = { expanded = false }, enabled = false) }
            else { items.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { onItemSelected(item); expanded = false }) } }
        }
    }
}

@Composable
fun SummaryItem(label: String, amount: Long, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(MoneyFormatter.format(amount), color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun CustomTopBar(month: Int, year: Int, onPrev: () -> Unit, onNext: () -> Unit, onAddCardClick: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(shadowElevation = 2.dp, color = Color.White) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, "Perfil", tint = MaterialTheme.colorScheme.primary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Ant") }
                Text(String.format("%02d/%d", month, year), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Prox") }
            }
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "Opções") }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Adicionar Cartão") },
                        onClick = { showMenu = false; onAddCardClick() },
                        leadingIcon = { Icon(Icons.Default.Add, null) }
                    )
                }
            }
        }
    }
}

@Composable
fun RevenueCard(revenue: br.com.expensetracker.viewmodel.RevenueUI, onAddExpenseClick: () -> Unit) {
    val totalSpent = revenue.expenses.sumOf { it.amountCents }
    val remaining = revenue.amountCents - totalSpent

    Card(elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(revenue.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Recebido: ${revenue.date}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                FilledTonalIconButton(onClick = onAddExpenseClick) { Icon(Icons.Default.ShoppingCart, "Novo Gasto") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            val progress = if (revenue.amountCents > 0) totalSpent.toFloat() / revenue.amountCents else 0f
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = if (progress > 0.9f) Color.Red else MaterialTheme.colorScheme.primary, trackColor = Color.LightGray.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("Total", style = MaterialTheme.typography.bodySmall); Text(MoneyFormatter.format(revenue.amountCents), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) }
                Column(horizontalAlignment = Alignment.End) { Text("Restante", style = MaterialTheme.typography.bodySmall); Text(MoneyFormatter.format(remaining), fontWeight = FontWeight.SemiBold, color = if (remaining < 0) Color.Red else Color(0xFF2E7D32)) }
            }
            if (revenue.expenses.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Gastos Vinculados:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                revenue.expenses.forEach { expense ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("- ${MoneyFormatter.format(expense.amountCents)}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}