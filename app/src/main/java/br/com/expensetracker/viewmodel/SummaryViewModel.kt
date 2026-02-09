package br.com.expensetracker.viewmodel

import br.com.expensetracker.bridge.CoreBridge
import java.time.LocalDate

class SummaryViewModel {

    fun loadCurrentMonth(): SummaryUiState {
        val now = LocalDate.now()
        val month = now.monthValue
        val year = now.year
        val result = CoreBridge.getMonthSummary(month, year)

        return SummaryUiState(
            month = month,
            year = year,
            totalRevenue = result[0],
            totalExpense = result[1],
            balance = result[2]
        )
    }

}