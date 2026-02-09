package br.com.expensetracker.ui

import android.content.Context
import android.view.View
import android.widget.TextView
import br.com.expensetracker.viewmodel.SummaryViewModel

class SummaryScreen (private val viewModel: SummaryViewModel) {
    fun render(context: Context): View {
        val state = viewModel.loadCurrentMonth()

        val text = """
            ${state.month} / ${state.year}
            
            Receita: ${state.totalRevenue}
            Despesa: ${state.totalExpense}
            
            Saldo: ${state.balance}
        """.trimIndent()

        return TextView(context).apply {
            textSize = 18f
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            text
        }
    }
}