package br.com.expensetracker.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.expensetracker.bridge.CoreBridge
import br.com.expensetracker.ui.TempExpenseData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class TimelineFilter { TODOS, RECEITAS, GASTOS, A_PRAZO, A_VISTA }
data class ExpenseSimpleUI(
    val id: Int,
    val amountCents: Long,
    val categoryId: Int,
    val payId: Int,
    val date: String
)

data class RevenueUI(
    val id: Int,
    val amountCents: Long,
    val date: String,
    val name: String,
    val expenses: List<ExpenseSimpleUI> = emptyList()
)

data class SimpleItem(val id: Int, val name: String)

data class HomeUiState(
    val currentMonth: Int = LocalDate.now().monthValue,
    val currentYear: Int = LocalDate.now().year,
    val totalRevenue: Long = 0,
    val totalExpense: Long = 0,
    val balance: Long = 0,
    val revenues: List<RevenueUI> = emptyList(),
    val categories: List<SimpleItem> = emptyList(),
    val paymentMethods: List<SimpleItem> = emptyList(),
    val isAddingRevenue: Boolean = false,
    val isAddingExpense: Boolean = false,
    val selectedRevenueIdForExpense: Int? = null,

    // --- NOVOS ESTADOS DE UX ---
    val isSelectingRevenueMode: Boolean = false,
    val showNoRevenueWarning: Boolean = false,

    val showSummaryTimeline: Boolean = false,
    val timelineFilter: TimelineFilter = TimelineFilter.TODOS,
    val pendingOverspendExpense: TempExpenseData? = null,

    val showPayFaturaDialog: Boolean = false,
    val revenueToDelete: RevenueUI? = null,
    val expenseToDelete: Int? = null,
    val smartPromptData: Pair<Long, String>? = null
)

class SummaryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadAuxiliaryData()
        loadData()
    }

    // --- CONTROLE DA LINHA DO TEMPO ---
    fun openSummaryTimeline() { _uiState.update { it.copy(showSummaryTimeline = true) } }
    fun closeSummaryTimeline() { _uiState.update { it.copy(showSummaryTimeline = false, timelineFilter = TimelineFilter.TODOS) } }
    fun setTimelineFilter(filter: TimelineFilter) { _uiState.update { it.copy(timelineFilter = filter) } }

    fun openPayFaturaDialog() { _uiState.update { it.copy(showPayFaturaDialog = true) } }
    fun closePayFaturaDialog() { _uiState.update { it.copy(showPayFaturaDialog = false) } }

    fun payFatura(targetRevenueId: Int) {
        val currentMonth = _uiState.value.currentMonth
        val currentYear = _uiState.value.currentYear

        // Manda o C++ transferir os gastos para a Receita escolhida
        br.com.expensetracker.bridge.CoreBridge.payCreditCardBill(currentMonth, currentYear, targetRevenueId)

        closePayFaturaDialog()
        loadData() // <-- CORRIGIDO AQUI! Usamos a sua função loadData() para atualizar a tela!
    }
    // --- CONTROLES DE EXCLUSÃO ---
    fun promptDeleteRevenue(revenue: RevenueUI) {
        _uiState.update { it.copy(revenueToDelete = revenue) }
    }

    fun dismissDeleteRevenue() {
        _uiState.update { it.copy(revenueToDelete = null) }
    }

    fun promptDeleteExpense(expenseId: Int) {
        _uiState.update { it.copy(expenseToDelete = expenseId) }
    }

    fun dismissDeleteExpense() {
        _uiState.update { it.copy(expenseToDelete = null) }
    }
    private fun loadAuxiliaryData() {
        viewModelScope.launch(Dispatchers.IO) {
            val catsRaw = CoreBridge.getAllCategories()
            val paysRaw = CoreBridge.getPaymentMethods()
            _uiState.update {
                it.copy(
                    categories = parseSimpleList(catsRaw),
                    paymentMethods = parseSimpleList(paysRaw)
                )
            }
        }
    }

    fun nextMonth() {
        var m = _uiState.value.currentMonth + 1
        var y = _uiState.value.currentYear
        if (m > 12) { m = 1; y++ }
        _uiState.update { it.copy(currentMonth = m, currentYear = y, isSelectingRevenueMode = false) }
        loadData()
    }

    fun prevMonth() {
        var m = _uiState.value.currentMonth - 1
        var y = _uiState.value.currentYear
        if (m < 1) { m = 12; y-- }
        _uiState.update { it.copy(currentMonth = m, currentYear = y, isSelectingRevenueMode = false) }
        loadData()
    }

    fun loadData() {
        val m = _uiState.value.currentMonth
        val y = _uiState.value.currentYear

        viewModelScope.launch(Dispatchers.IO) {
            val summary = CoreBridge.getMonthSummary(m, y)
            val revRaw = CoreBridge.getRevenuesForMonth(m, y)

            // 1. Lemos os dados brutos
            val parsedRevenues = parseRevenues(revRaw)

            // 2. A MÁGICA DA ORDENAÇÃO: Fatura (ID == 0) vai para o topo!
            val faturaCard = parsedRevenues.find { it.id == 0 }
            val salarios = parsedRevenues.filter { it.id != 0 }
            val revenuesOrdenadas = if (faturaCard != null) listOf(faturaCard) + salarios else salarios

            // 3. O INVESTIGADOR DO C++ (Lazy Evaluator)
            val now = LocalDate.now()
            val dueDataStr = CoreBridge.checkDueInvoices(now.dayOfMonth, now.monthValue, now.year)
            var promptData: Pair<Long, String>? = null

            if (dueDataStr.isNotBlank()) {
                val parts = dueDataStr.split(";")
                if (parts.size >= 2) {
                    promptData = Pair(parts[0].toLong(), parts[1]) // Pair(Valor, Data)
                }
            }

            _uiState.update {
                it.copy(
                    totalRevenue = summary.getOrElse(0) { 0 },
                    totalExpense = summary.getOrElse(1) { 0 },
                    balance = summary.getOrElse(2) { 0 },
                    revenues = revenuesOrdenadas, // <-- Usamos a lista ordenada aqui!
                    smartPromptData = promptData  // <-- Dispara o lembrete!
                )
            }
        }
    }

    // Não se esqueça de adicionar a função para dispensar o aviso:
    fun dismissSmartPrompt() {
        _uiState.update { it.copy(smartPromptData = null) }
    }
    fun deleteExpense(expenseId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            CoreBridge.deleteExpenseById(expenseId)
            loadData() // Recarrega a tela instantaneamente
        }
        dismissDeleteExpense() // Fecha o popup
    }

    fun deleteRevenue(revenueId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            CoreBridge.deleteRevenueById(revenueId)
            loadData() // Recarrega a tela instantaneamente
        }
        dismissDeleteRevenue() // Fecha o popup
    }

    // --- NOVA LÓGICA DO BOTÃO DE GASTO ---
    fun onAddExpenseActionClicked() {
        val currentRevenues = _uiState.value.revenues
        when {
            currentRevenues.isEmpty() -> {
                // Não tem receita: Mostra aviso
                _uiState.update { it.copy(showNoRevenueWarning = true) }
            }
            currentRevenues.size == 1 -> {
                // Só tem uma: Vai direto pro formulário
                openAddExpenseDialog(currentRevenues.first().id)
            }
            else -> {
                // Tem 2 ou mais: Entra no modo de seleção
                _uiState.update { it.copy(isSelectingRevenueMode = true) }
            }
        }
    }

    fun dismissNoRevenueWarning() {
        _uiState.update { it.copy(showNoRevenueWarning = false) }
    }

    fun cancelRevenueSelection() {
        _uiState.update { it.copy(isSelectingRevenueMode = false) }
    }

    // --- MANIPULAÇÃO DE DIALOGS E DADOS ---
    fun openAddRevenueDialog() { _uiState.update { it.copy(isAddingRevenue = true) } }
    fun closeAddRevenueDialog() { _uiState.update { it.copy(isAddingRevenue = false) } }

    fun confirmAddRevenue(name: String, amountCents: Long) {
        val now = LocalDate.now()
        val targetMonth = _uiState.value.currentMonth
        val targetYear = _uiState.value.currentYear

        viewModelScope.launch(Dispatchers.IO) {
            CoreBridge.addRevenueUseCase(name, amountCents, now.dayOfMonth, targetMonth, targetYear)
            loadData()
        }
        // REMOVIDO: closeAddRevenueDialog() - A UI fará isso suavemente agora
    }

    fun openAddExpenseDialog(revenueId: Int) {
        _uiState.update {
            it.copy(
                isAddingExpense = true,
                selectedRevenueIdForExpense = revenueId,
                isSelectingRevenueMode = false
            )
        }
    }

    fun closeAddExpenseDialog() {
        _uiState.update { it.copy(isAddingExpense = false, selectedRevenueIdForExpense = null) }
    }

    fun confirmAddExpense(revId: Int, amountCents: Long, catId: Int, payId: Int, installments: Int) {
        val targetMonth = _uiState.value.currentMonth
        val targetYear = _uiState.value.currentYear
        val now = LocalDate.now()

        viewModelScope.launch(Dispatchers.IO) {
            CoreBridge.addExpenseToRevenue(revId, amountCents, now.dayOfMonth, targetMonth, targetYear, catId, payId, installments)
            loadData()
        }
        // REMOVIDO: closeAddExpenseDialog() - A UI fará isso suavemente agora
    }

    fun addNewCard(name: String, closingDay: Int, dueDay: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            CoreBridge.addPaymentMethod(name, closingDay, dueDay)
            loadAuxiliaryData()
        }
    }

    // ... (Parsers continuam iguais)
    private fun parseRevenues(raw: String): List<RevenueUI> {
        // ESPIÃO DO LOGCAT: Vai imprimir a string bruta no painel!
        Log.e("JNI_DEBUG", "C++ Mandou: $raw")

        if (raw.isEmpty()) return emptyList()
        return raw.split("\n").filter { it.isNotBlank() }.mapNotNull { line ->
            try {
                val mainParts = line.split("|")
                val revenuePart = mainParts[0].split(";")
                val expensesList = if (mainParts.size > 1 && mainParts[1].isNotBlank()) {
                    mainParts[1].split("#").filter { it.isNotBlank() }.mapNotNull { expStr ->
                        val expParts = expStr.split(";")

                        if (expParts.size >= 5) {
                            ExpenseSimpleUI(
                                id = expParts[0].toInt(),
                                amountCents = expParts[1].toLong(),
                                categoryId = expParts[2].toInt(),
                                payId = expParts[3].toInt(),
                                date = expParts[4].trim()
                            )
                        } else if (expParts.size >= 4) {
                            ExpenseSimpleUI(
                                id = expParts[0].toInt(),
                                amountCents = expParts[1].toLong(),
                                categoryId = expParts[2].toInt(),
                                payId = expParts[3].toInt(),
                                date = "2026-01-01"
                            )
                        } else null
                    }
                } else emptyList()

                RevenueUI(
                    id = revenuePart[0].toInt(),
                    amountCents = revenuePart[1].toLong(),
                    date = revenuePart[2],
                    name = if(revenuePart.size > 3) revenuePart[3] else "Sem Nome",
                    expenses = expensesList
                )
            } catch (e: Exception) {
                // SE DER ERRO NA LINHA, O LOGCAT VAI DEDURAR O MOTIVO!
                Log.e("JNI_DEBUG", "Erro ao processar linha: $line. Motivo: ${e.message}")
                null
            }
        }
    }
    private fun parseSimpleList(raw: String): List<SimpleItem> {
        if (raw.isEmpty()) return emptyList()
        return raw.split("\n").filter { it.isNotBlank() }.mapNotNull {
            try {
                val p = it.split(";")
                SimpleItem(p[0].toInt(), p[1])
            } catch (e: Exception) { null }
        }
    }
    fun tryAddExpense(revId: Int, amountCents: Long, catId: Int, payId: Int, installments: Int) {
        if (payId > 1) {
            // A MÁGICA ACONTECE AQUI!
            // É A_PRAZO (Cartão). Não descontamos de nenhuma receita agora.
            // Mandamos '0' como Receita para o C++ salvar como NULL (Gasto flutuante)
            confirmAddExpense(0, amountCents, catId, payId, installments)
            return // Sai da função, não precisa checar saldo!
        }

        // Se chegou aqui, é A_VISTA (payId == 1). Faz a checagem de saldo normal:
        val targetRev = _uiState.value.revenues.find { it.id == revId }
        if (targetRev != null) {
            val totalSpent = targetRev.expenses.sumOf { it.amountCents }
            val remaining = targetRev.amountCents - totalSpent

            if (amountCents > remaining) {
                // Estourou o limite da receita! Abre o alerta.
                _uiState.update {
                    it.copy(pendingOverspendExpense = TempExpenseData(revId, amountCents, catId, payId, installments))
                }
            } else {
                // Tem saldo suficiente, salva direto.
                confirmAddExpense(revId, amountCents, catId, payId, installments)
            }
        }
    }

    fun dismissOverspendWarning() {
        _uiState.update { it.copy(pendingOverspendExpense = null) }
    }
}