package br.com.expensetracker.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.expensetracker.bridge.CoreBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ExpenseSimpleUI(
    val id: Int,
    val amountCents: Long,
    val categoryId: Int
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
    val showNoRevenueWarning: Boolean = false
)

class SummaryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadAuxiliaryData()
        loadData()
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

            _uiState.update {
                it.copy(
                    totalRevenue = summary.getOrElse(0) { 0 },
                    totalExpense = summary.getOrElse(1) { 0 },
                    balance = summary.getOrElse(2) { 0 },
                    revenues = parseRevenues(revRaw)
                )
            }
        }
    }
    fun deleteExpense(expenseId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            CoreBridge.deleteExpenseById(expenseId)
            loadData() // Recarrega a tela na hora!
        }
    }

    fun deleteRevenue(revenueId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            CoreBridge.deleteRevenueById(revenueId)
            loadData() // Recarrega a tela na hora!
        }
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
        if (raw.isEmpty()) return emptyList()
        return raw.split("\n").filter { it.isNotBlank() }.mapNotNull { line ->
            try {
                val mainParts = line.split("|")
                val revenuePart = mainParts[0].split(";")
                val expensesList = if (mainParts.size > 1 && mainParts[1].isNotBlank()) {
                    mainParts[1].split("#").filter { it.isNotBlank() }.mapNotNull { expStr ->
                        val expParts = expStr.split(";")
                        // ATENÇÃO: Estou assumindo que no C++ você colocou o ID no começo (id;amount;catId...)
                        if (expParts.size >= 3) {
                            ExpenseSimpleUI(
                                id = expParts[0].toInt(),
                                amountCents = expParts[1].toLong(),
                                categoryId = expParts[2].toInt()
                            )
                        } else null
                    }
                } else emptyList()

                RevenueUI(revenuePart[0].toInt(), revenuePart[1].toLong(), revenuePart[2], if(revenuePart.size > 3) revenuePart[3] else "Sem Nome", expensesList)
            } catch (e: Exception) { null }
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
}