package br.com.expensetracker.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import br.com.expensetracker.bridge.CoreBridge
import br.com.expensetracker.viewmodel.SummaryViewModel
import org.w3c.dom.Text
import java.time.LocalDate

class SummaryScreen(private val viewModel: SummaryViewModel) {

    @SuppressLint("SetTextI18n")
    fun render(context: Context): View {

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        val textView = TextView(context)

        fun refresh() {
            val state = viewModel.loadCurrentMonth()
            textView.text = """
                ${state.month}/${state.year}
        
                Receita: ${state.totalRevenue}
                Despesa: ${state.totalExpense}
        
                Saldo: ${state.balance}
            """.trimIndent()
        }

        val button = Button(context).apply {
            text = "Adicionar Receita 1000"
            setOnClickListener {
                viewModel.addRevenue(100000)
                refresh()
            }
        }

        refresh()

        layout.addView(textView)
        layout.addView(button)

        return layout
    }
}
