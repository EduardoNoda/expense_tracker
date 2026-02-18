package br.com.expensetracker.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MoneyInput(
    valueCents: Long,
    onValueChange: (Long) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    // O TextField "pensa" que o texto é apenas "2700"
    // O usuário "vê" o texto como "R$ 27,00"

    OutlinedTextField(
        value = if (valueCents == 0L) "" else valueCents.toString(),
        onValueChange = { input ->
            // Filtra apenas dígitos. Se vazio, vira 0.
            val cleanInput = input.filter { it.isDigit() }
            if (cleanInput.length <= 15) {
                onValueChange(cleanInput.toLongOrNull() ?: 0L)
            }
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = CurrencyVisualTransformation(), // <--- A MÁGICA AQUI
        modifier = modifier
    )
}

// Classe que ensina o Compose a desenhar o dinheiro sem alterar o valor real
private class CurrencyVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.text.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val cents = text.text.toLongOrNull() ?: 0L
        val reais = cents / 100.0

        // Formata para R$ X,XX
        val out = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(reais)

        // Mapping fixo: O cursor sempre fica no final do texto formatado
        // Isso evita o bug de digitar "2" e ele pular para o começo.
        val numberOffsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return out.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                return text.length
            }
        }

        return TransformedText(AnnotatedString(out), numberOffsetTranslator)
    }
}