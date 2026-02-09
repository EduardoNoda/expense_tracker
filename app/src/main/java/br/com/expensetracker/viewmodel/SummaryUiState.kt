package br.com.expensetracker.viewmodel

data class SummaryUiState (
    val month: Int,
    val year: Int,
    val totalRevenue: Long,
    val totalExpense: Long,
    val balance: Long,
)