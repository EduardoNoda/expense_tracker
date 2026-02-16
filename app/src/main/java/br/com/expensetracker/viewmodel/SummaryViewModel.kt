package br.com.expensetracker.viewmodel

import android.util.Log
import br.com.expensetracker.bridge.CoreBridge
import java.time.LocalDate
import br.com.expensetracker.viewmodel.RevenueUI
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
    fun loadRevenues(): List<RevenueUI> {
        val now = LocalDate.now()
        val raw = CoreBridge.getRevenuesForMonth(now.monthValue, now.year)

        return raw
            .split("\n")
            .filter { it.isNotBlank() }
            .map {
                val parts = it.split(";")
                RevenueUI(
                    id = parts[0].toInt(),
                    amountCents = parts[1].toLong(),
                    date = parts[2]
                )
            }
    }


}
