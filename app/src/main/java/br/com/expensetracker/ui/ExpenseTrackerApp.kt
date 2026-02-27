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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
            initialValue = 1f, targetValue = 1.03f,
            animationSpec = infiniteRepeatable(animation = tween(600, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
            label = "PulsingCard"
        ).value
    } else 1f

    val baseModifier = Modifier.fillMaxWidth().scale(scale)
    val finalModifier = if (isSelectingMode) {
        baseModifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCardClick)
    } else baseModifier

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
                Column { Text("Total Gasto", style = MaterialTheme.typography.bodySmall); Text(MoneyFormatter.format(totalSpent), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) }
                Column(horizontalAlignment = Alignment.End) { Text("Restante", style = MaterialTheme.typography.bodySmall); Text(MoneyFormatter.format(remaining), fontWeight = FontWeight.SemiBold, color = if (remaining < 0) Color.Red else Color(0xFF2E7D32)) }
            }

            if (revenue.expenses.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // MÁGICA AQUI: Separamos a lista em duas! (Supondo que payId 1 é Dinheiro/Pix/Débito)
                val aVista = revenue.expenses.filter { it.payId <= 1 }
                val noCredito = revenue.expenses.filter { it.payId > 1 }

                if (aVista.isNotEmpty()) {
                    Text("À Vista (Pix/Débito):", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    aVista.forEach { expense ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("- Categoria ${expense.categoryId}", style = MaterialTheme.typography.bodyMedium) // Aqui você pode pôr o nome da categoria no futuro
                            Text(MoneyFormatter.format(expense.amountCents), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (noCredito.isNotEmpty()) {
                    Text("Faturas de Cartão (Crédito):", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    noCredito.forEach { expense ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cat ${expense.categoryId}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(MoneyFormatter.format(expense.amountCents), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color(0xFFE65100)) // Laranjinha pra destacar dívida
                        }
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

    var textFieldValue by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Valor da Receita", style = MaterialTheme.typography.labelLarge, color = Color.Gray)

        TextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                val digits = newValue.text.filter { it.isDigit() }
                amountCents = if (digits.isEmpty()) 0L else digits.toLong()

                val formattedText = if (amountCents == 0L) {
                    ""
                } else {
                    val reais = amountCents / 100
                    val centavos = amountCents % 100
                    val reaisFormatado = "%,d".format(java.util.Locale("pt", "BR"), reais)
                    "R$ $reaisFormatado,%02d".format(centavos)
                }

                textFieldValue = androidx.compose.ui.text.input.TextFieldValue(
                    text = formattedText,
                    selection = androidx.compose.ui.text.TextRange(formattedText.length)
                )
            },
            textStyle = MaterialTheme.typography.displayMedium.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            ),
            placeholder = {
                Text("R$ 0,00", style = MaterialTheme.typography.displayMedium, color = Color.LightGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Descrição (ex: Salário, Freela)") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text("Cancelar") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (name.isNotBlank() && amountCents > 0) {
                        val cleanName = name.trim().replaceFirstChar { it.uppercase() }
                        onConfirm(cleanName, amountCents)
                    }
                },
                enabled = name.isNotBlank() && amountCents > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("Salvar Receita")
            }
        }
    }
}

// --- PREVIEW DA RECEITA ---
// Use o painel Split/Design do Android Studio para ver como ficou
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
fun AddRevenuePreview() {
    MaterialTheme {
        AddRevenueSheetContent(
            onCancel = {},
            onConfirm = { _, _ -> }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    var textFieldValue by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }

    var currentView by remember { mutableStateOf("FORM") }
    val focusManager = LocalFocusManager.current

    Crossfade(targetState = currentView, label = "ExpenseViewTransition") { view ->
        when (view) {
            "FORM" -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Valor do Gasto", style = MaterialTheme.typography.labelLarge, color = Color.Gray)

                    TextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            val digits = newValue.text.filter { it.isDigit() }
                            amountCents = if (digits.isEmpty()) 0L else digits.toLong()

                            val formattedText = if (amountCents == 0L) {
                                ""
                            } else {
                                val reais = amountCents / 100
                                val centavos = amountCents % 100
                                val reaisFormatado = "%,d".format(java.util.Locale("pt", "BR"), reais)
                                "R$ $reaisFormatado,%02d".format(centavos)
                            }

                            textFieldValue = androidx.compose.ui.text.input.TextFieldValue(
                                text = formattedText,
                                selection = androidx.compose.ui.text.TextRange(formattedText.length)
                            )
                        },
                        textStyle = MaterialTheme.typography.displayMedium.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        placeholder = { Text("R$ 0,00", style = MaterialTheme.typography.displayMedium, color = Color.LightGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            onClick = {
                                focusManager.clearFocus()
                                currentView = "CATEGORY"
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selectedCat != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Icon(Icons.Default.Sell, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = selectedCat?.name ?: "Categoria", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            }
                        }

                        Surface(
                            onClick = {
                                focusManager.clearFocus()
                                currentView = "PAYMENT"
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selectedPay != null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = selectedPay?.name ?: "Pagamento", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(visible = selectedPay != null && selectedPay!!.id > 1) {
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Parcelamento", style = MaterialTheme.typography.labelLarge)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { val c = installmentsText.toIntOrNull() ?: 1; if (c > 1) installmentsText = (c - 1).toString() }) { Icon(Icons.Default.KeyboardArrowLeft, "-") }
                                    Text(text = "${installmentsText}x", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { val c = installmentsText.toIntOrNull() ?: 1; if (c < 999) installmentsText = (c + 1).toString() }) { Icon(Icons.Default.KeyboardArrowRight, "+") }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

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

            // TELA INTERNA: CATEGORIAS (Corrigida com altura máxima para não travar)
            "CATEGORY" -> {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { currentView = "FORM" }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                        Text("Selecione a Categoria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        // AQUI ESTÁ A MÁGICA: Limita a altura para o BottomSheet não surtar
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(categories) { cat ->
                            Surface(
                                onClick = { selectedCat = cat; currentView = "FORM" },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.aspectRatio(1f)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(Icons.Default.Sell, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(cat.name, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }

            // TELA INTERNA: PAGAMENTOS (Corrigida sem LazyColumn)
            "PAYMENT" -> {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { currentView = "FORM" }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                        Text("Forma de Pagamento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp)
                    ) {
                        paymentMethods.forEach { pay ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // MUDANÇA AQUI: Clique seguro sem Ripple para não dar o erro do PlatformRipple
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            selectedPay = pay
                                            currentView = "FORM"
                                        }
                                    )
                                    .padding(vertical = 16.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(pay.name, style = MaterialTheme.typography.bodyLarge)
                            }
                            HorizontalDivider()
                        }
                    }
                }
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