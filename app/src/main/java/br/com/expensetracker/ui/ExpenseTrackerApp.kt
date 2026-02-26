package br.com.expensetracker.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.expensetracker.viewmodel.SimpleItem
import br.com.expensetracker.viewmodel.SummaryViewModel
import kotlinx.coroutines.launch
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.togetherWith
import androidx.compose.ui.tooling.preview.Preview

data class TempExpenseData(val revenueId: Int, val amount: Long, val catId: Int, val payId: Int, val installments: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerApp(viewModel: SummaryViewModel) {
    val state by viewModel.uiState.collectAsState()

    val revenueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val expenseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var showConfirmRevenueDialog by remember { mutableStateOf<Pair<String, Long>?>(null) }
    var pendingExpenseConfirmation by remember { mutableStateOf<TempExpenseData?>(null) }
    var showAddCardDialog by remember { mutableStateOf(false) }

    // --- ALERTA: SEM RECEITAS ---
    if (state.showNoRevenueWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNoRevenueWarning() },
            title = { Text("Atenção") },
            text = { Text("Você precisa adicionar pelo menos uma receita antes de registrar um gasto neste mês.") },
            confirmButton = {
                Button(onClick = { viewModel.dismissNoRevenueWarning() }) { Text("Entendi") }
            }
        )
    }

    // --- POPUPS DE CONFIRMAÇÃO ---
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
            dismissButton = { TextButton(onClick = { showConfirmRevenueDialog = null }) { Text("Cancelar") } }
        )
    }

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
                    viewModel.confirmAddExpense(data.revenueId, data.amount, data.catId, data.payId, data.installments)
                    pendingExpenseConfirmation = null
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { pendingExpenseConfirmation = null }) { Text("Cancelar") } }
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
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeAddRevenueDialog() },
            sheetState = revenueSheetState
        ) {
            AddRevenueSheetContent(
                onCancel = {
                    scope.launch {
                        revenueSheetState.hide()
                        viewModel.closeAddRevenueDialog()
                    }
                },
                onConfirm = { name, amount ->
                    scope.launch {
                        revenueSheetState.hide()
                        viewModel.closeAddRevenueDialog()
                        showConfirmRevenueDialog = Pair(name, amount)
                    }
                }
            )
        }
    }

    if (state.isAddingExpense) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeAddExpenseDialog() },
            sheetState = expenseSheetState
        ) {
            AddExpenseSheetContent(
                categories = state.categories,
                paymentMethods = state.paymentMethods,
                onCancel = {
                    scope.launch {
                        expenseSheetState.hide()
                        viewModel.closeAddExpenseDialog()
                    }
                },
                onConfirm = { amount, catId, payId, installments ->
                    val currentRevId = state.selectedRevenueIdForExpense
                    if (currentRevId != null) {
                        scope.launch {
                            expenseSheetState.hide()
                            viewModel.closeAddExpenseDialog()
                            pendingExpenseConfirmation = TempExpenseData(currentRevId, amount, catId, payId, installments)
                        }
                    }
                }
            )
        }
    }

    // Variável para controlar se a animação vai para a esquerda (-1) ou direita (1)
    var animationDirection by remember { mutableIntStateOf(1) }

    Scaffold(
        topBar = {
            CustomTopBar(
                month = state.currentMonth, year = state.currentYear,
                onPrev = {
                    animationDirection = 1 // Direção: voltando
                    viewModel.prevMonth()
                },
                onNext = {
                    animationDirection = -1 // Direção: avançando
                    viewModel.nextMonth()
                }
            )
        },
        floatingActionButton = {
            // ... (SEU CÓDIGO DO FLOATING ACTION BUTTON CONTINUA EXATAMENTE IGUAL AQUI) ...
            var expandedFabMenu by remember { mutableStateOf(false) }

            val rotation by animateFloatAsState(
                targetValue = if (expandedFabMenu) 45f else 0f,
                label = "fab_rotation"
            )

            if (!state.isSelectingRevenueMode) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    AnimatedVisibility(
                        visible = expandedFabMenu,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { 50 })
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            FabMenuItem(
                                text = "Novo Gasto",
                                icon = Icons.Default.ShoppingCart,
                                onClick = { expandedFabMenu = false; viewModel.onAddExpenseActionClicked() }
                            )
                            FabMenuItem(
                                text = "Nova Receita",
                                icon = Icons.Default.Add,
                                onClick = { expandedFabMenu = false; viewModel.openAddRevenueDialog() },
                                containerColor = Color(0xFFE8F5E9),
                                tint = Color(0xFF2E7D32)
                            )
                            FabMenuItem(
                                text = "Novo Cartão",
                                icon = Icons.Default.Add,
                                onClick = { expandedFabMenu = false; showAddCardDialog = true }
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = { expandedFabMenu = !expandedFabMenu },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Menu Principal",
                            modifier = Modifier
                                .scale(if (expandedFabMenu) 1.2f else 1f)
                                .rotate(rotation)
                        )
                    }
                }
            }
        } // Fim do FAB
    ) { padding ->

        var swipeOffset by remember { mutableFloatStateOf(0f) }

        // Container principal que captura o deslize do dedo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (swipeOffset > 60) {
                                animationDirection = 1
                                viewModel.prevMonth()
                            } else if (swipeOffset < -60) {
                                animationDirection = -1
                                viewModel.nextMonth()
                            }
                            swipeOffset = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            swipeOffset += dragAmount
                        }
                    )
                }
        ) {
            // A MÁGICA DA ANIMAÇÃO ACONTECE AQUI
            // Ele observa a dupla (Mês, Ano). Se mudar, ele dispara a transição.
            // A MÁGICA DA ANIMAÇÃO ACONTECE AQUI
            AnimatedContent(
                targetState = state, // 1. Agora passamos o estado completo da UI
                transitionSpec = {
                    if (animationDirection == -1) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut())
                    }
                },
                // 2. O Segredo: Ele só dispara a animação se a "chave" (mês/ano) mudar!
                contentKey = { "${it.currentMonth}-${it.currentYear}" },
                label = "MonthTransition"
            ) { targetState ->
                // 3. AQUI ESTAVA O ERRO! Em vez de usar '_', chamamos de 'targetState'
                // E trocamos todos os 'state.' por 'targetState.' aqui dentro.

                Column(modifier = Modifier.fillMaxSize()) {
                    HeaderSummary(targetState.totalRevenue, targetState.totalExpense, targetState.balance)

                    AnimatedVisibility(visible = targetState.isSelectingRevenueMode) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Selecione a receita que receberá o gasto", style = MaterialTheme.typography.labelLarge)
                                IconButton(onClick = { viewModel.cancelRevenueSelection() }) {
                                    Icon(Icons.Default.Close, "Cancelar")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Receitas do Mês", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(targetState.revenues) { revenue ->
                            RevenueCard(
                                revenue = revenue,
                                isSelectingMode = targetState.isSelectingRevenueMode,
                                onCardClick = {
                                    if (targetState.isSelectingRevenueMode) {
                                        viewModel.openAddExpenseDialog(revenue.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- NOVO COMPONENTE: ITEM DO MENU FAB SEGURO ---
@Composable
fun FabMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    tint: Color = LocalContentColor.current
) {
    // Row com clique silencioso (sem ripple problemático) para pegar a área toda
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
        }
        Spacer(modifier = Modifier.width(12.dp))
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = containerColor
        ) {
            Icon(icon, contentDescription = text, tint = tint)
        }
    }
}

// --- CARD DE RECEITA ---
@Composable
fun RevenueCard(
    revenue: br.com.expensetracker.viewmodel.RevenueUI,
    isSelectingMode: Boolean,
    onCardClick: () -> Unit
) {
    val totalSpent = revenue.expenses.sumOf { it.amountCents }
    val remaining = revenue.amountCents - totalSpent

    val scale = if (isSelectingMode) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PulsingCard"
        ).value
    } else 1f

    val baseModifier = Modifier
        .fillMaxWidth()
        .scale(scale)

    val finalModifier = if (isSelectingMode) {
        baseModifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onCardClick
        )
    } else {
        baseModifier
    }

    Card(
        elevation = CardDefaults.cardElevation(if (isSelectingMode) 8.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelectingMode) Color(0xFFE8F5E9) else Color.White),
        modifier = finalModifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(revenue.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Recebido: ${revenue.date}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
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

// --- OUTROS COMPONENTES ---

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
                if (name.isNotBlank() && c in 1..31 && d in 1..31) {
                    // CORREÇÃO: Mata o espaço extra e força a primeira maiúscula
                    val cleanName = name.trim().replaceFirstChar { it.uppercase() }
                    onConfirm(cleanName, c, d)
                }
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
            Button(
                onClick = {
                    if (name.isNotBlank() && amountCents > 0) {
                        // CORREÇÃO: Mata o espaço extra e força a primeira maiúscula
                        val cleanName = name.trim().replaceFirstChar { it.uppercase() }
                        onConfirm(cleanName, amountCents)
                    }
                },
                enabled = name.isNotBlank() && amountCents > 0
            ) {
                Text("Salvar Receita")
            }
        }
    }
}

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
    var installmentsText by remember { mutableStateOf("1") }

    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Adicionar Despesa", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        MoneyInput(valueCents = amountCents, onValueChange = { amountCents = it }, label = "Valor Total", modifier = Modifier.fillMaxWidth())
        AppDropdown(label = "Categoria", items = categories, selectedItem = selectedCat, onItemSelected = { selectedCat = it })
        AppDropdown(label = "Forma de Pagamento", items = paymentMethods, selectedItem = selectedPay, onItemSelected = { selectedPay = it })

        if (selectedPay != null && selectedPay!!.id > 1) {
            Column {
                Text("Parcelamento", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(onClick = { val c = installmentsText.toIntOrNull() ?: 1; if (c > 1) installmentsText = (c - 1).toString() }) {
                        Icon(Icons.Default.KeyboardArrowLeft, "-")
                    }
                    OutlinedTextField(
                        value = installmentsText, onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 3) installmentsText = it },
                        label = { Text("Qtd") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent)
                    )
                    FilledTonalIconButton(onClick = { val c = installmentsText.toIntOrNull() ?: 1; if (c < 999) installmentsText = (c + 1).toString() }) {
                        Icon(Icons.Default.KeyboardArrowRight, "+")
                    }
                }
                val count = installmentsText.toIntOrNull() ?: 1
                if (count > 0 && amountCents > 0) {
                    Text(text = "Valor da parcela: ${MoneyFormatter.format(amountCents / count)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
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
            ) { Text("Lançar Gasto") }
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
fun CustomTopBar(month: Int, year: Int, onPrev: () -> Unit, onNext: () -> Unit) {
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

                // MUDANÇA: Data formatada lindamente para o cliente (Ex: Fev/2026)
                val rawMonthName = Month.of(month)
                    .getDisplayName(TextStyle.SHORT, Locale("pt", "BR"))
                val monthName = rawMonthName.replace(".", "").lowercase().replaceFirstChar { it.uppercase() }

                Text(
                    text = "$monthName/$year",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Prox") }
            }
            IconButton(onClick = { /* Funcionalidades Futuras */ }) {
                Icon(Icons.Default.MoreVert, "Opções")
            }
        }
    }
}
// Isso aqui diz para o Android Studio desenhar essa função na tela lateral
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
fun AddExpensePreview() {
    // 1. Criamos dados falsos só pro Preview não quebrar
    val mockCategories = listOf(
        SimpleItem(1, "Alimentação"), SimpleItem(2, "Transporte"),
        SimpleItem(3, "Saúde"), SimpleItem(4, "Lazer"),
        SimpleItem(5, "Moradia"), SimpleItem(6, "Educação"),
        SimpleItem(7, "Supermercado"), SimpleItem(8, "Pet")
    )
    val mockPayments = listOf(
        SimpleItem(1, "Dinheiro/Pix"), SimpleItem(2, "Cartão Nubank"), SimpleItem(3, "Cartão Inter")
    )

    // 2. Chamamos a nossa tela com os dados falsos
    MaterialTheme { // Garante que as cores e fontes funcionem
        AddExpenseSheetContent(
            categories = mockCategories,
            paymentMethods = mockPayments,
            onCancel = {},
            onConfirm = { _, _, _, _ -> }
        )
    }
}