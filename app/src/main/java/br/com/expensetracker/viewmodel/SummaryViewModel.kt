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

// Modelos de Dados
data class ExpenseSimpleUi(
    val amountCents: Long,
    val categoryId: Int
)

data class RevenueUI(
    val id: Int,
    val amountCents: Long,
    val date: String,
    val name: String,
    val expenses: List<ExpenseSimpleUi> = emptyList()
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
    val selectedRevenueIdForExpense: Int? = null
)

class SummaryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadAuxiliaryData()
        loadData()
    }

    private fun loadAuxiliaryData() {
        // Joga para thread de IO (Background)
        viewModelScope.launch(Dispatchers.IO) {
            val catsRaw = CoreBridge.getAllCategories()
            val paysRaw = CoreBridge.getPaymentMethods()

            // Atualiza UI na thread principal (automático pelo StateFlow)
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
        _uiState.update { it.copy(currentMonth = m, currentYear = y) }
        loadData()
    }

    fun prevMonth() {
        var m = _uiState.value.currentMonth - 1
        var y = _uiState.value.currentYear
        if (m < 1) { m = 12; y-- }
        _uiState.update { it.copy(currentMonth = m, currentYear = y) }
        loadData()
    }

    fun loadData() {
        val m = _uiState.value.currentMonth
        val y = _uiState.value.currentYear

        // IO Dispatcher: O segredo da fluidez
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

    // --- Dialogs ---
    fun openAddRevenueDialog() { _uiState.update { it.copy(isAddingRevenue = true) } }
    fun closeAddRevenueDialog() { _uiState.update { it.copy(isAddingRevenue = false) } }

    fun confirmAddRevenue(name: String, amountCents: Long) {
        val now = LocalDate.now()
        val targetMonth = _uiState.value.currentMonth
        val targetYear = _uiState.value.currentYear

        viewModelScope.launch(Dispatchers.IO) {
            CoreBridge.addRevenueUseCase(name, amountCents, now.dayOfMonth, targetMonth, targetYear)
            loadData() // Recarrega após inserir
        }
        closeAddRevenueDialog()
    }

    fun openAddExpenseDialog(revenueId: Int) {
        _uiState.update { it.copy(isAddingExpense = true, selectedRevenueIdForExpense = revenueId) }
    }

    fun closeAddExpenseDialog() {
        _uiState.update { it.copy(isAddingExpense = false, selectedRevenueIdForExpense = null) }
    }

    // ATENÇÃO: Adicionei o parametro installments (parcelas) aqui
    fun confirmAddExpense(amountCents: Long, catId: Int, payId: Int, installments: Int) {
        val revId = _uiState.value.selectedRevenueIdForExpense ?: return
        val targetMonth = _uiState.value.currentMonth
        val targetYear = _uiState.value.currentYear
        val now = LocalDate.now()

        viewModelScope.launch(Dispatchers.IO) {
            CoreBridge.addExpenseToRevenue(
                revId, amountCents, now.dayOfMonth, targetMonth, targetYear,
                catId, payId, installments // Passando parcelas
            )
            loadData()
        }
        closeAddExpenseDialog()
    }

    // ... (Parsers continuam iguais) ...
    private fun parseRevenues(raw: String): List<RevenueUI> {
        if (raw.isEmpty()) return emptyList()

        return raw.split("\n").filter { it.isNotBlank() }.mapNotNull { line ->
            try {
                // Divide Receita (0) dos Gastos (1) pelo pipe "|"
                val mainParts = line.split("|")
                val revenuePart = mainParts[0].split(";")

                // Parse dos Gastos
                val expensesList = if (mainParts.size > 1 && mainParts[1].isNotBlank()) {
                    mainParts[1].split("#")
                        .filter { it.isNotBlank() }
                        .mapNotNull { expStr ->
                            val expParts = expStr.split(";")
                            if (expParts.size >= 2) {
                                ExpenseSimpleUi(expParts[0].toLong(), expParts[1].toInt())
                            } else null
                        }
                } else {
                    emptyList()
                }

                // Cria o objeto final
                RevenueUI(
                    id = revenuePart[0].toInt(),
                    amountCents = revenuePart[1].toLong(),
                    date = revenuePart[2],
                    name = if(revenuePart.size > 3) revenuePart[3] else "Sem Nome",
                    expenses = expensesList
                )
            } catch (e: Exception) {
                Log.e("ViewModel", "Parse Error: $line", e)
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
    fun addNewCard(name: String, closingDay: Int, dueDay: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            CoreBridge.addPaymentMethod(name, closingDay, dueDay)
            // Recarrega as listas para o novo cartão aparecer no dropdown imediatamente
            loadAuxiliaryData()
        }
    }
}