package br.com.expensetracker.ui

import kotlin.math.abs

object MoneyFormatter {
    fun format(cents: Long): String {
        // 1. Descobre se é negativo
        val isNegative = cents < 0

        // 2. Trabalha apenas com o número positivo para matemática
        val positiveCents = abs(cents)

        val reais = positiveCents / 100
        val centavos = positiveCents % 100

        // 3. Monta a string
        val sign = if (isNegative) "-" else ""

        // Resultado: R$ -277,77
        return "R$ $sign$reais,${"%02d".format(centavos)}"
    }
}