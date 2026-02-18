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

    private lateinit var root: LinearLayout
    private lateinit var summaryText: TextView

    @SuppressLint("SetTextI18n")
    fun render(context: Context): View {

        root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        summaryText = TextView(context)

        val addButton = Button(context).apply {
            text = "Adicionar Receita 1000"
            setOnClickListener {
                viewModel.addRevenue(100000)
                reload(context)
            }
        }

        root.addView(summaryText)
        root.addView(addButton)

        reload(context)

        return root
    }

    @SuppressLint("SetTextI18n")
    private fun reload(context: Context) {
        root.removeAllViews()

        val state = viewModel.loadCurrentMonth()

        summaryText = TextView(context).apply {
            text = """
                ${state.month}/${state.year}
                Receita: ${state.totalRevenue}
                Despesa: ${state.totalExpense}
                Saldo: ${state.balance}
            """.trimIndent()
        }

        root.addView(summaryText)

        val addButton = Button(context).apply {
            text = "Adicionar Receita 1000"
            setOnClickListener {
                viewModel.addRevenue(100000)
                reload(context)
            }
        }

        root.addView(addButton)

        val revenues = viewModel.loadRevenues()

        revenues.forEach { revenue ->

            val tv = TextView(context).apply {
                text = """
                    Receita #${revenue.id}
                    ${MoneyFormatter.format(revenue.amountCents)}
                    Data: ${revenue.date}
                """.trimIndent()
            }

            root.addView(tv)

            val expenseButton = Button(context).apply {
                text = "Adicionar gasto 100"
                setOnClickListener {
                    viewModel.addExpenseToRevenue(revenue.id)
                    reload(context)
                }
            }

            root.addView(expenseButton)
        }
    }
}

