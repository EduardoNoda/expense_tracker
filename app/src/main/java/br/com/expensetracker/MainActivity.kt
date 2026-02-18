package br.com.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import br.com.expensetracker.bridge.CoreBridge
import br.com.expensetracker.ui.ExpenseTrackerApp
import br.com.expensetracker.viewmodel.SummaryViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa Banco
        val dbPath = applicationContext.getDatabasePath("expense.db").absolutePath
        CoreBridge.initDatabase(dbPath)

        // Inicializa ViewModel
        val viewModel = SummaryViewModel()

        setContent {
            // MaterialTheme 3 padrão
            androidx.compose.material3.MaterialTheme {
                ExpenseTrackerApp(viewModel)
            }
        }
    }
}