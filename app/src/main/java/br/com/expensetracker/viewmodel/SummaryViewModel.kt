package br.com.expensetracker.viewmodel

import android.util.Log
import br.com.expensetracker.bridge.CoreBridge
import java.time.LocalDate

class SummaryViewModel {

    fun loadCurrentMonth(): SummaryUiState {
        val now = LocalDate.now()
        val month = now.monthValue
        val year = now.year
        val result = CoreBridge.getMonthSummary(month, year)
        Log.d("UI_DEBUG", "Revenue: ${result[0]}")

        return SummaryUiState(
            month = month,
            year = year,
            totalRevenue = result[0],
            totalExpense = result[1],
            balance = result[2]
        )
    }

    fun addRevenue(amount: Long) {
        val now = LocalDate.now()
        CoreBridge.addRevenueUseCase(
            amount,
            now.dayOfMonth,
            now.monthValue,
            now.year
        )
    }
}