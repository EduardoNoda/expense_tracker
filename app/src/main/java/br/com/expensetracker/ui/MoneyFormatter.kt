package br.com.expensetracker.ui

object MoneyFormatter {
    fun format(cents: Long): String {
        return "R$ %.2f".format(cents / 100.0)
    }
}
