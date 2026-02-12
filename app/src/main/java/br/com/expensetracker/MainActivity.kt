package br.com.expensetracker

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import br.com.expensetracker.bridge.CoreBridge
import br.com.expensetracker.ui.SummaryScreen
import br.com.expensetracker.viewmodel.SummaryViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dbPath = applicationContext.getDatabasePath("expense.db").absolutePath
        CoreBridge.initDatabase(dbPath)

        val screen = SummaryScreen(SummaryViewModel())
        setContentView(screen.render(this))

    }
}
