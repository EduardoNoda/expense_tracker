package br.com.expensetracker.ui

import java.util.Locale
import kotlin.math.abs

object MoneyFormatter {
    fun format(cents: Long): String {
        val isNegative = cents < 0
        val positiveCents = abs(cents)

        val reais = positiveCents / 100
        val centavos = positiveCents % 100

        val sign = if (isNegative) "-" else ""

        // MÁGICA AQUI: Formata com o ponto de milhar brasileiro
        val reaisFormatado = "%,d".format(Locale("pt", "BR"), reais)

        return "R$ $sign$reaisFormatado,${"%02d".format(centavos)}"
    }
}